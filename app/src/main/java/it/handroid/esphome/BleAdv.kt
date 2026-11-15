package it.handroid.esphome

/**
 * Contratto del proxy di ble_adv (NicoIIT/ha-ble-adv, lampade e ventilatori a annunci BLE
 * come FanLamp Pro e LampSmart), lo stesso del componente ESPHome `ble_adv_proxy`:
 * sensore di testo `ble_adv_proxy_name`, azioni `setup_svc_v0` e `adv_svc_v1`, evento
 * `esphome.ble_adv.raw_adv` per gli annunci ricevuti. Niente Android qui: si prova sulla JVM.
 */
object BleAdv {
    const val EVENTO = "esphome.ble_adv.raw_adv"
    const val SENSORE = "ble_adv_proxy_name"
    // ServiceArgType di api.proto.
    const val FLOAT = 2; const val STRING = 3; const val FLOAT_ARRAY = 6; const val STRING_ARRAY = 7
    // Lunghezza massima di un annuncio legacy, come in ble_adv_proxy.
    const val MAX_ANNUNCIO = 31

    class Servizio(val nome: String, val argomenti: List<Pair<String, Int>>) {
        val key = Entita.fnv1a("servizio_$nome")
        fun descrizione() = Messaggio(Msg.LIST_SERVICES, ProtoWriter().string(1, nome).fixed32(2, key).apply {
            argomenti.forEach { (n, t) -> messaggio(3, ProtoWriter().string(1, n).uint(2, t)) }
        }.toByteArray())
    }

    val SETUP = Servizio("setup_svc_v0", listOf("ignored_duration" to FLOAT, "ignored_cids" to FLOAT_ARRAY, "ignored_macs" to STRING_ARRAY))
    val ADV = Servizio("adv_svc_v1", listOf("raw" to STRING, "duration" to FLOAT, "repeat" to FLOAT,
        "ignored_advs" to STRING_ARRAY, "ignored_duration" to FLOAT))

    /** Un argomento di ExecuteServiceRequest: si legge il campo che serve al tipo dichiarato. */
    class Argomento(val float: Float = 0f, val string: String = "", val floats: List<Float> = emptyList(), val strings: List<String> = emptyList())

    /** ExecuteServiceRequest → chiave del servizio e argomenti nell'ordine di dichiarazione. */
    fun esegui(payload: ByteArray): Pair<Int, List<Argomento>> {
        val campi = ProtoReader(payload).lista()
        val key = campi.firstOrNull { it.numero == 1 }?.int ?: 0
        val args = campi.filter { it.numero == 2 && it.dati != null }.map { arg ->
            var f = 0f; var s = ""; val fs = ArrayList<Float>(); val ss = ArrayList<String>()
            for (c in ProtoReader(arg.dati!!).lista()) when (c.numero) {
                3 -> f = c.float
                4 -> s = c.string
                // float_array: un campo per valore, o "packed" (blocco di 4 byte per valore).
                8 -> if (c.dati != null) for (i in 0 until c.dati.size / 4) fs += java.lang.Float.intBitsToFloat(
                    (0 until 4).fold(0) { a, k -> a or ((c.dati[i * 4 + k].toInt() and 0xFF) shl (8 * k)) }) else fs += c.float
                9 -> ss += c.string
            }
            Argomento(f, s, fs, ss)
        }
        return key to args
    }

    /** Evento per HA (HomeassistantActionRequest con is_event). HA aggiunge da sé il device_id. */
    fun evento(mac: String, raw: ByteArray) = Messaggio(Msg.HA_SERVICE_RESP, ProtoWriter().string(1, EVENTO)
        .messaggio(2, ProtoWriter().string(1, "raw").string(2, hex(raw)))
        .messaggio(2, ProtoWriter().string(1, "orig").string(2, mac))
        .bool(5, true).toByteArray())

    fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    fun daHex(s: String): ByteArray = s.trim().chunked(2).filter { it.length == 2 }.map { it.toInt(16).toByte() }.toByteArray()

    /** Strutture AD di un annuncio: tipo e dati. Si ferma alla prima struttura vuota o troncata. */
    fun strutture(raw: ByteArray): List<Pair<Int, ByteArray>> {
        val l = ArrayList<Pair<Int, ByteArray>>()
        var i = 0
        while (i < raw.size) {
            val n = raw[i].toInt() and 0xFF
            if (n == 0 || i + 1 + n > raw.size) break
            l += (raw[i + 1].toInt() and 0xFF) to raw.copyOfRange(i + 2, i + 1 + n)
            i += 1 + n
        }
        return l
    }

    /**
     * Annuncio scomposto in ciò che `AdvertiseData` di Android sa trasmettere: UUID a 16 bit
     * (in ordine), service data a 16 bit e manufacturer data. I flag (tipo 0x01) li toglie:
     * Android non li mette sugli annunci non connettibili, e le lampade non li guardano.
     */
    class Contenuto(val uuid16: List<Int>, val serviceData: List<Pair<Int, ByteArray>>, val produttore: List<Pair<Int, ByteArray>>)

    /** `null` se l'annuncio contiene strutture che Android non sa ricreare. */
    fun scomponi(raw: ByteArray): Contenuto? {
        val uuid = ArrayList<Int>(); val sd = ArrayList<Pair<Int, ByteArray>>(); val md = ArrayList<Pair<Int, ByteArray>>()
        for ((tipo, d) in strutture(raw.copyOf(minOf(raw.size, MAX_ANNUNCIO)))) when (tipo) {
            0x01 -> {}
            0x02, 0x03 -> { if (d.size % 2 != 0) return null; for (i in d.indices step 2) uuid += u16(d, i) }
            0x16 -> { if (d.size < 2) return null; sd += u16(d, 0) to d.copyOfRange(2, d.size) }
            0xFF -> { if (d.size < 2) return null; md += u16(d, 0) to d.copyOfRange(2, d.size) }
            else -> return null
        }
        // Android tiene un solo manufacturer data per produttore: due uguali non si ricreano.
        if (md.map { it.first }.toSet().size != md.size) return null
        return Contenuto(uuid, sd, md)
    }

    /** Company ID del manufacturer data, se c'è (per `ignored_cids`). */
    fun produttore(raw: ByteArray) = strutture(raw).firstOrNull { it.first == 0xFF && it.second.size >= 2 }?.let { u16(it.second, 0) }

    private fun u16(d: ByteArray, i: Int) = (d[i].toInt() and 0xFF) or ((d[i + 1].toInt() and 0xFF) shl 8)

    /**
     * Filtro degli annunci ricevuti, come ble_adv_proxy: scarta per `ignorati` ms quelli già
     * visti e quelli appena trasmessi, più produttori e MAC esclusi da `setup_svc_v0`.
     */
    class Filtro(private val ora: () -> Long = System::currentTimeMillis) {
        @Volatile var durata = 20_000L
        @Volatile var cids: Set<Int> = emptySet()
        @Volatile var macs: Set<String> = emptySet()
        private val visti = HashMap<String, Long>()

        @Synchronized fun ignora(raw: String, ms: Long) { visti[raw.lowercase()] = ora() + ms }

        @Synchronized fun passa(mac: String, raw: ByteArray): Boolean {
            if (raw.size < 5 || raw.size > MAX_ANNUNCIO) return false
            if (mac.uppercase() in macs || produttore(raw) in cids) return false
            val t = ora()
            if (visti.size > 500) visti.values.removeAll { it < t }
            val k = hex(raw)
            if ((visti[k] ?: 0L) > t) return false
            visti[k] = t + durata
            return true
        }
    }
}
