package it.handroid

import android.content.Context
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Accesso da fuori casa con Tailscale (docs/accesso-remoto.md): l'app trova l'indirizzo 100.x.y.z del
 * telefono e lo scrive in files/url-remoto.txt; handroid_boot lo usa come URL Internet di HA se è vuoto.
 */
object AccessoRemoto {
    const val PACCHETTO_TAILSCALE = "com.tailscale.ipn"
    const val DOWNLOAD_TAILSCALE = "https://tailscale.com/download/android"

    /** Tailscale dà indirizzi nella rete CGNAT 100.64.0.0/10. */
    fun eTailscale(ip: ByteArray) =
        ip.size == 4 && ip[0].toInt() and 0xFF == 100 && ip[1].toInt() and 0xC0 == 64

    fun url(ip: String) = "http://$ip:8123"

    /** Indirizzo Tailscale del telefono, se la VPN è accesa. */
    fun indirizzo(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && eTailscale(it.address) }?.hostAddress
    }.getOrNull()

    /** Aggiorna files/url-remoto.txt (vuoto se Tailscale è spento); lo legge Python all'avvio di HA. */
    fun salva(context: Context): String? {
        val ip = indirizzo()
        File(context.filesDir, "url-remoto.txt").writeText(ip?.let(::url) ?: "")
        return ip
    }

    fun tailscaleInstallato(context: Context) =
        context.packageManager.getLaunchIntentForPackage(PACCHETTO_TAILSCALE) != null
}
