package it.handroid.esphome

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Numeri dei messaggi di api.proto (esphome/components/api/api.proto) usati qui. */
object Msg {
    const val HELLO_REQ = 1; const val HELLO_RESP = 2
    const val AUTH_REQ = 3; const val AUTH_RESP = 4
    const val DISCONNECT_REQ = 5; const val DISCONNECT_RESP = 6
    const val PING_REQ = 7; const val PING_RESP = 8
    const val DEVICE_INFO_REQ = 9; const val DEVICE_INFO_RESP = 10
    const val LIST_ENTITIES_REQ = 11
    const val LIST_BINARY_SENSOR = 12; const val LIST_SENSOR = 16; const val LIST_SWITCH = 17
    const val LIST_TEXT_SENSOR = 18; const val LIST_DONE = 19; const val LIST_BUTTON = 61
    const val SUBSCRIBE_STATES = 20
    const val BINARY_SENSOR_STATE = 21; const val SENSOR_STATE = 25; const val SWITCH_STATE = 26
    const val TEXT_SENSOR_STATE = 27
    const val SUBSCRIBE_LOGS = 28; const val SWITCH_COMMAND = 33
    const val SUBSCRIBE_HA_SERVICES = 34; const val GET_TIME_REQ = 36; const val GET_TIME_RESP = 37
    const val SUBSCRIBE_HA_STATES = 38; const val BUTTON_COMMAND = 62
}

/** Un messaggio pronto da spedire: tipo e payload protobuf. */
class Messaggio(val tipo: Int, val payload: ByteArray)

/**
 * Un'entità del dispositivo. La chiave è l'hash FNV-1a dell'object_id, come in ESPHome:
 * resta uguale tra un riavvio e l'altro, così HA non duplica le entità.
 */
sealed class Entita(val objectId: String, val nome: String, val icona: String?, val diagnostica: Boolean) {
    val key: Int = fnv1a(objectId)

    abstract fun descrizione(): Messaggio
    /** Stato corrente, o `null` se l'entità non ha stato (pulsanti) o non ne ha ancora uno. */
    abstract fun stato(): Messaggio?

    protected fun base(w: ProtoWriter) = w.string(1, objectId).fixed32(2, key).string(3, nome)
    protected val categoria get() = if (diagnostica) 2L else 0L

    companion object {
        fun fnv1a(s: String): Int {
            var h = 2166136261L.toInt()
            for (b in s.toByteArray(Charsets.UTF_8)) h = (h xor (b.toInt() and 0xFF)) * 16777619
            return h
        }
    }
}

class Sensore(
    objectId: String, nome: String, icona: String? = null, diagnostica: Boolean = false,
    val unita: String? = null, val decimali: Int = 0, val deviceClass: String? = null,
    /** 1 = measurement, 2 = total_increasing (SensorStateClass). */
    val stateClass: Int = 1,
    /** Variazione minima perché valga la pena inviare un aggiornamento. */
    val soglia: Float = 0f,
) : Entita(objectId, nome, icona, diagnostica) {
    @Volatile var valore: Float? = null

    override fun descrizione() = Messaggio(Msg.LIST_SENSOR, base(ProtoWriter()).string(5, icona).string(6, unita)
        .uint(7, decimali).string(9, deviceClass).uint(10, stateClass).varint(13, categoria).toByteArray())

    override fun stato() = valore?.let { Messaggio(Msg.SENSOR_STATE, ProtoWriter().fixed32(1, key).float(2, it).toByteArray()) }
}

class SensoreBinario(
    objectId: String, nome: String, icona: String? = null, diagnostica: Boolean = false,
    val deviceClass: String? = null,
) : Entita(objectId, nome, icona, diagnostica) {
    @Volatile var valore: Boolean? = null

    override fun descrizione() = Messaggio(Msg.LIST_BINARY_SENSOR, base(ProtoWriter()).string(5, deviceClass)
        .string(8, icona).varint(9, categoria).toByteArray())

    override fun stato() = valore?.let { Messaggio(Msg.BINARY_SENSOR_STATE, ProtoWriter().fixed32(1, key).bool(2, it).toByteArray()) }
}

