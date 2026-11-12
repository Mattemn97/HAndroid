package it.handroid.esphome

import java.util.UUID

/**
 * Parte del proxy Bluetooth che non usa API Android (spec 0005): numeri dei messaggi,
 * codifica, lotti di annunci, slot delle connessioni, handle GATT e coda delle operazioni.
 * `ProxyBle` la collega a BluetoothLeScanner e BluetoothGatt.
 */
object MsgBle {
    const val SUBSCRIBE_ADV = 66; const val DEVICE_REQ = 68; const val CONNECTION_RESP = 69
    const val GET_SERVICES_REQ = 70; const val GET_SERVICES_RESP = 71; const val GET_SERVICES_DONE = 72
    const val READ_REQ = 73; const val READ_RESP = 74; const val WRITE_REQ = 75
    const val READ_DESC_REQ = 76; const val WRITE_DESC_REQ = 77
    const val NOTIFY_REQ = 78; const val NOTIFY_DATA = 79
    const val SUBSCRIBE_FREE = 80; const val FREE_RESP = 81
    const val GATT_ERROR = 82; const val WRITE_RESP = 83; const val NOTIFY_RESP = 84
    const val PAIRING_RESP = 85; const val UNPAIRING_RESP = 86
    const val UNSUBSCRIBE_ADV = 87; const val CLEAR_CACHE_RESP = 88
    const val RAW_ADV = 93
    const val SCANNER_STATE = 126; const val SCANNER_SET_MODE = 127

    // BluetoothDeviceRequestType
    const val REQ_DISCONNECT = 1; const val REQ_PAIR = 2; const val REQ_UNPAIR = 3
    const val REQ_CONNECT_CACHE = 4; const val REQ_CONNECT_NO_CACHE = 5; const val REQ_CLEAR_CACHE = 6

    // BluetoothProxyFeature (bluetooth_proxy.h)
    const val F_PASSIVE_SCAN = 1; const val F_ACTIVE_CONNECTIONS = 2; const val F_REMOTE_CACHING = 4
    const val F_PAIRING = 8; const val F_CACHE_CLEARING = 16; const val F_RAW_ADVERTISEMENTS = 32
    const val F_STATE_AND_MODE = 64

    /** Quello che HAndroid dichiara: niente pairing né svuotamento cache (vedi spec 0005, B5). */
    const val FEATURES = F_PASSIVE_SCAN or F_ACTIVE_CONNECTIONS or F_REMOTE_CACHING or
        F_RAW_ADVERTISEMENTS or F_STATE_AND_MODE

    // BluetoothScannerState / Mode
    const val STATO_RUNNING = 2; const val STATO_FAILED = 3; const val STATO_STOPPED = 5
    const val MODO_PASSIVO = 0; const val MODO_ATTIVO = 1
}

/** Un annuncio ricevuto: MAC come intero a 48 bit, dati AD grezzi (annuncio + scan response). */
class Annuncio(val indirizzo: Long, val rssi: Int, val tipoIndirizzo: Int, val dati: ByteArray)

object CodecBle {
    /** "AA:BB:CC:DD:EE:FF" -> 0xAABBCCDDEEFF, come `address` nei messaggi ESPHome. */
    fun indirizzo(mac: String): Long = mac.replace(":", "").toLong(16)
    fun mac(indirizzo: Long): String = (5 downTo 0).joinToString(":") { "%02X".format((indirizzo ushr (8 * it)) and 0xFF) }

    /**
     * Android restituisce i dati dell'annuncio con zeri in coda fino a 62 byte: si tengono
     * solo le strutture AD valide (lunghezza, tipo, dati), fermandosi alla prima lunghezza 0.
     */
    fun taglia(raw: ByteArray?): ByteArray {
        if (raw == null) return ByteArray(0)
        var i = 0
        while (i < raw.size) {
            val n = raw[i].toInt() and 0xFF
            if (n == 0 || i + 1 + n > raw.size) break
            i += 1 + n
        }
        return raw.copyOf(minOf(i, 62))
    }

    fun annuncio(a: Annuncio): ByteArray = ProtoWriter().varint(1, a.indirizzo).sint(2, a.rssi)
        .uint(3, a.tipoIndirizzo).bytes(4, a.dati).toByteArray()

    fun lotto(annunci: List<Annuncio>): Messaggio {
        val w = ProtoWriter()
        annunci.forEach { w.bytes(1, annuncio(it)) }
        return Messaggio(MsgBle.RAW_ADV, w.toByteArray())
    }

    fun connessione(ind: Long, connesso: Boolean, mtu: Int, errore: Int) = Messaggio(MsgBle.CONNECTION_RESP,
        ProtoWriter().varint(1, ind).bool(2, connesso).uint(3, mtu).varint(4, errore.toLong()).toByteArray())

    fun liberi(s: Slot) = Messaggio(MsgBle.FREE_RESP, ProtoWriter().uint(1, s.liberi).uint(2, s.limite)
        .also { w -> s.occupati.forEach { w.varint(3, it) } }.toByteArray())

