package it.handroid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlloTest {
    @Test
    fun indirizzi() {
        assertTrue(Controllo.valido("https://hc-ping.com/1234-abcd"))
        assertTrue(Controllo.valido(" http://casa.lan/ping "))
        assertFalse(Controllo.valido("hc-ping.com/1234"))
        assertFalse(Controllo.valido("https://"))
        assertFalse(Controllo.valido("ciao mondo"))
    }
}
