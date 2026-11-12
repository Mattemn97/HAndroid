package it.handroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Riavvia il server dopo l'accensione (anche prima dello sblocco) e dopo un aggiornamento. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> HaService.avvia(ctx)
        }
    }
}
