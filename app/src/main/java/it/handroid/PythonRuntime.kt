package it.handroid

import android.content.Context
import android.system.Os
import java.io.File
import java.util.zip.ZipInputStream

/** CPython ufficiale incorporato: estrae stdlib e codice app, poi esegue handroid_main. */
object PythonRuntime {
    init {
        System.loadLibrary("handroid")
    }

    private external fun runModule(home: String, paths: Array<String>, module: String): Int

    /** Cartella dati leggibile anche prima dello sblocco (Direct Boot). */
    fun base(ctx: Context): File = ctx.createDeviceProtectedStorageContext().filesDir

    /** Blocca finché Python non termina; restituisce il codice di uscita. */
    fun avvia(ctx: Context, safeMode: Boolean): Int {
        val base = base(ctx)
        val home = File(base, "python")
        prepara(ctx, home)

        val config = File(base, "config").apply { mkdirs() }
        val sitePackages = File(base, "site-packages").apply { mkdirs() }
        Os.setenv("HANDROID_CONFIG", config.path, true)
        Os.setenv("HANDROID_SITE", sitePackages.path, true)
        Os.setenv("HANDROID_SAFE_MODE", if (safeMode) "1" else "0", true)
        Os.setenv("HOME", base.path, true)
        Os.setenv("TMPDIR", ctx.cacheDir.path, true)

        val lib = File(home, "lib/python3.14")
        val paths = arrayOf(
            File(home, "app").path,
            lib.path,
            File(lib, "lib-dynload").path,
            sitePackages.path,
            // HA e dipendenze inclusi nell'APK: dopo site-packages, così un pacchetto
            // aggiornato a runtime vince su quello di serie.
            File(home, "site").path,
        )
        return runModule(home.path, paths, "handroid_main")
    }

    /** Estrae assets/python.zip (e site.zip, se c'è) solo quando l'APK cambia. */
    private fun prepara(ctx: Context, home: File) {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val firma = "${info.longVersionCode}-${info.lastUpdateTime}"
        val stamp = File(home, ".versione")
        if (stamp.exists() && stamp.readText() == firma) return

        home.deleteRecursively()
        home.mkdirs()
        estrai(ctx, "python.zip", home)
        if ("site.zip" in ctx.assets.list("").orEmpty()) estrai(ctx, "site.zip", File(home, "site"))
        stamp.writeText(firma)
    }

    private fun estrai(ctx: Context, asset: String, dest: File) {
        val radice = dest.canonicalPath + File.separator
        ZipInputStream(ctx.assets.open(asset).buffered()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val out = File(dest, e.name)
                require(out.canonicalPath.startsWith(radice)) { "Percorso non valido: ${e.name}" }
                if (e.isDirectory) out.mkdirs()
                else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.copyTo(it) }
                }
            }
        }
    }
}
