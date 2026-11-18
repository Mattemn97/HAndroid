package it.handroid

import android.content.Context
import java.io.File
import java.io.RandomAccessFile

/** Strumenti della schermata dell'app: log, spazio occupato, pulizia e azzeramento della configurazione. */
object Strumenti {
    /** Log senza PC: logcat dell'app (stessi tag del comando adb) e la coda di home-assistant.log. */
    fun log(context: Context): String {
        val logcat = runCatching {
            ProcessBuilder("logcat", "-d", "-t", "400", "-v", "time", "-s", "HAndroid", "python.stdout", "python.stderr")
                .redirectErrorStream(true).start().inputStream.bufferedReader().readText()
        }.getOrElse { "logcat non disponibile: ${it.message}" }
        val ha = File(context.filesDir, "ha-config/home-assistant.log")
        return "HAndroid ${BuildConfig.VERSION_NAME}\n\n== logcat ==\n$logcat\n== home-assistant.log ==\n" +
            (if (ha.exists()) coda(ha, 300) else "(non ancora creato)")
    }

    /** Ultime [righe] di un file, leggendo solo la fine (il log di HA può crescere molto). */
    fun coda(file: File, righe: Int, maxByte: Long = 256 * 1024): String = RandomAccessFile(file, "r").use { f ->
        val inizio = maxOf(0, f.length() - maxByte)
        f.seek(inizio)
        val testo = ByteArray((f.length() - inizio).toInt()).also { f.readFully(it) }.toString(Charsets.UTF_8)
        testo.lines().let { if (inizio > 0) it.drop(1) else it }.takeLast(righe).joinToString("\n")
    }

    /** Spazio occupato dalle parti di HAndroid, più lo spazio libero. */
    fun spazio(context: Context): List<Pair<String, Long>> {
        val config = File(context.filesDir, "ha-config")
        val pip = cachePip(context)
        return listOf(
            "Python e Home Assistant" to dimensione(File(context.filesDir, "python")),
            "Pacchetti installati" to dimensione(File(config, "deps")),
            "Database (storico)" to (config.listFiles { f -> f.name.startsWith("home-assistant_v2.db") }?.sumOf { it.length() } ?: 0L),
            "Backup" to dimensione(File(config, "backups")),
            "Add-on" to dimensione(File(context.filesDir, "addons")),
            "Cache" to dimensione(pip) + dimensione(context.cacheDir),
            "Spazio libero" to context.filesDir.usableSpace,
        )
    }

    fun dimensione(f: File): Long = if (!f.exists()) 0 else f.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun formatta(byte: Long): String = when {
        byte >= 1L shl 30 -> "%.1f GB".format(byte / (1L shl 30).toDouble())
        byte >= 1L shl 20 -> "%.0f MB".format(byte / (1L shl 20).toDouble())
        else -> "%.0f KB".format(byte / 1024.0)
    }

    // HOME di Python è files/ (handroid_boot.py): pip tiene lì la sua cache
    private fun cachePip(context: Context) = File(context.filesDir, ".cache/pip")

    /** Cancella la cache di pip e quella dell'app; restituisce i byte liberati. */
    fun svuotaCache(context: Context): Long {
        val pip = cachePip(context)
        val prima = dimensione(pip) + dimensione(context.cacheDir)
        pip.deleteRecursively()
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        return prima - dimensione(pip) - dimensione(context.cacheDir)
    }

    /** Cancella la configurazione di HA (utenti, integrazioni, storico); tiene i backup. HA va fermato prima. */
    fun azzera(context: Context) {
        val config = File(context.filesDir, "ha-config")
        config.listFiles()?.filter { it.name != "backups" }?.forEach { it.deleteRecursively() }
    }
}
