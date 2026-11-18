package it.handroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class TunnelTest {
    private val token = Base64.getEncoder().encodeToString("""{"a":"acc","t":"tun","s":"segreto"}""".toByteArray())

    @Test
    fun tokenValido() {
        assertTrue(Tunnel.valido(token))
        assertTrue(Tunnel.valido(" $token\n"))
    }

    @Test
    fun tokenNonValido() {
        assertFalse(Tunnel.valido("ciao"))
        assertFalse(Tunnel.valido(Base64.getEncoder().encodeToString("""{"a":"acc","t":"tun"}""".toByteArray())))
        assertFalse(Tunnel.valido(""))
    }

    @Test
    fun estraeDalComando() {
        assertEquals(token, Tunnel.estrai("sudo cloudflared service install $token"))
        assertEquals(token, Tunnel.estrai("cloudflared tunnel run --token $token"))
        assertNull(Tunnel.estrai("cloudflared tunnel run"))
    }
}
