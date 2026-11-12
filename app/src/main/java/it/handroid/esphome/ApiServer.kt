package it.handroid.esphome

import android.util.Log
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/** Dati fissi del dispositivo, inviati in HelloResponse e DeviceInfoResponse. */
data class InfoDispositivo(
    val nome: String,
    val nomeVisibile: String,
    val mac: String,
    val modello: String,
    val produttore: String,
    val versione: String,
)

/**
 * Estensione del server (es. il proxy Bluetooth): riceve i messaggi che il nucleo non
 * gestisce e può aggiungere campi a DeviceInfoResponse (es. bluetooth_proxy_feature_flags).
 */
interface Modulo {
    /** `true` se il messaggio è stato gestito. */
    fun gestisci(conn: Connessione, tipo: Int, payload: ByteArray): Boolean
    fun deviceInfo(w: ProtoWriter) {}
    fun chiusa(conn: Connessione) {}
}

/** Una connessione di un client (Home Assistant). `invia` è sicuro da più thread. */
class Connessione(private val socket: Socket) {
    private val out: OutputStream = socket.getOutputStream()
    @Volatile var statiAttivi = false

    fun invia(m: Messaggio) = invia(m.tipo, m.payload)
    fun invia(tipo: Int, payload: ByteArray = ByteArray(0)) {
        val f = Frame.codifica(tipo, payload)
        synchronized(out) { out.write(f); out.flush() }
    }
    fun chiudi() = runCatching { socket.close() }
}

/**
 * Server dell'API nativa ESPHome (protocollo in chiaro, senza password). Un thread
 * per connessione: i client sono pochi (di solito solo HA sullo stesso telefono).
 * Con `soloLocale` accetta solo connessioni dal telefono stesso, anche se ascolta su
 * tutte le interfacce: serve perché via mDNS HA vede l'indirizzo LAN, non 127.0.0.1.
 */
class ApiServer(
    private val info: InfoDispositivo,
    val registro: Registro,
    private val porta: Int = 6053,
    private val soloLocale: Boolean = true,
) {
    val moduli = mutableListOf<Modulo>()
    private var server: ServerSocket? = null
    val portaEffettiva get() = server?.localPort ?: porta

    fun avvia() {
        val s = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(porta)) }
        server = s
        Thread({
            while (!s.isClosed) {
                val c = runCatching { s.accept() }.getOrNull() ?: continue
                if (soloLocale && !daQuestoTelefono(c)) { Log.w(TAG, "Rifiutata ${c.inetAddress}"); c.close(); continue }
                Thread({ servi(c) }, "esphome-conn").start()
            }
        }, "esphome-api").start()
    }

    fun ferma() = runCatching { server?.close() }

    private fun daQuestoTelefono(c: Socket): Boolean {
        val a: InetAddress = c.inetAddress
        return a.isLoopbackAddress || a == c.localAddress
    }

    private fun servi(socket: Socket) {
        socket.tcpNoDelay = true
        val conn = Connessione(socket)
        val ascolta: (Messaggio) -> Unit = { m -> if (conn.statiAttivi) runCatching { conn.invia(m) }.onFailure { conn.chiudi() } }
        registro.ascolta(ascolta)
        try {
            val input = BufferedInputStream(socket.getInputStream())
            while (true) {
                val (tipo, payload) = Frame.leggi(input) ?: break
                if (!gestisci(conn, tipo, payload)) break
            }
        } catch (e: Exception) {
            Log.i(TAG, "Connessione chiusa: ${e.message}")
        } finally {
            registro.smetti(ascolta)
            moduli.forEach { runCatching { it.chiusa(conn) } }
            conn.chiudi()
        }
    }

    /** Gestisce un messaggio; `false` se la connessione va chiusa. */
    internal fun gestisci(conn: Connessione, tipo: Int, payload: ByteArray): Boolean {
        when (tipo) {
            Msg.HELLO_REQ -> conn.invia(Msg.HELLO_RESP, ProtoWriter().uint(1, API_MAJOR).uint(2, API_MINOR)
                .string(3, "HAndroid ${info.versione}").string(4, info.nome).toByteArray())
            Msg.AUTH_REQ -> conn.invia(Msg.AUTH_RESP)
            Msg.DISCONNECT_REQ -> { conn.invia(Msg.DISCONNECT_RESP); return false }
            Msg.PING_REQ -> conn.invia(Msg.PING_RESP)
            Msg.DEVICE_INFO_REQ -> conn.invia(Msg.DEVICE_INFO_RESP, deviceInfo())
            Msg.LIST_ENTITIES_REQ -> {
                registro.entita.forEach { conn.invia(it.descrizione()) }
                conn.invia(Msg.LIST_DONE)
            }
            Msg.SUBSCRIBE_STATES -> {
                conn.statiAttivi = true
                registro.entita.forEach { e -> e.stato()?.let { conn.invia(it) } }
            }
            Msg.SWITCH_COMMAND -> {
                val c = ProtoReader(payload).campi()
                (registro.cerca(c[1]?.int ?: 0) as? Interruttore)?.let { s -> runCatching { s.comando(c[2]?.bool ?: false) } }
            }
            Msg.BUTTON_COMMAND -> {
                val c = ProtoReader(payload).campi()
                (registro.cerca(c[1]?.int ?: 0) as? Pulsante)?.let { b -> runCatching { b.comando() } }
            }
            Msg.GET_TIME_REQ -> conn.invia(Msg.GET_TIME_RESP,
                ProtoWriter().fixed32(1, (System.currentTimeMillis() / 1000).toInt()).toByteArray())
            // Log, servizi e stati di HA: niente da mandare, ma la sottoscrizione è valida.
            Msg.SUBSCRIBE_LOGS, Msg.SUBSCRIBE_HA_SERVICES, Msg.SUBSCRIBE_HA_STATES -> {}
            else -> if (moduli.none { it.gestisci(conn, tipo, payload) }) Log.d(TAG, "Messaggio $tipo ignorato")
        }
        return true
    }

    internal fun deviceInfo(): ByteArray {
        val w = ProtoWriter().string(2, info.nome).string(3, info.mac).string(4, info.versione)
            .string(5, COMPILATO).string(6, info.modello).string(12, info.produttore).string(13, info.nomeVisibile)
        moduli.forEach { it.deviceInfo(w) }
        return w.toByteArray()
    }

    companion object {
        const val TAG = "HAndroid-ESPHome"
        // 1.10 basta per sensori, switch e pulsanti; i client più nuovi si adattano.
        const val API_MAJOR = 1
        const val API_MINOR = 10
        private const val COMPILATO = "HAndroid"
    }
}
