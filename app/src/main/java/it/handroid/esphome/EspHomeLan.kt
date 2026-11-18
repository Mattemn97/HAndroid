package it.handroid.esphome

import android.content.Context
import java.io.File

/**
 * Dispositivo ESPHome raggiungibile anche dagli altri HA della rete, non solo da quello del telefono.
 * Un file in files/ e non SharedPreferences: lo scrive l'app e lo legge il processo :ha.
 */
object EspHomeLan {
    private fun file(context: Context) = File(context.filesDir, "esphome-lan")

    fun attivo(context: Context) = file(context).exists()

    fun imposta(context: Context, attivo: Boolean) {
        if (attivo) file(context).writeText("1") else file(context).delete()
    }
}
