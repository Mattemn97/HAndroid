package it.handroid

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupEsternoTest {
    @Test
    fun tieneIPiuRecenti() {
        val file = listOf(
            Triple("1", "a.tar", 100L), Triple("2", "b.tar", 300L), Triple("3", "c.tar", 200L),
            Triple("4", "note.txt", 1L),  // non è un backup: non si tocca
        )
        assertEquals(listOf("1"), BackupEsterno.daCancellare(file, 2))
        assertEquals(emptyList<String>(), BackupEsterno.daCancellare(file, 7))
    }
}
