package it.handroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessoRemotoTest {
    private fun ip(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }

    @Test
    fun indirizziTailscale() {
        assertTrue(AccessoRemoto.eTailscale(ip(100, 64, 0, 1)))
        assertTrue(AccessoRemoto.eTailscale(ip(100, 101, 2, 3)))
        assertTrue(AccessoRemoto.eTailscale(ip(100, 127, 255, 254)))
    }

    @Test
    fun altriIndirizzi() {
        assertFalse(AccessoRemoto.eTailscale(ip(192, 168, 1, 36)))
        assertFalse(AccessoRemoto.eTailscale(ip(100, 63, 0, 1)))
        assertFalse(AccessoRemoto.eTailscale(ip(100, 128, 0, 1)))
        assertFalse(AccessoRemoto.eTailscale(ip(10, 0, 0, 1)))
    }

    @Test
    fun url() = assertEquals("http://100.64.0.1:8123", AccessoRemoto.url("100.64.0.1"))
}
