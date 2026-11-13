package it.handroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Fa ripartire Home Assistant dopo l'accensione del telefono e dopo un aggiornamento dell'app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS || !Supervisor.autostart(context)) return
        Log.i("HAndroid", "Avvio automatico (${intent.action})")
        HaService.start(context)
    }

    private companion object {
        val ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
