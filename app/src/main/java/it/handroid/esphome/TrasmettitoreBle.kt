package it.handroid.esphome

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Trasmettitore per ble_adv (spec 0008): HA chiede `adv_svc_v1` e il telefono trasmette
 * l'annuncio, così si comandano lampade e ventilatori senza un ESP32. Gli annunci ricevuti
 * (dal proxy Bluetooth) tornano a HA come evento, per abbinare i telecomandi.
 * Un annuncio alla volta, in coda, sul thread "esphome-adv".
 */
@SuppressLint("MissingPermission")
class TrasmettitoreBle private constructor(private val advertiser: BluetoothLeAdvertiser) : Modulo {
    private val h = Handler(HandlerThread("esphome-adv").apply { start() }.looper)
    private val pronti = CopyOnWriteArraySet<Connessione>()
    private val filtro = BleAdv.Filtro()
    private val coda = ArrayDeque<Pair<AdvertiseData, Long>>()
    private var attivo: AdvertisingSetCallback? = null

    override fun descrizioni() = listOf(BleAdv.SETUP.descrizione(), BleAdv.ADV.descrizione())

    override fun gestisci(conn: Connessione, tipo: Int, payload: ByteArray): Boolean {
        if (tipo != Msg.EXECUTE_SERVICE) return false
        val (key, a) = BleAdv.esegui(payload)
        when (key) {
            BleAdv.SETUP.key -> {
                filtro.durata = (a.getOrNull(0)?.float ?: 20_000f).toLong()
                filtro.cids = a.getOrNull(1)?.floats.orEmpty().map { it.toInt() }.toSet()
                filtro.macs = a.getOrNull(2)?.strings.orEmpty().map { it.uppercase() }.toSet()
                pronti += conn
            }
            BleAdv.ADV.key -> {
                val raw = BleAdv.daHex(a.getOrNull(0)?.string.orEmpty())
                val durata = a.getOrNull(1)?.float ?: 100f
                val ripeti = (a.getOrNull(2)?.float ?: 3f).coerceAtLeast(1f)
                a.getOrNull(3)?.strings?.forEach { filtro.ignora(it, (a.getOrNull(4)?.float ?: 0f).toLong()) }
                val c = BleAdv.scomponi(raw) ?: return true.also { Log.w(TAG, "Annuncio non trasmissibile da Android: ${BleAdv.hex(raw)}") }
                // Android trasmette ogni 100 ms al massimo: la finestra va allungata, o la lampada
                // vede una sola copia. ESPHome trasmette `repeat` volte per `duration` ms.
                val finestra = (durata * ripeti).toLong().coerceIn(FINESTRA_MIN, FINESTRA_MAX)
                h.post { coda.addLast(dati(c) to finestra); prossimo() }
            }
            else -> return false
        }
        return true
    }

    override fun chiusa(conn: Connessione) { pronti -= conn }

    /** Annuncio visto dal proxy Bluetooth: a HA solo dopo `setup_svc_v0`, come fa ble_adv_proxy. */
    fun ricevuto(mac: String, raw: ByteArray) {
        if (pronti.isEmpty() || !filtro.passa(mac, raw)) return
        val m = BleAdv.evento(mac, raw)
        pronti.forEach { c -> if (c.serviziHa) runCatching { c.invia(m) } }
    }

    private fun dati(c: BleAdv.Contenuto) = AdvertiseData.Builder().apply {
        c.uuid16.forEach { addServiceUuid(uuid16(it)) }
        c.serviceData.forEach { (u, d) -> addServiceData(uuid16(u), d) }
        c.produttore.forEach { (id, d) -> addManufacturerData(id, d) }
    }.build()

    private fun prossimo() {
        if (attivo != null) return
        val (d, ms) = coda.removeFirstOrNull() ?: return
        val cb = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(s: AdvertisingSet?, tx: Int, stato: Int) {
                if (stato != ADVERTISE_SUCCESS) { Log.w(TAG, "Trasmissione BLE fallita: $stato"); h.post { attivo = null; prossimo() } }
            }
        }
        attivo = cb
        runCatching { advertiser.startAdvertisingSet(PARAMETRI, d, null, null, null, cb) }
            .onFailure { Log.w(TAG, "Trasmissione BLE: ${it.message}"); attivo = null; return prossimo() }
        h.postDelayed({
            runCatching { advertiser.stopAdvertisingSet(cb) }
            if (attivo === cb) attivo = null
            prossimo()
        }, ms)
    }

    companion object {
        private const val TAG = ApiServer.TAG
        private const val FINESTRA_MIN = 300L
        private const val FINESTRA_MAX = 2_000L
        private val PARAMETRI = AdvertisingSetParameters.Builder().setLegacyMode(true).setConnectable(false)
            .setScannable(false).setInterval(AdvertisingSetParameters.INTERVAL_MIN)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH).build()

        private fun uuid16(u: Int) = ParcelUuid(UUID.fromString("0000%04x-0000-1000-8000-00805f9b34fb".format(u)))

        /** `null` senza adattatore, senza annunci BLE o senza il permesso di trasmettere. */
        fun crea(ctx: Context, registro: Registro): TrasmettitoreBle? {
            val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
            if (Build.VERSION.SDK_INT >= 31 && ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Trasmettitore BLE spento: manca il permesso BLUETOOTH_ADVERTISE"); return null
            }
            val adv = adapter.bluetoothLeAdvertiser ?: return null.also { Log.i(TAG, "Trasmettitore BLE spento: Bluetooth spento o senza annunci") }
            // ble_adv riconosce il proxy da questo sensore; il valore è il nome dell'adattatore.
            registro.aggiungi(SensoreTesto(BleAdv.SENSORE, BleAdv.SENSORE, "mdi:bluetooth-transfer", diagnostica = true, valore = "handroid"))
            return TrasmettitoreBle(adv)
        }
    }
}
