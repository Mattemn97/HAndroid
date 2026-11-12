package it.handroid.addon

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * API HTTP locale del gestore add-on, solo su 127.0.0.1:[PORTA], con token Bearer letto
 * da `<addons>/token`. La usa l'integrazione custom_components/handroid dentro HA.
 *
 *   GET  /addons                       elenco con stato
 *   GET  /addons/<slug>/log            ultime righe del log
 *   POST /addons/<slug>/{install|start|stop|uninstall}
 *   POST /addons/<slug>/options        {"options": {...}, "avvio_auto": true}
 *   POST /repositories                 {"url": "https://…"}   (DELETE per toglierlo)
 *   POST /refresh                      rilegge i repository
 */
object AddonApi {
    const val PORTA = 8099
    private const val TAG = "HAndroid"

    fun avvia(dir: File) {
        val token = token(dir)
        Thread({
            runCatching {
                ServerSocket(PORTA, 16, InetAddress.getByName("127.0.0.1")).use { s ->
                    while (true) {
                        val c = s.accept()
                        Thread({ c.use { servi(it, token) } }, "addon-api").start()
                    }
                }
            }.onFailure { Log.e(TAG, "API add-on non avviata", it) }
        }, "addon-api").start()
    }

    /** Token casuale creato al primo avvio; il file è leggibile solo dall'app (e quindi da HA). */
    fun token(dir: File): String {
        dir.mkdirs()
        val f = File(dir, "token")
        if (!f.exists()) {
            val b = ByteArray(24).also { SecureRandom().nextBytes(it) }
            f.writeText(b.joinToString("") { "%02x".format(it) })
        }
        return f.readText().trim()
    }

    private fun servi(c: Socket, token: String) {
        c.soTimeout = 10_000
        val inp = c.getInputStream().buffered()
        val prima = leggiRiga(inp) ?: return
        val (metodo, percorso) = prima.split(" ").let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") }
        val header = HashMap<String, String>()
        while (true) {
            val r = leggiRiga(inp) ?: break
            if (r.isEmpty()) break
            val i = r.indexOf(':')
            if (i > 0) header[r.substring(0, i).trim().lowercase()] = r.substring(i + 1).trim()
        }
        val n = header["content-length"]?.toIntOrNull()?.coerceIn(0, 1 shl 20) ?: 0
        // readNBytes è solo da API 33: si legge a mano.
        val buf = ByteArray(n)
        var letti = 0
        while (letti < n) {
            val k = inp.read(buf, letti, n - letti)
            if (k < 0) break
            letti += k
        }
        val corpo = String(buf, 0, letti)
        val (codice, risposta) = if (!autorizzato(header["authorization"], token)) 401 to errore("token mancante o errato")
        else runCatching { gestisci(metodo, percorso, corpo) }.getOrElse {
            Log.w(TAG, "API add-on: $metodo $percorso: ${it.message}")
            (if (it is IllegalArgumentException) 400 else 500) to errore(it.message ?: it.toString())
        }
        val b = risposta.toByteArray()
        c.getOutputStream().apply {
            write(("HTTP/1.1 $codice OK\r\nContent-Type: application/json\r\nContent-Length: ${b.size}\r\n" +
                "Connection: close\r\n\r\n").toByteArray())
            write(b)
            flush()
        }
    }

    /** Confronto a tempo costante. */
    fun autorizzato(header: String?, token: String) = header != null &&
        MessageDigest.isEqual(header.removePrefix("Bearer ").trim().toByteArray(), token.toByteArray())

    /** Instradamento: separato dal socket per poterlo leggere (e provare) da solo. */
    fun gestisci(metodo: String, percorso: String, corpo: String): Pair<Int, String> {
        val p = percorso.substringBefore('?').trim('/').split('/')
        val m = AddonManager
        val ok = JSONObject().put("ok", true).toString()
        return when {
            metodo == "GET" && p == listOf("addons") -> 200 to m.elenco().toString()
            metodo == "GET" && p.size == 3 && p[0] == "addons" && p[2] == "log" ->
                200 to JSONObject().put("log", m.log(p[1])).toString()
            metodo == "POST" && p == listOf("refresh") -> { m.aggiornaCatalogo(); 200 to m.elenco().toString() }
            p == listOf("repositories") && (metodo == "POST" || metodo == "DELETE") -> {
                val url = JSONObject(corpo).getString("url")
                if (metodo == "POST") m.stato.aggiungiRepository(url) else m.stato.rimuoviRepository(url)
                m.aggiornaCatalogo()
                200 to ok
            }
            metodo == "POST" && p.size == 3 && p[0] == "addons" -> {
                val slug = p[1]
                when (p[2]) {
                    "install" -> m.installa(slug)
                    "start" -> m.avviaAddon(slug)
                    "stop" -> m.fermaAddon(slug)
                    "uninstall" -> m.rimuovi(slug)
                    "options" -> {
                        val o = JSONObject(corpo)
                        val err = m.impostaOpzioni(slug, Json.mappa(o.optJSONObject("options") ?: JSONObject()),
                            if (o.has("avvio_auto")) o.getBoolean("avvio_auto") else null)
                        require(err.isEmpty()) { err.joinToString("; ") }
                    }
                    else -> return 404 to errore("azione sconosciuta")
                }
                200 to ok
            }
            else -> 404 to errore("percorso sconosciuto")
        }
    }

    private fun errore(msg: String) = JSONObject().put("ok", false).put("errore", msg).toString()

    private fun leggiRiga(inp: java.io.InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = inp.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().trimEnd('\r')
            if (sb.length > 8192) return null
            sb.append(b.toChar())
        }
    }
}
