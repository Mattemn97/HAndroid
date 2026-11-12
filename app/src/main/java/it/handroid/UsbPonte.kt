package it.handroid

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Ponte USB seriale → TCP nel processo ":ha" (spec 0003). Ogni chiavetta autorizzata
 * diventa `socket://127.0.0.1:6638` (Zigbee) o `:6639` (Z-Wave), come chiede D7.
 * Opzioni in `config/handroid_usb.json`, stato per l'utente in `config/handroid_usb_stato.json`.
 */
object UsbPonte {
    private const val TAG = "HAndroid.usb"
    private const val AZIONE_PERMESSO = "it.handroid.USB_PERMESSO"
    const val PORTA_ZIGBEE = 6638
    const val PORTA_ZWAVE = 6639
    // Vendor delle chiavette Z-Wave note (Sigma Designs / Silicon Labs Z-Wave CDC).
    private val VID_ZWAVE = setOf(0x0658)

    private val ponti = mutableMapOf<String, Ponte>()
    private val chiesti = mutableSetOf<String>()
    private val errori = mutableMapOf<String, String>()
    private var avviato = false

    @Synchronized
    fun avvia(ctx: Context) {
        if (avviato) return
        avviato = true
        val app = ctx.applicationContext
        val filtro = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(AZIONE_PERMESSO)
        }
        val ricevitore = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val d = dispositivo(i)
                if (i.action == UsbManager.ACTION_USB_DEVICE_DETACHED && d != null) stacca(app, d.deviceName)
                if (i.action == AZIONE_PERMESSO && d != null) synchronized(this@UsbPonte) { chiesti -= d.deviceName }
                ricontrolla(app)
            }
        }
        // Il permesso arriva da un PendingIntent inviato dal sistema: serve un ricevitore esportato.
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(ricevitore, filtro, Context.RECEIVER_EXPORTED)
        else app.registerReceiver(ricevitore, filtro)
        ricontrolla(app)
    }

    /** Apre un ponte per ogni chiavetta compatibile; chiede il permesso a quelle nuove. */
    @Synchronized
    fun ricontrolla(ctx: Context) {
        val usb = ctx.getSystemService(UsbManager::class.java) ?: return
        val opzioni = opzioni(ctx)
        for (d in usb.deviceList.values) {
            if (d.deviceName in ponti || !seriale(d)) continue
            if (!usb.hasPermission(d)) {
                if (chiesti.add(d.deviceName)) chiediPermesso(ctx, usb, d)
                continue
            }
            val porta = if (d.vendorId in VID_ZWAVE) PORTA_ZWAVE else PORTA_ZIGBEE
            if (ponti.values.any { it.porta == porta }) {
                errori[d.deviceName] = "porta $porta già usata da un'altra chiavetta"
                continue
            }
            runCatching {
                val conn = usb.openDevice(d) ?: error("openDevice non riuscito")
                val s = UsbSeriale.apri(d, conn, opzioni.optInt("baud", 115_200), opzioni.optBoolean("rtscts", false))
                val p = runCatching { Ponte(s, porta) }.onFailure { s.chiudi() }.getOrThrow()
                ponti[d.deviceName] = p.also { it.avvia() }
                errori -= d.deviceName
                Log.i(TAG, "${nome(d)} su socket://127.0.0.1:$porta")
            }.onFailure {
                errori[d.deviceName] = it.message ?: it.toString()
                Log.e(TAG, "Ponte per ${nome(d)} non avviato", it)
            }
        }
        scriviStato(ctx, usb)
    }

    @Synchronized
    private fun stacca(ctx: Context, nome: String) {
        ponti.remove(nome)?.ferma()
        errori -= nome
        chiesti -= nome
        Log.i(TAG, "Chiavetta $nome scollegata")
    }

    private fun chiediPermesso(ctx: Context, usb: UsbManager, d: UsbDevice) {
        // FLAG_MUTABLE: il sistema aggiunge all'intent il dispositivo e l'esito.
        val pi = PendingIntent.getBroadcast(
            ctx, d.deviceId, Intent(AZIONE_PERMESSO).setPackage(ctx.packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        usb.requestPermission(d, pi)
    }

    // Solo le chiavette del filtro o comunque seriali (CDC-ACM): niente tastiere o dischi.
    private fun seriale(d: UsbDevice) = d.vendorId == 0x10C4 ||
        (0 until d.interfaceCount).any { d.getInterface(it).interfaceClass in setOf(2, 10) }

    private fun opzioni(ctx: Context) = runCatching {
        JSONObject(File(cartella(ctx), "handroid_usb.json").readText())
    }.getOrDefault(JSONObject())

    private fun scriviStato(ctx: Context, usb: UsbManager) {
        val lista = JSONArray()
        for (d in usb.deviceList.values.filter { seriale(it) }) {
            val p = ponti[d.deviceName]
            lista.put(JSONObject().apply {
                put("nome", nome(d))
                put("vid", "%04x".format(d.vendorId))
                put("pid", "%04x".format(d.productId))
                put("driver", UsbSeriale.driver(d))
                when {
                    p != null -> { put("stato", "attivo"); put("url", "socket://127.0.0.1:${p.porta}") }
                    d.deviceName in errori -> { put("stato", "errore"); put("errore", errori[d.deviceName]) }
                    else -> put("stato", "in attesa del permesso")
                }
            })
        }
        runCatching {
            File(cartella(ctx), "handroid_usb_stato.json").writeText(JSONObject().put("chiavette", lista).toString(2))
        }.onFailure { Log.w(TAG, "Stato USB non scritto", it) }
    }

    private fun cartella(ctx: Context) = File(PythonRuntime.base(ctx), "config").apply { mkdirs() }

    private fun nome(d: UsbDevice) = d.productName ?: "%04x:%04x".format(d.vendorId, d.productId)

    @Suppress("DEPRECATION")
    private fun dispositivo(i: Intent): UsbDevice? =
        if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        else i.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    /** Una chiavetta, un server TCP su 127.0.0.1, un client per volta: quello nuovo sostituisce il vecchio. */
    private class Ponte(val seriale: UsbSeriale, val porta: Int) {
        private val server = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(InetAddress.getLoopbackAddress(), porta), 1)
        }
        @Volatile private var client: Socket? = null
        @Volatile private var attivo = true

        fun avvia() {
            Thread({ accetta() }, "usb-accetta-$porta").start()
            Thread({ daUsb() }, "usb-leggi-$porta").start()
        }

        fun ferma() {
            attivo = false
            runCatching { server.close() }
            runCatching { client?.close() }
            seriale.chiudi()
        }

        private fun accetta() {
            while (attivo) {
                val s = runCatching { server.accept() }.getOrNull() ?: break
                s.tcpNoDelay = true
                runCatching { client?.close() }
                client = s
                Log.i(TAG, "Client collegato alla porta $porta")
                Thread({ versoUsb(s) }, "usb-scrivi-$porta").start()
            }
        }

        // USB → TCP. Senza client i byte si scartano: ZHA riparte comunque con un reset.
        private fun daUsb() {
            val buf = ByteArray(4096)
            while (attivo) {
                val n = runCatching { seriale.leggi(buf) }.getOrDefault(-1)
                if (n < 0) break
                if (n == 0) continue
                val c = client ?: continue
                runCatching { c.getOutputStream().write(buf, 0, n) }.onFailure { runCatching { c.close() } }
            }
        }

        // TCP → USB, finché il client resta collegato.
        private fun versoUsb(s: Socket) {
            val buf = ByteArray(4096)
            runCatching {
                val ins = s.getInputStream()
                while (attivo) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    seriale.scrivi(buf, n)
                }
            }
            runCatching { s.close() }
            if (client === s) client = null
        }
    }
}