class Interruttore(
    objectId: String, nome: String, icona: String? = null,
    /** Esegue il comando di HA; lo stato vero arriva poi con `Registro.aggiorna`. */
    val comando: (Boolean) -> Unit,
) : Entita(objectId, nome, icona, false) {
    @Volatile var valore: Boolean? = null

    override fun descrizione() = Messaggio(Msg.LIST_SWITCH, base(ProtoWriter()).string(5, icona).toByteArray())
    override fun stato() = valore?.let { Messaggio(Msg.SWITCH_STATE, ProtoWriter().fixed32(1, key).bool(2, it).toByteArray()) }
}

class Pulsante(
    objectId: String, nome: String, icona: String? = null, diagnostica: Boolean = false,
    val deviceClass: String? = null, val comando: () -> Unit,
) : Entita(objectId, nome, icona, diagnostica) {
    override fun descrizione() = Messaggio(Msg.LIST_BUTTON, base(ProtoWriter()).string(5, icona)
        .varint(7, categoria).string(8, deviceClass).toByteArray())
    override fun stato(): Messaggio? = null
}

/**
 * Registro delle entità. Chi produce i valori chiama `aggiorna`; il registro scarta le
 * variazioni sotto soglia e invia al massimo un aggiornamento ogni `intervalloMinMs`
 * per entità (l'ultimo valore non va perso: parte allo scadere dell'intervallo).
 */
class Registro(private val intervalloMinMs: Long = 5_000) {
    val entita = CopyOnWriteArrayList<Entita>()
    private val ascoltatori = CopyOnWriteArrayList<(Messaggio) -> Unit>()
    private val ultimoInvio = HashMap<Int, Long>()
    private val inviato = HashMap<Int, Any?>()
    private val inAttesa = HashSet<Int>()
    private val timer = Executors.newSingleThreadScheduledExecutor { Thread(it, "esphome-timer").apply { isDaemon = true } }

    fun <T : Entita> aggiungi(e: T): T = e.also { entita += it }
    fun cerca(key: Int) = entita.firstOrNull { it.key == key }

    fun ascolta(f: (Messaggio) -> Unit) { ascoltatori += f }
    fun smetti(f: (Messaggio) -> Unit) { ascoltatori -= f }

    fun aggiorna(e: Sensore, v: Float) { e.valore = v; pianifica(e, v) }
    fun aggiorna(e: SensoreBinario, v: Boolean) { e.valore = v; pianifica(e, v) }
    fun aggiorna(e: Interruttore, v: Boolean) { e.valore = v; pianifica(e, v) }

    @Synchronized private fun pianifica(e: Entita, v: Any) {
        val prec = inviato[e.key]
        if (prec == v) return
        if (e is Sensore && prec is Float && kotlin.math.abs(prec - (v as Float)) < e.soglia) return
        if (e.key in inAttesa) return
        val attesa = (ultimoInvio[e.key] ?: 0L) + intervalloMinMs - System.currentTimeMillis()
        // Gli interruttori rispondono subito: HA aspetta la conferma del comando.
        if (attesa <= 0 || e is Interruttore) invia(e)
        else { inAttesa += e.key; timer.schedule({ synchronized(this) { inAttesa -= e.key; invia(e) } }, attesa, TimeUnit.MILLISECONDS) }
    }

    private fun invia(e: Entita) {
        val m = e.stato() ?: return
        ultimoInvio[e.key] = System.currentTimeMillis()
        inviato[e.key] = when (e) { is Sensore -> e.valore; is SensoreBinario -> e.valore; is Interruttore -> e.valore; else -> null }
        ascoltatori.forEach { it(m) }
    }
}
