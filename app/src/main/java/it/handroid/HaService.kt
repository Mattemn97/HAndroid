package it.handroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.util.Log
import androidx.core.app.ServiceCompat
import java.io.File
import java.io.FilterInputStream
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

/** Servizio in primo piano che avvia Home Assistant nel processo ":ha". */
class HaService : Service() {
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, 1, notification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (!started) {
            started = true
            thread(name = "hass") {
                val wakeLock = getSystemService(PowerManager::class.java)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HAndroid:hass")
                wakeLock.acquire()
                val code = try {
                    runHomeAssistant()
                } catch (e: Throwable) {
                    Log.e(TAG, "Avvio fallito", e)
                    -1
                }
                Log.w(TAG, "Home Assistant terminato con codice $code")
                setStatus(this, "Home Assistant fermo (codice $code)")
                wakeLock.release()
                stopSelf()
                Process.killProcess(Process.myPid())
            }
        }
        return START_STICKY
    }

    private fun runHomeAssistant(): Int {
        val home = extractPython()
        val config = File(filesDir, "ha-config").apply { mkdirs() }
        val yaml = File(config, "configuration.yaml")
        if (!yaml.exists()) yaml.writeText(assets.open("configuration.yaml").reader().readText())

        // le variabili d'ambiente impostate da qui non arrivano a Python: handroid_boot.py ricava HOME,
        // TMPDIR e il file di stato dalla cartella di --config, e legge da dns.txt i DNS della rete
        // (Android non ha resolv.conf)
        File(filesDir, "dns.txt").writeText(dnsServers().joinToString(","))
        setStatus(this, "Carico Python e Home Assistant (dopo un'installazione anche 2-3 minuti)…")
        Log.i(TAG, "Avvio Home Assistant, configurazione in $config")
        return PythonRunner.runModule(
            home.path, File(home, "site-packages").path, "handroid_boot",
            arrayOf("hass", "--config", config.path, "--skip-pip"),
        )
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

        /** Ultimo passo dell'avvio, scritto da qui e da handroid_boot.py, mostrato da MainActivity. */
        fun statusFile(context: Context) = File(context.filesDir, "stato.txt")

        fun setStatus(context: Context, text: String) = statusFile(context).writeText(text)

        fun start(context: Context) {
            val intent = Intent(context, HaService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
