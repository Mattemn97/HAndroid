package it.handroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.util.Log
import androidx.core.app.ServiceCompat
import java.io.File
import java.io.FilterInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

/**
 * Servizio in primo piano che avvia Home Assistant nel processo ":ha" e lo sorveglia.
 * Il processo vive quanto un giro di HA: quando HA esce, il processo muore e, se serve, il
 * supervisor lo fa ripartire (vedi [Supervisor]).
 */
class HaService : Service() {
    private var started = false
    @Volatile private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, 1, notification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (intent?.action == ACTION_STOP) {
            stop("Home Assistant fermato dall'app")
            return START_NOT_STICKY
        }
        if (!started) {
            started = true
            Supervisor.setAutostart(this, true)
            // il giro precedente è finito senza passare da restart() o stop(): processo ucciso di colpo
            if (Supervisor.markRunning(this)) Supervisor.recordCrash(this)
            Supervisor.scheduleSafetyNet(this)
            setStatus(this, "Avvio del servizio…")  // cancella lo stato del giro precedente
            // hardware del telefono per HA (M2): dispositivo ESPHome con proxy Bluetooth, ponte USB
            it.handroid.esphome.EspHome.avvia(this)
            UsbPonte.avvia(this)
            // add-on (M3): API locale su 127.0.0.1:8099 e avvio automatico, nei processi :addonN
            it.handroid.addon.AddonManager.avvia(this)
            thread(name = "hass") { runAndSupervise() }
            thread(name = "watchdog", isDaemon = true) { watchdog() }
        }
        if (intent?.getBooleanExtra(RECHECK_USB, false) == true) UsbPonte.ricontrolla(this)
        return START_STICKY
    }

    private fun runAndSupervise() {
        val wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HAndroid:hass")
        // Wi-Fi sempre attivo anche a schermo spento, e multicast per mDNS (scoperta dei dispositivi)
        val wifi = applicationContext.getSystemService(WifiManager::class.java)
        @Suppress("DEPRECATION")
        val wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HAndroid:wifi")
        val multicastLock = wifi.createMulticastLock("HAndroid:mdns")
        wakeLock.acquire()
        wifiLock.acquire()
        multicastLock.acquire()
        val code = try {
            runHomeAssistant()
        } catch (e: Throwable) {
            Log.e(TAG, "Avvio fallito", e)
            -1
        }
        Log.w(TAG, "Home Assistant terminato con codice $code")
        when {
            stopping -> {}
            code == 0 -> stop("Home Assistant fermato dall'interfaccia")
            code == Supervisor.RESTART_EXIT_CODE -> restart("Riavvio richiesto da Home Assistant", 1000)
            else -> crashed("Home Assistant terminato con codice $code")
        }
        multicastLock.release()
        wifiLock.release()
        wakeLock.release()
    }

    /**
     * Ogni 30 secondi controlla che HA risponda. Dopo 3 controlli falliti di fila lo considera bloccato
     * e lo riavvia. Parte quando HA è pronto (o dopo 20 minuti, se non lo diventa mai).
     */
    private fun watchdog() {
        val begin = System.currentTimeMillis()
        while (statusFile(this).readText() != READY && System.currentTimeMillis() - begin < 20 * 60_000L) {
            Thread.sleep(5_000)
            Supervisor.scheduleSafetyNet(this)
        }
        val readyAt = System.currentTimeMillis()
        var failures = 0
        var stable = false
        while (!stopping) {
            Thread.sleep(30_000)
            Supervisor.scheduleSafetyNet(this)
            failures = if (responds()) 0 else failures + 1
            if (failures >= 3) {
                crashed("Home Assistant non risponde da 90 secondi")
                return
            }
            if (!stable && System.currentTimeMillis() - readyAt > Supervisor.STABLE_AFTER_MS) {
                stable = true
                Supervisor.resetCrashes(this)
            }
        }
    }

    private fun crashed(reason: String) {
        val crashes = Supervisor.recordCrash(this)
        val recovery = crashes >= Supervisor.MAX_CRASHES
        Log.e(TAG, "$reason (crash $crashes di fila)")
        restart(
            if (recovery) "$reason. Troppi errori: riparto in modalità di ripristino" else "$reason. Riavvio…",
            if (recovery) 5_000 else 10_000,
        )
    }

    private fun restart(reason: String, delayMs: Long) {
        stopping = true
        setStatus(this, reason)
        Supervisor.clearRunning(this)
        Supervisor.scheduleRestart(this, delayMs)
        Process.killProcess(Process.myPid())
    }

    private fun stop(reason: String) {
        stopping = true
        setStatus(this, reason)
        Supervisor.setAutostart(this, false)
        Supervisor.clearRunning(this)
        Supervisor.cancelRestart(this)
        stopSelf()
        Process.killProcess(Process.myPid())
    }

    private fun runHomeAssistant(): Int {
        val home = extractPython()
        val config = File(filesDir, "ha-config").apply { mkdirs() }
        val yaml = File(config, "configuration.yaml")
        // si scrive se manca, o se è ancora quella minima della M0 (senza scoperta dei dispositivi)
        if (!yaml.exists() || yaml.readText().startsWith("# Configurazione minima della M0")) {
            yaml.writeText(assets.open("configuration.yaml").reader().readText())
        }

        // le variabili d'ambiente impostate da qui non arrivano a Python: handroid_boot.py ricava HOME,
        // TMPDIR e il file di stato dalla cartella di --config, e legge da dns.txt i DNS della rete
        // (Android non ha resolv.conf)
        File(filesDir, "dns.txt").writeText(dnsServers().joinToString(","))
        setStatus(this, "Carico Python e Home Assistant (dopo un'installazione anche 2-3 minuti)…")
        val args = mutableListOf("hass", "--config", config.path)
        if (Supervisor.recoveryMode(this)) args += "--recovery-mode"
        Log.i(TAG, "Avvio Home Assistant: ${args.joinToString(" ")}")
        return PythonRunner.runModule(home.path, File(home, "site-packages").path, "handroid_boot", args.toTypedArray())
    }

    /** Estrae python.zip (stdlib e site-packages) a ogni nuova installazione dell'APK. */
    private fun extractPython(): File {
        val home = File(filesDir, "python")
        val stamp = File(home, ".installato")
        val version = packageManager.getPackageInfo(packageName, 0).lastUpdateTime.toString()
        if (stamp.exists() && stamp.readText() == version) return home

        Log.i(TAG, "Estraggo Python in $home")
        home.deleteRecursively()
        // python.zip non è compresso nell'APK (noCompress): la sua lunghezza dà l'avanzamento
        val total = assets.openFd("python.zip").use { it.length }
        var read = 0L
        var shown = -1
        val counting = object : FilterInputStream(assets.open("python.zip")) {
            override fun read(b: ByteArray, off: Int, len: Int) = super.read(b, off, len).also { if (it > 0) read += it }
        }
        ZipInputStream(counting.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val percent = (read * 100 / total).toInt()
                if (percent != shown) {
                    shown = percent
                    setStatus(this, "Prima installazione: estraggo Python ($percent%)")
                }
                val out = File(home, entry.name)
                require(out.canonicalPath.startsWith(home.canonicalPath)) { "Percorso non valido: ${entry.name}" }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.copyTo(it) }
                }
            }
        }
        stamp.writeText(version)
        return home
    }

    private fun dnsServers(): List<String> {
        val cm = getSystemService(ConnectivityManager::class.java)
        val props = cm.getLinkProperties(cm.activeNetwork) ?: return emptyList()
        return props.dnsServers.mapNotNull { it.hostAddress?.substringBefore('%') }
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Server", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL)
        else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setContentTitle("Home Assistant attivo")
            .setContentText("Porta 8123")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "HAndroid"
        private const val CHANNEL = "server"
        private const val ACTION_STOP = "it.handroid.STOP"
        private const val RECHECK_USB = "ricontrolla_usb"

        /** Testo che handroid_boot.py scrive quando HA ha finito di avviarsi. */
        const val READY = "Home Assistant pronto"

        /** L'interfaccia di HA risponde su questo telefono. */
        fun responds() = try {
            (URL("http://127.0.0.1:8123/manifest.json").openConnection() as HttpURLConnection).run {
                connectTimeout = 5_000
                readTimeout = 10_000
                (responseCode == 200).also { disconnect() }
            }
        } catch (e: Exception) {
            false
        }

        /** Ultimo passo dell'avvio, scritto da qui e da handroid_boot.py, mostrato da MainActivity. */
        fun statusFile(context: Context) = File(context.filesDir, "stato.txt")

        fun setStatus(context: Context, text: String) = statusFile(context).writeText(text)

        /** [recheckUsb]: una chiavetta è appena stata collegata (da [UsbAttivita]). */
        fun start(context: Context, recheckUsb: Boolean = false) {
            val intent = Intent(context, HaService::class.java).putExtra(RECHECK_USB, recheckUsb)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }

        /** Ferma HA e disattiva l'avvio automatico finché non lo riavvii dall'app. */
        fun stop(context: Context) {
            val intent = Intent(context, HaService::class.java).setAction(ACTION_STOP)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
