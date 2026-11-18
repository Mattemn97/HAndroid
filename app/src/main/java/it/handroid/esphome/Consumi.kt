package it.handroid.esphome

/** Conti dei sensori di consumo del telefono, separati per poterli provare senza Android. */
object Consumi {
    /** Potenza in W da corrente (µA, positiva in carica) e tensione (V) della batteria. */
    fun watt(microampere: Long, volt: Float) = microampere / 1_000_000f * volt

    fun percento(libera: Long, totale: Long) = if (totale > 0) 100f * libera / totale else 0f
}
