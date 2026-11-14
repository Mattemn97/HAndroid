package it.handroid

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import java.io.IOException

/**
 * Driver seriali minimi, solo quelli delle chiavette Zigbee/Z-Wave diffuse:
 * CP210x (ZBDongle-P, ZBT-1) e CDC-ACM (ZBDongle-E con CH9102, ConBee II, Z-Stick 7).
 * Sempre 8N1; il baud conta solo per i chip con una UART vera (CP210x, CH9102).
 */
class UsbSeriale private constructor(
    private val conn: UsbDeviceConnection,
    private val interfacce: List<UsbInterface>,
    private val ingresso: UsbEndpoint,
    private val uscita: UsbEndpoint,
) {
    @Volatile private var chiusa = false

    /** Legge fino a `buf.size` byte; 0 se in [TIMEOUT_MS] non arriva niente. */
    fun leggi(buf: ByteArray): Int {
        if (chiusa) throw IOException("seriale chiusa")
        return conn.bulkTransfer(ingresso, buf, buf.size, TIMEOUT_MS).coerceAtLeast(0)
    }

    fun scrivi(dati: ByteArray, n: Int) {
        var fatto = 0
        while (fatto < n) {
            if (chiusa) throw IOException("seriale chiusa")
            val k = conn.bulkTransfer(uscita, dati, fatto, minOf(n - fatto, BLOCCO), 2_000)
            if (k < 0) throw IOException("scrittura USB fallita")
            fatto += k
        }
    }

    fun chiudi() {
        if (chiusa) return
        chiusa = true
        interfacce.forEach { conn.releaseInterface(it) }
        conn.close()
    }

    companion object {
        private const val TIMEOUT_MS = 200
        private const val BLOCCO = 4096
        private const val VID_SILABS = 0x10C4

        fun driver(d: UsbDevice) = if (d.vendorId == VID_SILABS) "cp210x" else "cdc-acm"

        /** Apre la chiavetta già autorizzata. [rtscts] attiva il controllo di flusso hardware (solo CP210x). */
        fun apri(d: UsbDevice, conn: UsbDeviceConnection, baud: Int, rtscts: Boolean): UsbSeriale =
            runCatching { if (driver(d) == "cp210x") cp210x(d, conn, baud, rtscts) else cdcAcm(d, conn, baud) }
                .onFailure { conn.close() }
                .getOrThrow()

        private fun cp210x(d: UsbDevice, conn: UsbDeviceConnection, baud: Int, rtscts: Boolean): UsbSeriale {
            val i = d.getInterface(0)
            reclama(conn, i)
            fun ctrl(req: Int, valore: Int, dati: ByteArray? = null) {
                if (conn.controlTransfer(0x41, req, valore, i.id, dati, dati?.size ?: 0, 1_000) < 0)
                    throw IOException("CP210x: comando 0x${req.toString(16)} rifiutato")
            }
            ctrl(0x00, 0x0001) // IFC_ENABLE
            ctrl(0x1E, 0, le32(baud)) // SET_BAUDRATE
            ctrl(0x03, 0x0800) // SET_LINE_CTL: 8 bit, nessuna parità, 1 stop
            // SET_FLOW: DTR attivo; RTS attivo, oppure gestito dal chip insieme a CTS (rtscts).
            val flusso = if (rtscts) le32(0x09) + le32(0x80) else le32(0x01) + le32(0x40)
            ctrl(0x13, 0, flusso + le32(0x80) + le32(0x80))
            if (!rtscts) ctrl(0x07, 0x0303) // SET_MHS: DTR e RTS alti
            return UsbSeriale(conn, listOf(i), endpoint(i, UsbConstants.USB_DIR_IN), endpoint(i, UsbConstants.USB_DIR_OUT))
        }

        private fun cdcAcm(d: UsbDevice, conn: UsbDeviceConnection, baud: Int): UsbSeriale {
            val tutte = (0 until d.interfaceCount).map { d.getInterface(it) }
            val dati = tutte.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA }
                ?: tutte.firstOrNull { haBulk(it) }
                ?: throw IOException("CDC-ACM: nessuna interfaccia dati")
            val comm = tutte.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_COMM }
            val usate = listOfNotNull(comm, dati)
            usate.forEach { reclama(conn, it) }
            val idx = (comm ?: dati).id
            // SET_LINE_CODING (baud, 1 stop, nessuna parità, 8 bit) e SET_CONTROL_LINE_STATE (DTR+RTS).
            // Alcuni chip li ignorano: non è un errore.
            conn.controlTransfer(0x21, 0x20, 0, idx, le32(baud) + byteArrayOf(0, 0, 8), 7, 1_000)
            conn.controlTransfer(0x21, 0x22, 0x03, idx, null, 0, 1_000)
            return UsbSeriale(conn, usate, endpoint(dati, UsbConstants.USB_DIR_IN), endpoint(dati, UsbConstants.USB_DIR_OUT))
        }

        private fun reclama(conn: UsbDeviceConnection, i: UsbInterface) {
            if (!conn.claimInterface(i, true)) throw IOException("interfaccia ${i.id} occupata")
        }

        private fun haBulk(i: UsbInterface) = (0 until i.endpointCount).count {
            i.getEndpoint(it).type == UsbConstants.USB_ENDPOINT_XFER_BULK
        } >= 2

        private fun endpoint(i: UsbInterface, dir: Int) = (0 until i.endpointCount).map { i.getEndpoint(it) }
            .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == dir }
            ?: throw IOException("endpoint bulk mancante sull'interfaccia ${i.id}")

        private fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
    }
}
