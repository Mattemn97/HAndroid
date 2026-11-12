package it.handroid.esphome

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.Socket

class EspHomeTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test fun varintEStringaComeProtobuf() {
        // Esempi della documentazione protobuf: 150 -> 08 96 01, "testing" -> 12 07 ...
        assertArrayEquals(hex("089601"), ProtoWriter().uint(1, 150).toByteArray())
        assertArrayEquals(hex("120774657374696e67"), ProtoWriter().string(2, "testing").toByteArray())
        assertArrayEquals(ByteArray(0), ProtoWriter().uint(1, 0).bool(2, false).string(3, "").toByteArray())
    }

    @Test fun andataERitorno() {
        val b = ProtoWriter().fixed32(1, 0xDEADBEEF.toInt()).float(2, 21.5f).bool(3, true)
            .string(4, "città").uint(5, -1).toByteArray()
        val c = ProtoReader(b).campi()
        assertEquals(0xDEADBEEF.toInt(), c[1]!!.int)
        assertEquals(21.5f, c[2]!!.float)
        assertTrue(c[3]!!.bool)
        assertEquals("città", c[4]!!.string)
        assertEquals(-1, c[5]!!.int)
    }

    @Test fun floatZeroSiScrive() {
        assertArrayEquals(hex("1500000000"), ProtoWriter().float(2, 0f).toByteArray())
    }

    @Test(expected = IllegalArgumentException::class) fun messaggioTroncato() {
        ProtoReader(hex("0a05616263")).campi()
    }

    @Test fun frame() {
        val payload = ByteArray(200) { it.toByte() }
        val f = Frame.codifica(25, payload)
        assertArrayEquals(hex("00c80119"), f.copyOfRange(0, 4))
        val input = ByteArrayInputStream(f + Frame.codifica(7, ByteArray(0)))
        val (t1, p1) = Frame.leggi(input)!!
        assertEquals(25, t1); assertArrayEquals(payload, p1)
        val (t2, p2) = Frame.leggi(input)!!
        assertEquals(7, t2); assertEquals(0, p2.size)
        assertNull(Frame.leggi(input))
    }

    @Test(expected = IOException::class) fun frameNoiseRifiutato() {
        Frame.leggi(ByteArrayInputStream(hex("010000")))
    }

    @Test fun chiaveFnv1a() {
        // Valore di riferimento FNV-1a 32 bit di "a".
        assertEquals(0xE40C292C.toInt(), Entita.fnv1a("a"))
    }

    @Test fun limitazioneFrequenza() {
        val r = Registro(intervalloMinMs = 200)
        val s = r.aggiungi(Sensore("t", "T", soglia = 1f))
        val ricevuti = mutableListOf<Float>()
        r.ascolta { ricevuti += ProtoReader(it.payload).campi()[2]!!.float }
        r.aggiorna(s, 10f)      // subito
        r.aggiorna(s, 10.5f)    // sotto soglia: scartato
        r.aggiorna(s, 12f)      // troppo presto: rinviato
        r.aggiorna(s, 13f)      // sostituisce il precedente in attesa
        Thread.sleep(400)
        assertEquals(listOf(10f, 13f), ricevuti)
    }

    @Test fun sessioneCompleta() {
        val r = Registro(intervalloMinMs = 0)
        val s = r.aggiungi(Sensore("batteria", "Batteria", unita = "%"))
        var torcia = false
        val t = r.aggiungi(Interruttore("torcia", "Torcia") { torcia = it })
        r.aggiorna(s, 80f)
        val server = ApiServer(InfoDispositivo("handroid", "HAndroid", "02:00:00:00:00:01", "Test", "HAndroid", "2025.11.0"),
            r, porta = 0)
        server.avvia()
        Socket("127.0.0.1", server.portaEffettiva).use { sock ->
            val out = sock.getOutputStream()
            val inp = sock.getInputStream()
            fun manda(tipo: Int, p: ByteArray = ByteArray(0)) = out.write(Frame.codifica(tipo, p))
            fun ricevi() = Frame.leggi(inp)!!

            manda(Msg.HELLO_REQ, ProtoWriter().string(1, "aioesphomeapi").uint(2, 1).uint(3, 10).toByteArray())
            val (th, ph) = ricevi()
            assertEquals(Msg.HELLO_RESP, th)
            assertEquals("handroid", ProtoReader(ph).campi()[4]!!.string)

            manda(Msg.DEVICE_INFO_REQ)
            val (td, pd) = ricevi()
            assertEquals(Msg.DEVICE_INFO_RESP, td)
            assertEquals("02:00:00:00:00:01", ProtoReader(pd).campi()[3]!!.string)

            manda(Msg.LIST_ENTITIES_REQ)
            assertEquals(Msg.LIST_SENSOR, ricevi().first)
            assertEquals(Msg.LIST_SWITCH, ricevi().first)
            assertEquals(Msg.LIST_DONE, ricevi().first)

            manda(Msg.SUBSCRIBE_STATES)
            val (ts, ps) = ricevi()
            assertEquals(Msg.SENSOR_STATE, ts)
            assertEquals(80f, ProtoReader(ps).campi()[2]!!.float)

            manda(Msg.SWITCH_COMMAND, ProtoWriter().fixed32(1, t.key).bool(2, true).toByteArray())
            manda(Msg.PING_REQ)
            assertEquals(Msg.PING_RESP, ricevi().first)
            assertTrue(torcia)

            r.aggiorna(t, true)
            val (tt, pt) = ricevi()
            assertEquals(Msg.SWITCH_STATE, tt)
            assertTrue(ProtoReader(pt).campi()[2]!!.bool)

            manda(Msg.DISCONNECT_REQ)
            assertEquals(Msg.DISCONNECT_RESP, ricevi().first)
        }
        server.ferma()
    }
}
