package it.handroid.addon

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import it.handroid.PythonRuntime
import org.json.JSONObject
import java.io.File

/**
 * Esegue un add-on in un processo tutto suo del pool (`:addon1`…`:addon3`, AndroidManifest).
 * Python: CPython caricato di nuovo via il launcher JNI, come in `:ha`. Native: il binario
 * `lib<nome>.so` dalla nativeLibraryDir (D3, D10). Fermare = chiudere il processo.
 */
abstract class AddonService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val slug = intent?.getStringExtra(EXTRA_SLUG)
        if (slug == null || avviato != null) return START_NOT_STICKY
        avviato = slug
        primoPiano(slug)
        val dir = File(File(PythonRuntime.base(this), "addons"), slug)
        Thread({
            val rc = runCatching { esegui(dir) }
                .onFailure { Log.e(TAG, "Add-on $slug non è partito", it) }
                .getOrDefault(-1)
            Log.w(TAG, "Add-on $slug terminato con codice $rc")
            File(dir, "uscita.txt").writeText("$rc\n")
            stopSelf()
            Process.killProcess(Process.myPid())
        }, "addon-$slug").start()
        // Non sticky: lo riavvia il gestore in `:ha`, che sa quali add-on devono girare.
        return START_NOT_STICKY
    }

    private fun esegui(dir: File): Int {
        val m = Manifest.fromJson(JSONObject(File(dir, "manifest.json").readText()))
        if (m.type == "python") return PythonRuntime.avviaAddon(this, dir)
        val bin = binarioNativo(applicationInfo.nativeLibraryDir, m.entry)
            ?: error("Binario lib${m.entry}.so non incluso in questo APK")
        val p = ProcessBuilder(listOf(bin.path) + m.args)
            .directory(dir).redirectErrorStream(true)
            .redirectOutput(File(dir, "log.txt")).start()
        return p.waitFor()
    }

    override fun onDestroy() {
        super.onDestroy()
        // CPython non si ferma in modo pulito: il processo si chiude e basta.
        Process.killProcess(Process.myPid())
    }

    private fun primoPiano(slug: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CANALE, "Add-on", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, CANALE)
            .setContentTitle("Add-on attivo: $slug")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .build()
        val id = 100 + javaClass.simpleName.last().digitToInt()
        if (Build.VERSION.SDK_INT >= 34) startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(id, n)
    }

    companion object {
        const val TAG = "HAndroid"
        const val EXTRA_SLUG = "slug"
        private const val CANALE = "addon"
        @Volatile private var avviato: String? = null

        /** Gli add-on nativi sono moduli dentro l'APK: `lib<nome>.so` in jniLibs (D10). */
        fun binarioNativo(nativeLibraryDir: String, nome: String): File? =
            File(nativeLibraryDir, "lib$nome.so").takeIf { it.isFile }

        fun classe(slot: Int): Class<out AddonService> = when (slot) {
            1 -> AddonService1::class.java
            2 -> AddonService2::class.java
            3 -> AddonService3::class.java
            else -> throw IllegalArgumentException("Slot $slot fuori dal pool")
        }
    }
}

class AddonService1 : AddonService()
class AddonService2 : AddonService()
class AddonService3 : AddonService()
