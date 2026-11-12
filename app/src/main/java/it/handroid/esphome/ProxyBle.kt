package it.handroid.esphome

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelUuid
import android.os.PowerManager
import android.util.Log
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Proxy Bluetooth ESPHome (spec 0005): inoltra a HA gli annunci BLE visti dal telefono
 * e gli fa usare connessioni GATT attive. Tutto gira su un solo thread ("esphome-ble"):
 * i messaggi di HA e le callback di Android vi vengono postati, così lo stato non ha lock.
 */
@SuppressLint("MissingPermission")
class ProxyBle private constructor(private val ctx: Context, private val adapter: BluetoothAdapter, private val mac: String) : Modulo {
    private val h = Handler(HandlerThread("esphome-ble").apply { start() }.looper)
    private val iscrittiAnnunci = CopyOnWriteArraySet<Connessione>()
    private val iscrittiSlot = CopyOnWriteArraySet<Connessione>()
    private val lotto = LottoAnnunci()
    private val slot = Slot(LIMITE_CONNESSIONI)
    private val dispositivi = HashMap<Long, Dispositivo>()
    private var scansione = false
    private var statoScanner = MsgBle.STATO_STOPPED

    override fun deviceInfo(w: ProtoWriter) { w.uint(15, MsgBle.FEATURES).string(18, mac) }

    override fun gestisci(conn: Connessione, tipo: Int, payload: ByteArray): Boolean {
        if (tipo !in MsgBle.SUBSCRIBE_ADV..MsgBle.CLEAR_CACHE_RESP && tipo != MsgBle.SCANNER_SET_MODE) return false
        h.post { runCatching { esegui(conn, tipo, ProtoReader(payload).campi()) }.onFailure { Log.w(TAG, "BLE $tipo: ${it.message}") } }
        return true
    }

    override fun chiusa(conn: Connessione) {
        h.post {
            iscrittiAnnunci.remove(conn); iscrittiSlot.remove(conn)
            dispositivi.values.filter { it.conn === conn }.forEach { it.gatt?.disconnect(); chiudi(it, 0, avvisa = false) }
            aggiornaScansione()
        }
    }

    private fun esegui(conn: Connessione, tipo: Int, c: Map<Int, ProtoReader.Campo>) {
        val ind = c[1]?.num ?: 0L
        val handle = c[2]?.int ?: 0
        when (tipo) {
            MsgBle.SUBSCRIBE_ADV -> { iscrittiAnnunci += conn; aggiornaScansione(); conn.invia(CodecBle.statoScanner(statoScanner, MsgBle.MODO_ATTIVO)) }
            MsgBle.UNSUBSCRIBE_ADV -> { iscrittiAnnunci -= conn; aggiornaScansione() }
            MsgBle.SUBSCRIBE_FREE -> { iscrittiSlot += conn; conn.invia(CodecBle.liberi(slot)) }
            // Android scansiona sempre in modo attivo: la richiesta si accetta e si riconferma lo stato.
            MsgBle.SCANNER_SET_MODE -> conn.invia(CodecBle.statoScanner(statoScanner, MsgBle.MODO_ATTIVO))
            MsgBle.DEVICE_REQ -> richiestaDispositivo(conn, ind, c[2]?.int ?: 0, c[4]?.int ?: 0)
            MsgBle.GET_SERVICES_REQ -> {
                val d = dispositivi[ind]?.takeIf { it.pronto } ?: return conn.invia(CodecBle.errore(ind, 0, NON_CONNESSO))
                d.servizi.forEach { conn.invia(CodecBle.servizi(ind, listOf(it))) }
                conn.invia(MsgBle.GET_SERVICES_DONE, ProtoWriter().varint(1, ind).toByteArray())
            }
            MsgBle.READ_REQ, MsgBle.READ_DESC_REQ, MsgBle.WRITE_REQ, MsgBle.WRITE_DESC_REQ -> {
                val d = dispositivi[ind]?.takeIf { it.pronto } ?: return conn.invia(CodecBle.errore(ind, handle, NON_CONNESSO))
                d.coda.accoda(when (tipo) {
                    MsgBle.READ_REQ, MsgBle.READ_DESC_REQ -> Op(tipo, handle, null, false)
                    MsgBle.WRITE_REQ -> Op(tipo, handle, c[4]?.dati ?: ByteArray(0), c[3]?.bool ?: false)
                    else -> Op(tipo, handle, c[3]?.dati ?: ByteArray(0), true)
                })
            }
            MsgBle.NOTIFY_REQ -> {
                val d = dispositivi[ind]?.takeIf { it.pronto } ?: return conn.invia(CodecBle.errore(ind, handle, NON_CONNESSO))
                val ch = d.tabella.cerca(handle) as? BluetoothGattCharacteristic
                // Con REMOTE_CACHING il descrittore CCCD lo scrive HA: qui si attiva solo la consegna locale.
                if (ch != null && d.gatt?.setCharacteristicNotification(ch, c[3]?.bool ?: false) == true)
                    conn.invia(CodecBle.handle(MsgBle.NOTIFY_RESP, ind, handle))
                else conn.invia(CodecBle.errore(ind, handle, GATT_ERRORE))
            }
        }
    }

