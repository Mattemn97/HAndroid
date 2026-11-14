package it.handroid.esphome

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * Framing in chiaro dell'API ESPHome: 0x00, varint lunghezza del payload,
 * varint tipo del messaggio, payload protobuf. (Il primo byte 0x01 indicherebbe Noise.)
 */
object Frame {
    const val MAX_PAYLOAD = 1 shl 20

    fun codifica(tipo: Int, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(payload.size + 8)
        out.write(0)
        ProtoWriter.rawVarint(out, payload.size.toLong())
        ProtoWriter.rawVarint(out, tipo.toLong())
        out.write(payload)
        return out.toByteArray()
    }

    /** Legge un frame; `null` se il client ha chiuso la connessione in modo pulito. */
    fun leggi(input: InputStream): Pair<Int, ByteArray>? {
        val preambolo = input.read()
        if (preambolo < 0) return null
        if (preambolo != 0) throw IOException("Preambolo $preambolo: serve il protocollo in chiaro (niente chiave di crittografia)")
        val n = varint(input)
        if (n < 0 || n > MAX_PAYLOAD) throw IOException("Payload troppo grande: $n")
        val tipo = varint(input)
        val payload = ByteArray(n)
        var letti = 0
        while (letti < n) {
            val r = input.read(payload, letti, n - letti)
            if (r < 0) throw EOFException()
            letti += r
        }
        return tipo to payload
    }

    private fun varint(input: InputStream): Int {
        var r = 0
        var shift = 0
        while (shift < 35) {
            val b = input.read()
            if (b < 0) throw EOFException()
            r = r or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return r
            shift += 7
        }
        throw IOException("varint troppo lungo")
    }
}
