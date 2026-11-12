package it.handroid

import android.Manifest
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log

class AdminReceiver : DeviceAdminReceiver()

/**
 * Se l'app è device owner (scripts/installa-handroid.sh --device-owner) si concede da sola
 * i permessi e blocca ciò che potrebbe spegnere il server. Altrimenti non fa niente.
 */
object DeviceOwner {
    fun configura(ctx: Context) {
        val dpm = ctx.getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isDeviceOwnerApp(ctx.packageName)) return
        val admin = ComponentName(ctx, AdminReceiver::class.java)

        val permessi = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permessi.forEach {
            dpm.setPermissionGrantState(admin, ctx.packageName, it, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)
        }
        runCatching {
            // Gli aggiornamenti di sistema riavviano il telefono senza preavviso.
            dpm.setSystemUpdatePolicy(admin, SystemUpdatePolicy.createPostponeInstallPolicy())
            dpm.setUninstallBlocked(admin, ctx.packageName, true)
            dpm.setKeyguardDisabled(admin, true)
        }.onFailure { Log.w(HaService.TAG, "Criterio device owner non applicato", it) }
    }
}