    // --- Scansione ---

    private val callbackScan = object : ScanCallback() {
        override fun onScanResult(tipo: Int, r: ScanResult) { lotto.aggiungi(annuncio(r))?.let(::aTutti) }
        override fun onBatchScanResults(l: MutableList<ScanResult>) = l.forEach { onScanResult(0, it) }
        override fun onScanFailed(e: Int) { Log.w(TAG, "Scansione BLE fallita: $e"); scansione = false; statoCambiato(MsgBle.STATO_FAILED) }
    }

    private fun annuncio(r: ScanResult): Annuncio {
        val tipo = if (Build.VERSION.SDK_INT >= 35) r.device.addressType else 0
        return Annuncio(CodecBle.indirizzo(r.device.address), r.rssi, tipo, CodecBle.taglia(r.scanRecord?.bytes))
    }

    private val svuota = object : Runnable {
        override fun run() { lotto.svuota()?.let(::aTutti); if (scansione) h.postDelayed(this, 100) }
    }

    private fun aggiornaScansione() {
        val serve = iscrittiAnnunci.isNotEmpty()
        if (serve == scansione) return
        if (!serve) { ferma(); statoCambiato(MsgBle.STATO_STOPPED); return }
        val scanner = adapter.bluetoothLeScanner ?: return statoCambiato(MsgBle.STATO_FAILED)
        val acceso = ctx.getSystemService(PowerManager::class.java).isInteractive
        val impostazioni = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { scanner.startScan(if (acceso) null else filtriSchermoSpento(), impostazioni, callbackScan) }
            .onSuccess { scansione = true; h.postDelayed(svuota, 100); statoCambiato(MsgBle.STATO_RUNNING)
                Log.i(TAG, "Scansione BLE avviata (schermo ${if (acceso) "acceso" else "spento, con filtri"})") }
            .onFailure { Log.w(TAG, "Scansione BLE non avviata: ${it.message}"); statoCambiato(MsgBle.STATO_FAILED) }
    }

    private fun ferma() {
        scansione = false
        h.removeCallbacks(svuota)
        runCatching { adapter.bluetoothLeScanner?.stopScan(callbackScan) }
    }

    private fun statoCambiato(s: Int) {
        if (s == statoScanner) return
        statoScanner = s
        iscrittiAnnunci.forEach { runCatching { it.invia(CodecBle.statoScanner(s, MsgBle.MODO_ATTIVO)) } }
    }

    private fun aTutti(m: Messaggio) = iscrittiAnnunci.forEach { c -> runCatching { c.invia(m) }.onFailure { iscrittiAnnunci -= c } }

    // --- Connessioni GATT ---

    private class Op(val tipo: Int, val handle: Int, val dati: ByteArray?, val risposta: Boolean)

    private inner class Dispositivo(val ind: Long, val conn: Connessione) {
        var gatt: BluetoothGatt? = null
        var mtu = 23
        var pronto = false
        var servizi: List<ServizioGatt> = emptyList()
        val tabella = TabellaHandle<Any>()
        val coda = CodaGatt<Op> { avvia(this, it) }
    }

    private fun richiestaDispositivo(conn: Connessione, ind: Long, tipo: Int, tipoIndirizzo: Int) {
        when (tipo) {
            MsgBle.REQ_CONNECT_CACHE, MsgBle.REQ_CONNECT_NO_CACHE -> connetti(conn, ind, tipoIndirizzo)
            MsgBle.REQ_DISCONNECT -> dispositivi[ind]?.let { it.gatt?.disconnect(); chiudi(it, 0) }
                ?: conn.invia(CodecBle.connessione(ind, false, 0, 0))
            MsgBle.REQ_PAIR -> conn.invia(MsgBle.PAIRING_RESP, ProtoWriter().varint(1, ind).varint(3, NON_SUPPORTATO.toLong()).toByteArray())
            MsgBle.REQ_UNPAIR -> conn.invia(CodecBle.esito(MsgBle.UNPAIRING_RESP, ind, false, NON_SUPPORTATO))
            MsgBle.REQ_CLEAR_CACHE -> conn.invia(CodecBle.esito(MsgBle.CLEAR_CACHE_RESP, ind, false, NON_SUPPORTATO))
        }
    }

