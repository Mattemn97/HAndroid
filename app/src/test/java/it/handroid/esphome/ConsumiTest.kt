package it.handroid.esphome

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsumiTest {
    @Test
    fun potenza() {
        assertEquals(1.92f, Consumi.watt(500_000, 3.84f), 0.001f)   // 500 mA in carica a 3,84 V
        assertEquals(-0.77f, Consumi.watt(-200_000, 3.85f), 0.001f) // 200 mA in scarica
    }

    @Test
    fun percento() {
        assertEquals(25f, Consumi.percento(2, 8), 0.001f)
        assertEquals(0f, Consumi.percento(1, 0), 0.001f)
    }
}
