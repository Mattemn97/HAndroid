package it.handroid

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class StrumentiTest {
    @Test
    fun formatta() {
        assertEquals("512 KB", Strumenti.formatta(512 * 1024).replace(',', '.'))
        assertEquals("150 MB", Strumenti.formatta(150L shl 20).replace(',', '.'))
        assertEquals("2.5 GB", Strumenti.formatta(5L shl 29).replace(',', '.'))
    }

    @Test
    fun codaDelFile() {
        val f = File.createTempFile("log", ".txt").apply { deleteOnExit() }
        f.writeText((1..1000).joinToString("\n") { "riga $it" })
        assertEquals("riga 999\nriga 1000", Strumenti.coda(f, 2))
        // leggendo solo la fine, la prima riga (tagliata a metà) si scarta
        val fine = Strumenti.coda(f, 100, maxByte = 30)
        assertEquals(listOf("riga 998", "riga 999", "riga 1000"), fine.lines())
    }
}
