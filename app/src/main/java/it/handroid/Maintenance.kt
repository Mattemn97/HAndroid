package it.handroid

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Backup e aggiornamenti: operazioni lente, da chiamare fuori dal thread dell'interfaccia. */
object Maintenance {
    /** Repository dei rilasci: l'APK nuovo contiene anche la nuova versione di Home Assistant. */
    private const val RELEASES = "https://api.github.com/repos/Mattemn97/HAndroid/releases/latest"

    /**
     * Copia in Download/HAndroid l'ultimo backup creato da Home Assistant (Impostazioni → Sistema →
     * Backup). Così il backup esce dalla memoria privata dell'app e sopravvive a una disinstallazione.
     * Restituisce il nome del file copiato, o null se non ci sono backup.
     */
    fun exportLatestBackup(context: Context): String? {
        val latest = File(context.filesDir, "ha-config/backups").listFiles { f -> f.name.endsWith(".tar") }
            ?.maxByOrNull { it.lastModified() } ?: return null
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, latest.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/x-tar")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HAndroid")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Impossibile creare il file in Download")
            context.contentResolver.openOutputStream(uri)!!.use { out -> latest.inputStream().use { it.copyTo(out) } }
        } else {
            error("Serve Android 10 o successivo")
        }
        return latest.name
    }

    /** Ultimo rilascio pubblicato: versione e indirizzo dell'APK, o null se non è più recente di questo. */
    fun newerRelease(): Pair<String, String>? {
        val connection = URL(RELEASES).openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        if (connection.responseCode != 200) return null  // 404: nessun rilascio pubblicato
        val release = JSONObject(connection.inputStream.bufferedReader().readText())
        val version = release.getString("tag_name").removePrefix("v")
        if (!isNewer(version, BuildConfig.VERSION_NAME)) return null
        val assets = release.getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) return version to asset.getString("browser_download_url")
        }
        return null
    }

    /** Confronta versioni tipo "0.2.0" e "0.1.3-m1" sulle parti numeriche. */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