    private fun connetti(conn: Connessione, ind: Long, tipoIndirizzo: Int) {
        dispositivi[ind]?.let { d -> if (d.pronto) conn.invia(CodecBle.connessione(ind, true, d.mtu, 0)); return }
        if (!slot.occupa(ind)) return conn.invia(CodecBle.connessione(ind, false, 0, NESSUNO_SLOT))
        val d = Dispositivo(ind, conn)
        dispositivi[ind] = d
        aggiornaSlot()
        val mac = CodecBle.mac(ind)
        val dev: BluetoothDevice = if (Build.VERSION.SDK_INT >= 33)
            adapter.getRemoteLeDevice(mac, if (tipoIndirizzo == 1) BluetoothDevice.ADDRESS_TYPE_RANDOM else BluetoothDevice.ADDRESS_TYPE_PUBLIC)
        else adapter.getRemoteDevice(mac)
        d.gatt = dev.connectGatt(ctx, false, callbackGatt(d), BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M_MASK, h)
        if (d.gatt == null) return chiudi(d, GATT_ERRORE)
        // HA ha i suoi timeout, ma uno slot non deve restare preso da un dispositivo che non risponde.
        h.postDelayed({ if (dispositivi[ind] === d && !d.pronto) { d.gatt?.disconnect(); chiudi(d, TIMEOUT) } }, 30_000)
    }

    /** Libera tutto e avvisa HA; dopo `close()` Android non manda più callback. */
    private fun chiudi(d: Dispositivo, errore: Int, avvisa: Boolean = true) {
        if (dispositivi[d.ind] !== d) return
        dispositivi.remove(d.ind)
        runCatching { d.gatt?.close() }
        d.coda.svuota()
        slot.libera(d.ind)
        if (avvisa) runCatching { d.conn.invia(CodecBle.connessione(d.ind, false, 0, errore)) }
        aggiornaSlot()
    }

    private fun aggiornaSlot() = iscrittiSlot.forEach { c -> runCatching { c.invia(CodecBle.liberi(slot)) } }

    /** Avvia un'operazione accodata; se Android la rifiuta subito, HA riceve l'errore. */
    @Suppress("DEPRECATION")
    private fun avvia(d: Dispositivo, op: Op): Boolean {
        val g = d.gatt
        val o = d.tabella.cerca(op.handle)
        val partita = g != null && when (op.tipo) {
            MsgBle.READ_REQ -> (o as? BluetoothGattCharacteristic)?.let { g.readCharacteristic(it) } ?: false
            MsgBle.READ_DESC_REQ -> (o as? BluetoothGattDescriptor)?.let { g.readDescriptor(it) } ?: false
            MsgBle.WRITE_REQ -> (o as? BluetoothGattCharacteristic)?.let { ch ->
                val tipo = if (op.risposta) BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT else BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                if (Build.VERSION.SDK_INT >= 33) g.writeCharacteristic(ch, op.dati!!, tipo) == 0
                else { ch.writeType = tipo; ch.value = op.dati; g.writeCharacteristic(ch) }
            } ?: false
            else -> (o as? BluetoothGattDescriptor)?.let { ds ->
                if (Build.VERSION.SDK_INT >= 33) g.writeDescriptor(ds, op.dati!!) == 0
                else { ds.value = op.dati; g.writeDescriptor(ds) }
            } ?: false
        }
        if (!partita) d.conn.invia(CodecBle.errore(d.ind, op.handle, GATT_ERRORE))
        return partita
    }

    /** Chiude l'operazione in corso e risponde a HA con i dati o con l'errore. */
    private fun risposta(d: Dispositivo, status: Int, dati: ByteArray?) {
        val op = d.coda.finita() ?: return
        val m = when {
            status != BluetoothGatt.GATT_SUCCESS -> CodecBle.errore(d.ind, op.handle, status)
            op.tipo == MsgBle.READ_REQ || op.tipo == MsgBle.READ_DESC_REQ -> CodecBle.dati(MsgBle.READ_RESP, d.ind, op.handle, dati)
            op.risposta -> CodecBle.handle(MsgBle.WRITE_RESP, d.ind, op.handle)
            else -> null
        }
        m?.let { runCatching { d.conn.invia(it) } }
    }

