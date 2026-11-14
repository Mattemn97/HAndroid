package it.handroid

import android.Manifest
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Amministratore del dispositivo. Diventa device owner con
 *   adb shell dpm set-device-owner it.handroid/.AdminReceiver
 * (solo su un telefono senza account: vedi docs/guida-sviluppo.md). Da device owner il sistema non
 * può chiudere l'app né limitarla in background.
 */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        applyPolicies(context)
    }

    companion object {
        fun component(context: Context) = ComponentName(context, AdminReceiver::class.java)

        fun isDeviceOwner(context: Context) =
            context.getSystemService(DevicePolicyManager::class.java).isDeviceOwnerApp(context.packageName)

        /** Regole per un server sempre acceso. Si riapplicano a ogni avvio dell'app: sono idempotenti. */
        fun applyPolicies(context: Context) {
            if (!isDeviceOwner(context)) return
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            val admin = component(context)
            // permessi concessi senza chiedere
            for (permission in MainActivity.runtimePermissions()) {
                dpm.setPermissionGrantState(
                    admin, context.packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED,
                )
            }
            // aggiornamenti di sistema solo di notte (2:00-5:00), così il server non si riavvia di giorno
            dpm.setSystemUpdatePolicy(admin, SystemUpdatePolicy.createWindowedInstallPolicy(120, 300))
            // niente schermata di blocco: dopo un riavvio il telefono riparte da solo
            if (Build.VERSION.SDK_INT >= 28) dpm.setKeyguardDisabled(admin, true)
            Log.i("HAndroid", "Regole di device owner applicate")
        }
    }
}