    /** Messaggi con solo indirizzo e handle (WriteResponse, NotifyResponse). */
    fun handle(tipo: Int, ind: Long, h: Int) = Messaggio(tipo, ProtoWriter().varint(1, ind).uint(2, h).toByteArray())
    fun dati(tipo: Int, ind: Long, h: Int, d: ByteArray?) =
        Messaggio(tipo, ProtoWriter().varint(1, ind).uint(2, h).bytes(3, d).toByteArray())
    fun errore(ind: Long, h: Int, e: Int) =
        Messaggio(MsgBle.GATT_ERROR, ProtoWriter().varint(1, ind).uint(2, h).varint(3, e.toLong()).toByteArray())
    /** Risposte a pairing, unpairing e clear cache: indirizzo, esito, errore. */
    fun esito(tipo: Int, ind: Long, ok: Boolean, e: Int) =
        Messaggio(tipo, ProtoWriter().varint(1, ind).bool(2, ok).varint(3, e.toLong()).toByteArray())
    fun statoScanner(stato: Int, modo: Int) = Messaggio(MsgBle.SCANNER_STATE,
        ProtoWriter().uint(1, stato).uint(2, modo).uint(3, modo).toByteArray())

    /** UUID come due uint64 [alti, bassi]: è il campo `uuid` che leggono anche i client prima della 1.12. */
    private fun uuid(w: ProtoWriter, u: UUID) = w.varint(1, u.mostSignificantBits).varint(1, u.leastSignificantBits)

    fun servizi(ind: Long, servizi: List<ServizioGatt>): Messaggio {
        val w = ProtoWriter().varint(1, ind)
        for (s in servizi) {
            val ws = ProtoWriter().also { uuid(it, s.uuid) }.uint(2, s.handle)
            for (c in s.caratteristiche) {
                val wc = ProtoWriter().also { uuid(it, c.uuid) }.uint(2, c.handle).uint(3, c.proprieta)
                for (d in c.descrittori) wc.bytes(4, ProtoWriter().also { uuid(it, d.uuid) }.uint(2, d.handle).toByteArray())
                ws.bytes(3, wc.toByteArray())
            }
            w.bytes(2, ws.toByteArray())
        }
        return Messaggio(MsgBle.GET_SERVICES_RESP, w.toByteArray())
    }
}

class DescrittoreGatt(val uuid: UUID, val handle: Int)
class CaratteristicaGatt(val uuid: UUID, val handle: Int, val proprieta: Int, val descrittori: List<DescrittoreGatt>)
class ServizioGatt(val uuid: UUID, val handle: Int, val caratteristiche: List<CaratteristicaGatt>)

/**
 * Raccoglie gli annunci e li spedisce a lotti, come ESPHome: un messaggio ogni
 * `dimensione` annunci o allo scadere del timer del chiamante (`svuota`).
 */
class LottoAnnunci(private val dimensione: Int = 16) {
    private val coda = ArrayList<Annuncio>(dimensione)

    /** Aggiunge un annuncio; restituisce il lotto se è pieno. */
    @Synchronized fun aggiungi(a: Annuncio): Messaggio? {
        coda += a
        return if (coda.size >= dimensione) svuota() else null
    }

    @Synchronized fun svuota(): Messaggio? {
        if (coda.isEmpty()) return null
        return CodecBle.lotto(coda.toList()).also { coda.clear() }
    }
}

/** Slot delle connessioni GATT attive: limite prudente, Android ne regge poche insieme. */
class Slot(val limite: Int = 3) {
    private val usati = LinkedHashSet<Long>()
    val occupati: List<Long> @Synchronized get() = usati.toList()
    val liberi: Int @Synchronized get() = limite - usati.size

    /** `true` se l'indirizzo ha (o aveva già) uno slot. */
    @Synchronized fun occupa(ind: Long): Boolean = ind in usati || (usati.size < limite && usati.add(ind))
    @Synchronized fun libera(ind: Long): Boolean = usati.remove(ind)
}

/**
 * Handle GATT numerati in ordine di scoperta (servizio, caratteristiche, descrittori),
 * come gli attributi ATT: Android non espone gli handle dei descrittori.
 */
class TabellaHandle<T : Any> {
    private val oggetti = HashMap<Int, T>()
    private var prossimo = 1

    fun registra(o: T): Int = prossimo++.also { oggetti[it] = o }
    fun cerca(h: Int): T? = oggetti[h]
}

/**
 * Android ammette una sola operazione GATT alla volta per dispositivo: le richieste
 * di HA si accodano e partono quando la precedente ha avuto la sua callback.
 * `avvia` restituisce `false` se l'operazione non è partita (si passa alla successiva).
 */
class CodaGatt<O : Any>(private val avvia: (O) -> Boolean) {
    private val coda = ArrayDeque<O>()
    var corrente: O? = null
        private set

    @Synchronized fun accoda(o: O) { coda.addLast(o); if (corrente == null) prossima() }

    /** Chiude l'operazione in corso e restituisce quella appena finita. */
    @Synchronized fun finita(): O? = corrente.also { corrente = null; prossima() }

    @Synchronized fun svuota(): List<O> = (listOfNotNull(corrente) + coda).also { coda.clear(); corrente = null }

    private fun prossima() {
        while (corrente == null) {
            val o = coda.removeFirstOrNull() ?: return
            corrente = o
            if (!avvia(o)) corrente = null
        }
    }
}
