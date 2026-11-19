package it.handroid

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Avviso se il server si ferma: finché HA risponde, il watchdog chiama ogni 5 minuti un URL di controllo
 * (es. healthchecks.io, gratis). Se le chiamate smettono, è il servizio esterno ad avvisare l'utente.
 */
object Controllo {
    fun file(context: Context) = File(context.filesDir, "controllo-url.txt")

    fun url(context: Context) = file(context).takeIf { it.exists() }?.readText()?.trim().orEmpty()

    fun valido(url: String) = Regex("^https?://[^\\s/]+\\.[^\\s]+$").matches(url.trim())

    fun salva(context: Context, url: String?) {
        if (url.isNullOrBlank()) file(context).delete() else file(context).writeText(url.trim())
    }

    /** Dal watchdog, solo con HA che risponde. Gli errori di rete si ignorano: il prossimo giro riprova. */
    fun ping(context: Context) {
        val url = url(context).takeIf { it.isNotEmpty() } ?: return
        runCatching {
            (URL(url).openConnection() as HttpURLConnection).run {
                connectTimeout = 10_000
                readTimeout = 10_000
                responseCode
                disconnect()
            }
        }
    }
}
