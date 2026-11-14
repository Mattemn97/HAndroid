package it.handroid.esphome

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BleTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test fun indirizzoEMac() {
        assertEquals(0xA4C1381A2B3CL, CodecBle.indirizzo("A4:C1:38:1A:2B:3C"))
        assertEquals("A4:C1:38:1A:2B:3C", CodecBle.mac(0xA4C1381A2B3CL))
    }

    @Test fun sintZigzag() {
        // -60 -> 119 (zigzag), 1 -> 2
        assertArrayEquals(hex("1077"), ProtoWriter().sint(2, -60).toByteArray())
        assertArrayEquals(hex("1002"), ProtoWriter().sint(2, 1).toByteArray())
    }

    @Test fun taglioDegliZeriInCoda() {
        val raw = hex("020106" + "0416951a01") + ByteArray(54)
        assertArrayEquals(hex("0201060416951a01"), CodecBle.taglia(raw))
        assertArrayEquals(ByteArray(0), CodecBle.taglia(null))
        // Struttura che sfora: si tiene solo la parte valida.
        assertArrayEquals(hex("020106"), CodecBle.taglia(hex("02010609ff")))
    }

    @Test fun annuncioGrezzo() {
        val b = CodecBle.annuncio(Annuncio(0xA4C1381A2B3CL, -60, 0, hex("020106")))
        // address varint, rssi sint32, address_type omesso (0), data
        assertArrayEquals(hex("08bcd6e8c0939829" + "1077" + "2203020106"), b)
    }

    @Test fun lottiDaSedici() {
        val l = LottoAnnunci(16)
        val a = Annuncio(1, -50, 1, hex("020106"))
        repeat(15) { assertNull(l.aggiungi(a)) }
        val m = l.aggiungi(a)!!
        assertEquals(MsgBle.RAW_ADV, m.tipo)
        assertNull(l.svuota())
        l.aggiungi(a)
        val uno = l.svuota()!!
        val c = ProtoReader(uno.payload).campi()
        val annuncio = ProtoReader(c[1]!!.dati!!).campi()
        assertEquals(1L, annuncio[1]!!.num)
        assertEquals(1, annuncio[3]!!.int)
        assertEquals(16 * (2 + CodecBle.annuncio(a).size), m.payload.size)
    }

    @Test fun slot() {
        val s = Slot(3)
        assertTrue(s.occupa(1)); assertTrue(s.occupa(2)); assertTrue(s.occupa(1))
        assertTrue(s.occupa(3)); assertFalse(s.occupa(4))
        assertEquals(0, s.liberi)
        assertTrue(s.libera(2)); assertFalse(s.libera(2))
        assertEquals(1, s.liberi)
        assertEquals(listOf(1L, 3L), s.occupati)
        val c = ProtoReader(CodecBle.liberi(s).payload).campi()
        assertEquals(1, c[1]!!.int); assertEquals(3, c[2]!!.int)
    }

    @Test fun codaUnaOperazioneAllaVolta() {
        val partite = mutableListOf<Int>()
        val q = CodaGatt<Int> { partite += it; it != 2 }
        q.accoda(1); q.accoda(2); q.accoda(3)
        assertEquals(listOf(1), partite)
        assertEquals(1, q.finita())
        // La 2 viene rifiutata da Android: si passa subito alla 3.
        assertEquals(listOf(1, 2, 3), partite)
        assertEquals(3, q.corrente)
        assertEquals(listOf(3), q.svuota())
        assertNull(q.finita())
    }

    @Test fun serviziConUuidEHandle() {
        val u = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")
        val t = TabellaHandle<String>()
        val hs = t.registra("s"); val hc = t.registra("c"); val hd = t.registra("d")
        assertEquals(listOf(1, 2, 3), listOf(hs, hc, hd))
        assertEquals("c", t.cerca(2))
        val m = CodecBle.servizi(7, listOf(ServizioGatt(u, hs, listOf(CaratteristicaGatt(u, hc, 0x12, listOf(DescrittoreGatt(u, hd)))))))
        assertEquals(MsgBle.GET_SERVICES_RESP, m.tipo)
        val c = ProtoReader(m.payload).campi()
        assertEquals(7L, c[1]!!.num)
        val s = ProtoReader(c[2]!!.dati!!).campi()
        assertEquals(1, s[2]!!.int)
        val ch = ProtoReader(s[3]!!.dati!!).campi()
        assertEquals(2, ch[2]!!.int); assertEquals(0x12, ch[3]!!.int)
        assertEquals(3, ProtoReader(ch[4]!!.dati!!).campi()[2]!!.int)
        // uuid ripetuto: il lettore tiene l'ultimo valore, cioè i 64 bit bassi.
        assertEquals(u.leastSignificantBits, s[1]!!.num)
    }

    @Test fun featureFlagsInDeviceInfo() {
        val srv = ApiServer(InfoDispositivo("h", "H", "02:00:00:00:00:01", "m", "p", "v"), Registro(), porta = 0)
        srv.moduli += object : Modulo {
            override fun gestisci(conn: Connessione, tipo: Int, payload: ByteArray) = false
            override fun deviceInfo(w: ProtoWriter) { w.uint(15, MsgBle.FEATURES) }
        }
        assertEquals(0x67, ProtoReader(srv.deviceInfo()).campi()[15]!!.int)
    }
}
