package it.handroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Servizio sempre attivo nel processo ":ha": tiene sveglio il telefono, avvia Python
 * e controlla che risponda. Se Python esce o si blocca, il processo viene chiuso e
 * Android lo riavvia (START_STICKY).
 */
class HaService : Service() {
    private val locks = mutableListOf<() -> Unit>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        avviaInPrimoPiano()
        prendiLock()

        val prefs = createDeviceProtectedStorageContext().getSharedPreferences("supervisore", Context.MODE_PRIVATE)
        val falliti = prefs.getInt(AVVII_FALLITI, 0)
        prefs.edit().putInt(AVVII_FALLITI, falliti + 1).apply()
        val safeMode = falliti >= MAX_FALLITI
        if (safeMode) Log.w(TAG, "Modalità provvisoria: $falliti avvii falliti di fila")

        Thread({
            val rc = runCatching { PythonRuntime.avvia(this, safeMode) }
                .onFailure { Log.e(TAG, "Python non è partito", it) }
                .getOrDefault(-1)
            Log.w(TAG, "Python terminato con codice $rc, riavvio")
            Process.killProcess(Process.myPid())
        }, "python").start()

        it.handroid.addon.AddonManager.avvia(this) // add-on: API locale e avvio automatico (0007)
        Thread({ watchdog { prefs.edit().putInt(AVVII_FALLITI, 0).apply() } }, "watchdog").start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        locks.forEach { it() }
        super.onDestroy()
        Process.killProcess(Process.myPid())
    }

    private fun watchdog(sano: () -> Unit) {
        Thread.sleep(GRAZIA_MS)
        var errori = 0
        while (true) {
            if (risponde()) {
                sano()
                errori = 0
            } else if (++errori >= MAX_FALLITI) {
                Log.e(TAG, "Home Assistant non risponde da ${errori * 30} s, riavvio")
                Process.killProcess(Process.myPid())
            }
            Thread.sleep(30_000)
        }
    }

    private fun risponde() = runCatching {
        val c = URL("http://127.0.0.1:8123/").openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 10_000
        c.responseCode.also { c.disconnect() } > 0
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    private fun prendiLock() {
        val wake = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HAndroid:server")
        wake.acquire()
        val wifi = getSystemService(WifiManager::class.java)
        val wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HAndroid:wifi")
        wifiLock.acquire()
        // Senza multicast zeroconf/mDNS non trova i dispositivi in casa.
        val multicast = wifi.createMulticastLock("HAndroid:mdns").apply { setReferenceCounted(false) }
        multicast.acquire()
        locks += { wake.release() }
        locks += { wifiLock.release() }
        locks += { multicast.release() }
    }

    private fun avviaInPrimoPiano() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANALE, "Server Home Assistant", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, CANALE)
            .setContentTitle("Home Assistant attivo")
            .setContentText("http://<ip-del-telefono>:8123")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    companion object {
        const val TAG = "HAndroid"
        private const val CANALE = "server"
        private const val AVVII_FALLITI = "avvii_falliti"
        private const val MAX_FALLITI = 3
        // Il primo avvio di HA installa pacchetti e può richiedere minuti.
        private const val GRAZIA_MS = 5 * 60_000L

        fun avvia(ctx: Context) = ctx.startForegroundService(Intent(ctx, HaService::class.java))
    }
}
