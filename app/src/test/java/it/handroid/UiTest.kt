package it.handroid

import org.junit.Assert.assertEquals
import org.junit.Test

class UiTest {
    @Test
    fun tempoTrascorso() {
        assertEquals("ora", Ui.fa(2))
        assertEquals("40 s fa", Ui.fa(40))
        assertEquals("3 min fa", Ui.fa(200))
        assertEquals("2 h fa", Ui.fa(7300))
    }
}
