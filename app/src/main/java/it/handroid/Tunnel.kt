package it.handroid

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

/**
 * Accesso da fuori casa senza costi con Cloudflare Tunnel (docs/accesso-remoto.md): cloudflared, nell'APK come
 * libcloudflared.so, apre un collegamento in uscita verso Cloudflare e porta il dominio dell'utente su :8123.
 * Gira nel processo :ha accanto a HA; il token sta in files/cloudflare-token.txt, letto solo dall'app.
 */
object Tunnel {
    private const val TAG = "HAndroid"

    fun tokenFile(context: Context) = File(context.filesDir, "cloudflare-token.txt")
    fun statoFile(context: Context) = File(context.filesDir, "tunnel-stato.txt")

    fun token(context: Context) = tokenFile(context).takeIf { it.exists() }?.readText()?.trim().orEmpty()

    /** Un token di Cloudflare Tunnel è JSON in base64 con account (a), tunnel (t) e segreto (s). */
    fun valido(token: String) = runCatching {
        val json = JSONObject(String(java.util.Base64.getDecoder().decode(token.trim())))
        listOf("a", "t", "s").all { json.optString(it).isNotEmpty() }
    }.getOrDefault(false)

    /** Accetta anche il comando intero che mostra Cloudflare ("cloudflared ... --token eyJ..."). */
    fun estrai(testo: String) = testo.trim().split(Regex("\\s+")).lastOrNull { valido(it) }

    fun salva(context: Context, token: String?) {
        if (token == null) tokenFile(context).delete() else tokenFile(context).writeText(token)
        statoFile(context).delete()
    }

    fun stato(context: Context) = when {
        token(context).isEmpty() -> "spento"
        else -> statoFile(context).takeIf { it.exists() }?.readText() ?: "in avvio (dopo il riavvio di HA)"
    }

    /** Dal servizio di HA: fa girare cloudflared finché c'è un token, e lo riavvia se esce. */
    fun avvia(context: Context) {
        val token = token(context)
        if (token.isEmpty()) return
        val exe = File(context.applicationInfo.nativeLibraryDir, "libcloudflared.so")
        if (!exe.exists()) return statoFile(context).writeText("cloudflared non è in questo APK")
        thread(name = "tunnel", isDaemon = true) {
            var attesa = 10_000L
            while (token(context) == token) {
                statoFile(context).writeText("in collegamento…")
                // il token passa dall'ambiente: negli argomenti lo vedrebbe chiunque legga l'elenco dei processi
                val p = ProcessBuilder(exe.path, "tunnel", "--no-autoupdate", "run")
                    .redirectErrorStream(true)
                    .apply { environment()["TUNNEL_TOKEN"] = token; environment()["HOME"] = context.filesDir.path }
                    .start()
                p.inputStream.bufferedReader().forEachLine { riga ->
                    Log.i(TAG, "tunnel: $riga")
                    if ("Registered tunnel connection" in riga) { statoFile(context).writeText("collegato"); attesa = 10_000L }
                    if ("Unauthorized" in riga || "invalid token" in riga.lowercase()) statoFile(context).writeText("token rifiutato da Cloudflare")
                }
                Log.w(TAG, "cloudflared uscito con codice ${p.waitFor()}, riparto tra ${attesa / 1000}s")
                if (statoFile(context).readText() == "collegato") statoFile(context).writeText("scollegato, riprovo…")
                Thread.sleep(attesa)
                attesa = minOf(attesa * 2, 5 * 60_000L)
            }
        }
    }
}
