package it.handroid.esphome

import java.io.ByteArrayOutputStream

/**
 * Codec protobuf minimo, scritto a mano: all'API ESPHome bastano varint, fixed32,
 * float, bool e stringhe/bytes. I campi a zero si omettono, come fa protobuf.
 */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun varint(campo: Int, v: Long) = apply { if (v != 0L) { tag(campo, 0); rawVarint(out, v) } }
    fun uint(campo: Int, v: Int) = varint(campo, v.toLong() and 0xFFFFFFFFL)
    fun bool(campo: Int, v: Boolean) = varint(campo, if (v) 1 else 0)
    fun fixed32(campo: Int, v: Int) = apply { if (v != 0) { tag(campo, 5); rawFixed32(v) } }
    // Il float a zero si scrive comunque: 0.0 è un valore, non "assente".
    fun float(campo: Int, v: Float) = apply { tag(campo, 5); rawFixed32(java.lang.Float.floatToIntBits(v)) }
    fun string(campo: Int, v: String?) = bytes(campo, v?.toByteArray(Charsets.UTF_8))
    fun bytes(campo: Int, v: ByteArray?) = apply {
        if (v != null && v.isNotEmpty()) { tag(campo, 2); rawVarint(out, v.size.toLong()); out.write(v) }
    }

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun tag(campo: Int, tipo: Int) = rawVarint(out, (campo.toLong() shl 3) or tipo.toLong())
    private fun rawFixed32(v: Int) { for (i in 0 until 4) out.write((v ushr (8 * i)) and 0xFF) }

    companion object {
        fun rawVarint(out: ByteArrayOutputStream, valore: Long) {
            var v = valore
            while (v and 0x7FL.inv() != 0L) { out.write(((v and 0x7F) or 0x80).toInt()); v = v ushr 7 }
            out.write(v.toInt())
        }
    }
}

/** Legge un messaggio campo per campo: `campi()` restituisce numero di campo e valore grezzo. */
class ProtoReader(private val buf: ByteArray) {
    private var pos = 0

    /** Valore di un campo: `num` per varint/fixed, `dati` per i length-delimited. */
    class Campo(val numero: Int, val num: Long, val dati: ByteArray?) {
        val int get() = num.toInt()
        val bool get() = num != 0L
        val float get() = java.lang.Float.intBitsToFloat(num.toInt())
        val string get() = String(dati ?: ByteArray(0), Charsets.UTF_8)
    }

    fun campi(): Map<Int, Campo> {
        val m = HashMap<Int, Campo>()
        while (pos < buf.size) {
            val tag = varint()
            val numero = (tag ushr 3).toInt()
            val c = when ((tag and 7).toInt()) {
                0 -> Campo(numero, varint(), null)
                1 -> Campo(numero, fixed(8), null)
                2 -> { val n = varint().toInt(); require(n >= 0 && pos + n <= buf.size) { "lunghezza non valida" }
                    Campo(numero, 0, buf.copyOfRange(pos, pos + n)).also { pos += n } }
                5 -> Campo(numero, fixed(4), null)
                else -> throw IllegalArgumentException("wire type non supportato")
            }
            m[numero] = c
        }
        return m
    }

    private fun varint(): Long {
        var r = 0L
        var shift = 0
        while (true) {
            require(pos < buf.size && shift < 64) { "varint troncato" }
            val b = buf[pos++].toInt() and 0xFF
            r = r or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) return r
            shift += 7
        }
    }

    private fun fixed(n: Int): Long {
        require(pos + n <= buf.size) { "fixed troncato" }
        var r = 0L
        for (i in 0 until n) r = r or ((buf[pos++].toLong() and 0xFF) shl (8 * i))
        return r
    }
}
