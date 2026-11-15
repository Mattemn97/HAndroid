package it.handroid.esphome

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleAdvTest {
    private fun hex(s: String) = BleAdv.daHex(s)

    @Test fun descrizioneServizio() {
        val c = ProtoReader(BleAdv.ADV.descrizione().payload).lista()
        assertEquals("adv_svc_v1", c[0].string)
        assertEquals(BleAdv.ADV.key, c[1].int)
        val args = c.filter { it.numero == 3 }.map { ProtoReader(it.dati!!).campi() }
        assertEquals(listOf("raw", "duration", "repeat", "ignored_advs", "ignored_duration"), args.map { it[1]!!.string })
        assertEquals(BleAdv.STRING, args[0][2]!!.int)
        assertEquals(BleAdv.STRING_ARRAY, args[3][2]!!.int)
    }

    @Test fun eseguiConArrayPackedENon() {
        val req = ProtoWriter().fixed32(1, BleAdv.SETUP.key)
            .messaggio(2, ProtoWriter().float(3, 500f))
            .messaggio(2, ProtoWriter().bytes(8, byteArrayOf(0x00, 0x00, 0x80.toByte(), 0x3F, 0x00, 0x00, 0x00, 0x40)))
            .messaggio(2, ProtoWriter().string(9, "AA:BB:CC:DD:EE:FF").string(9, "11:22:33:44:55:66"))
            .toByteArray()
        val (key, a) = BleAdv.esegui(req)
        assertEquals(BleAdv.SETUP.key, key)
        assertEquals(500f, a[0].float)
        assertEquals(listOf(1f, 2f), a[1].floats)
        assertEquals(listOf("AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66"), a[2].strings)
        val nonPacked = ProtoWriter().fixed32(1, 7).messaggio(2, ProtoWriter().float(8, 1f).float(8, 3f)).toByteArray()
        assertEquals(listOf(1f, 3f), BleAdv.esegui(nonPacked).second[0].floats)
    }

    @Test fun scomponeUuidEManufacturer() {
        // Flag, lista di UUID a 16 bit (formato FanLamp), manufacturer data 0xFFF0.
        val c = BleAdv.scomponi(hex("020102" + "0503f0081234" + "05fff0ffaabb"))!!
        assertEquals(listOf(0x08F0, 0x3412), c.uuid16)
        assertEquals(0xFFF0, c.produttore[0].first)
        assertArrayEquals(hex("aabb"), c.produttore[0].second)
        assertNull(BleAdv.scomponi(hex("03091234")))  // nome: Android non lo trasmette così
        assertEquals(0xFFF0, BleAdv.produttore(hex("05fff0ffaabb")))
    }

    @Test fun evento() {
        val c = ProtoReader(BleAdv.evento("AA:BB:CC:DD:EE:FF", hex("0201020303f008")).payload).lista()
        assertEquals(BleAdv.EVENTO, c[0].string)
        val mappa = c.filter { it.numero == 2 }.map { ProtoReader(it.dati!!).campi() }.associate { it[1]!!.string to it[2]!!.string }
        assertEquals(mapOf("raw" to "0201020303f008", "orig" to "AA:BB:CC:DD:EE:FF"), mappa)
        assertTrue(c.last { it.numero == 5 }.bool)
    }

    @Test fun filtroDuplicatiEEsclusi() {
        var t = 0L
        val f = BleAdv.Filtro { t }
        val raw = hex("0201020303f008")
        assertTrue(f.passa("AA:BB:CC:DD:EE:FF", raw))
        assertFalse(f.passa("AA:BB:CC:DD:EE:FF", raw))
        t = 20_001
        assertTrue(f.passa("AA:BB:CC:DD:EE:FF", raw))
        f.macs = setOf("11:22:33:44:55:66")
        assertFalse(f.passa("11:22:33:44:55:66", hex("0201020303f009")))
        f.ignora("0201020303f00a", 1000)
        assertFalse(f.passa("AA:BB:CC:DD:EE:FF", hex("0201020303f00a")))
        assertFalse(f.passa("AA:BB:CC:DD:EE:FF", hex("0201")))  // troppo corto
    }
}
