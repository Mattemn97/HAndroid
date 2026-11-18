package it.handroid

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Riquadro "Home Assistant" nelle impostazioni rapide: acceso = avvio automatico attivo, un tocco lo accende o lo ferma. */
class HaTile : TileService() {
    override fun onStartListening() = aggiorna(Supervisor.autostart(this))

    override fun onClick() {
        val acceso = !Supervisor.autostart(this)
        if (acceso) HaService.start(this) else HaService.stop(this)
        aggiorna(acceso)
    }

    private fun aggiorna(acceso: Boolean) {
        qsTile?.run {
            state = if (acceso) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (acceso) "Attivo" else "Fermo"
            updateTile()
        }
    }
}
