package it.handroid

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegoleTest {
    @Test fun rotazioneTieneGliUltimiSette() {
        val nomi = (1..10).map { "handroid_202611%02d_033000.tar".format(it) } + "manuale.tar" + "handroid_x.tar.tmp"
        val via = Regole.backupDaEliminare(nomi.shuffled())
        assertEquals(
            listOf("handroid_20261103_033000.tar", "handroid_20261102_033000.tar", "handroid_20261101_033000.tar"),
            via,
        )
    }

    @Test fun rotazioneConPochiBackupNonCancella() {
        assertEquals(emptyList<String>(), Regole.backupDaEliminare(listOf("handroid_20261101_033000.tar")))
    }

    @Test fun confrontoVersioni() {
        assertTrue(Regole.confronta("2026.11.1", "2026.11.2") < 0)
        assertTrue(Regole.confronta("2026.12.0", "2026.11.10") > 0)
        assertTrue(Regole.confronta("2026.12.0b1", "2026.12.0") < 0)
        assertTrue(Regole.confronta("2026.12.0b2", "2026.12.0b1") > 0)
        assertEquals(0, Regole.confronta("v0.1.42", "0.1.42"))
        assertTrue(Regole.confronta("v0.1.9", "0.1.10") < 0)
        assertEquals(0, Regole.confronta("1.0", "1.0.0"))
    }

    @Test fun decisioneDiRollback() {
        assertEquals(Regole.Avvio.NORMALE, Regole.decidiAvvio(inProva = true, falliti = 2, max = 3))
        assertEquals(Regole.Avvio.ROLLBACK, Regole.decidiAvvio(inProva = true, falliti = 3, max = 3))
        assertEquals(Regole.Avvio.PROVVISORIA, Regole.decidiAvvio(inProva = false, falliti = 3, max = 3))
    }

    @Test fun prossimaEsecuzione() {
        val c = Calendar.getInstance().apply { set(2026, 10, 12, 2, 0, 0); set(Calendar.MILLISECOND, 0) }
        val oggi = Regole.prossima(c.timeInMillis, 3, 30)
        assertEquals(90 * 60_000L, oggi - c.timeInMillis)
        val domani = Calendar.getInstance().apply { timeInMillis = Regole.prossima(oggi, 3, 30) }
        assertEquals(13, domani.get(Calendar.DAY_OF_MONTH))
    }
}
