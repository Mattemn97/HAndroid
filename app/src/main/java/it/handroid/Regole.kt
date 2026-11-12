package it.handroid

import java.util.Calendar

/** Logica pura del supervisore (spec 0006), senza dipendenze Android: si prova con i test JVM. */
object Regole {
    const val PREFISSO_BACKUP = "handroid_"
    const val BACKUP_DA_TENERE = 7

    /** Backup automatici da cancellare: tutti i "handroid_*.tar" oltre i [tieni] più recenti.
     *  Il nome contiene la data (handroid_aaaammgg_hhmmss), quindi l'ordine alfabetico è cronologico. */
    fun backupDaEliminare(nomi: List<String>, tieni: Int = BACKUP_DA_TENERE): List<String> =
        nomi.filter { it.startsWith(PREFISSO_BACKUP) && it.endsWith(".tar") }
            .sortedDescending()
            .drop(tieni)

    /** Confronta versioni tipo "2026.11.1", "v0.1.42", "2026.12.0b1": negativo se a < b.
     *  Una pre-release (b, rc, dev) viene prima della versione finale con gli stessi numeri. */
    fun confronta(a: String, b: String): Int {
        val (na, pa) = scomponi(a)
        val (nb, pb) = scomponi(b)
        for (i in 0 until maxOf(na.size, nb.size)) {
            val d = na.getOrElse(i) { 0 }.compareTo(nb.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return when {
            pa == pb -> 0
            pa.isEmpty() -> 1
            pb.isEmpty() -> -1
            else -> pa.compareTo(pb)
        }
    }

    private fun scomponi(v: String): Pair<List<Int>, String> {
        val pulita = v.trim().removePrefix("v")
        val numeri = Regex("""^\d+(\.\d+)*""").find(pulita)?.value.orEmpty()
        return numeri.split('.').filter { it.isNotEmpty() }.map { it.toInt() } to pulita.removePrefix(numeri)
    }

    enum class Avvio { NORMALE, ROLLBACK, PROVVISORIA }

    /** Cosa fare all'avvio del servizio, dati gli avvii falliti di fila prima di questo.
     *  Con un aggiornamento in prova, prima della modalità provvisoria si torna alla versione di prima. */
    fun decidiAvvio(inProva: Boolean, falliti: Int, max: Int): Avvio = when {
        falliti < max -> Avvio.NORMALE
        inProva -> Avvio.ROLLBACK
        else -> Avvio.PROVVISORIA
    }

    /** Prossimo istante (ms) alle [ora]:[minuto] locali, strettamente dopo [adesso]. */
    fun prossima(adesso: Long, ora: Int, minuto: Int): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = adesso
            set(Calendar.HOUR_OF_DAY, ora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= adesso) c.add(Calendar.DAY_OF_MONTH, 1)
        return c.timeInMillis
    }
}