    private fun callbackGatt(d: Dispositivo) = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, stato: Int) {
            if (stato == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                if (!g.requestMtu(517)) g.discoverServices()
            } else chiudi(d, if (status == 0) 0 else status)
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) d.mtu = mtu
            if (!d.pronto && d.servizi.isEmpty()) g.discoverServices()
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (d.pronto) return
            if (status != BluetoothGatt.GATT_SUCCESS) { g.disconnect(); return chiudi(d, status) }
            d.servizi = g.services.map { s ->
                ServizioGatt(s.uuid, d.tabella.registra(s), s.characteristics.map { c ->
                    CaratteristicaGatt(c.uuid, d.tabella.registra(c), c.properties,
                        c.descriptors.map { DescrittoreGatt(it.uuid, d.tabella.registra(it)) })
                })
            }
            d.pronto = true
            runCatching { d.conn.invia(CodecBle.connessione(d.ind, true, d.mtu, 0)) }
        }

        // Android 13+ chiama le varianti con il valore; le vecchie servono fino ad Android 12.
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, v: ByteArray, status: Int) = risposta(d, status, v)
        @Deprecated("Android < 13") override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) =
            @Suppress("DEPRECATION") risposta(d, status, c.value)
        override fun onDescriptorRead(g: BluetoothGatt, ds: BluetoothGattDescriptor, status: Int, v: ByteArray) = risposta(d, status, v)
        @Deprecated("Android < 13") override fun onDescriptorRead(g: BluetoothGatt, ds: BluetoothGattDescriptor, status: Int) =
            @Suppress("DEPRECATION") risposta(d, status, ds.value)
        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) = risposta(d, status, null)
        override fun onDescriptorWrite(g: BluetoothGatt, ds: BluetoothGattDescriptor, status: Int) = risposta(d, status, null)

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, v: ByteArray) = notifica(c, v)
        @Deprecated("Android < 13") override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) =
            @Suppress("DEPRECATION") notifica(c, c.value)

        private fun notifica(c: BluetoothGattCharacteristic, v: ByteArray?) {
            val handle = d.servizi.firstNotNullOfOrNull { s -> s.caratteristiche.firstOrNull { d.tabella.cerca(it.handle) === c } }?.handle ?: return
            runCatching { d.conn.invia(CodecBle.dati(MsgBle.NOTIFY_DATA, d.ind, handle, v)) }
        }
    }

    // In fondo: le callback del ricevitore usano le proprietà dichiarate sopra.
    init {
        // Schermo e adattatore cambiano i filtri o permettono di (ri)partire: si riapplica lo stato voluto.
        ctx.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.action == BluetoothAdapter.ACTION_STATE_CHANGED &&
                    i.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) != BluetoothAdapter.STATE_ON) { scansione = false; return }
                if (scansione) ferma()
                aggiornaScansione()
            }
        }, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }, null, h)
    }

    companion object {
        private const val TAG = ApiServer.TAG
        // Android regge 4-7 connessioni GATT in tutto, e il telefono può averne già altre.
        const val LIMITE_CONNESSIONI = 3
        // Codici di errore Bluetooth/ESP-IDF che HA sa tradurre.
        private const val GATT_ERRORE = 0x85
        private const val NESSUNO_SLOT = 0x80
        private const val TIMEOUT = 0x08
        private const val NON_CONNESSO = 0x85
        private const val NON_SUPPORTATO = 0x06

        /**
         * A schermo spento Android ferma le scansioni senza filtri. Si passa a filtri "larghi"
         * sui formati dei sensori più diffusi (spec 0005, B4): service data o manufacturer data
         * con dati vuoti, che accettano qualsiasi contenuto.
         */
        private fun filtriSchermoSpento(): List<ScanFilter> {
            fun uuid16(u: Int) = ParcelUuid(UUID.fromString("%08x-0000-1000-8000-00805f9b34fb".format(u)))
            val servizi = listOf(0xFE95, 0x181A, 0xFCD2, 0xFDCD, 0xFE9F, 0xFEAA).map {
                ScanFilter.Builder().setServiceData(uuid16(it), ByteArray(0)).build()
            }
            val produttori = listOf(0x0499, 0xEC88, 0x0969).map {
                ScanFilter.Builder().setManufacturerData(it, ByteArray(0)).build()
            }
            return servizi + produttori
        }

        /** Il proxy, o `null` se mancano Bluetooth o permessi (il device owner li concede, spec 0001). */
        fun crea(ctx: Context, mac: String): ProxyBle? {
            val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
            val permessi = if (Build.VERSION.SDK_INT >= 31) listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                else listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (permessi.any { ctx.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
                Log.w(TAG, "Proxy Bluetooth disattivo: permessi Bluetooth mancanti"); return null
            }
            return ProxyBle(ctx, adapter, mac)
        }
    }
}
