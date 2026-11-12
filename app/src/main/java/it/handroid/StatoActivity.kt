package it.handroid

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.text.DateFormat
import java.util.Date

/**
 * Schermata nativa di stato e impostazioni (spec 0006, F3), senza AndroidX: si apre dalla
 * notifica del server o tenendo premuto Indietro nella WebView.
 */
class StatoActivity : Activity() {
    private lateinit var info: TextView
    private lateinit var registro: TextView
    private lateinit var lista: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        info = TextView(this).apply { textSize = 16f; setTextIsSelectable(true) }
        lista.addView(info)
        bottone("Backup ora") { Supervisore.invia(this, Supervisore.BACKUP) { putExtra(Supervisore.EXTRA_MOTIVO, "manuale") }; avviso("Backup avviato") }
        bottone("Controlla aggiornamenti") { controllaAggiornamenti() }
        bottone("Riavvia Home Assistant") { conferma("Riavviare il server?") { Supervisore.invia(this, Supervisore.RIAVVIA) } }
        bottone("Cartella per copiare i backup") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SCEGLI_CARTELLA) }
        if (!getSystemService(DevicePolicyManager::class.java).isDeviceOwnerApp(packageName)) {
            bottone("Togli l'ottimizzazione batteria") { batteria() }
            bottone("Avvio automatico (Xiaomi)") { avvioAutomatico() }
        }
        registro = TextView(this).apply { typeface = Typeface.MONOSPACE; textSize = 11f; setTextIsSelectable(true) }
        lista.addView(registro)
        setContentView(ScrollView(this).apply { addView(lista) })
    }

    override fun onResume() {
        super.onResume()
        aggiorna()
    }

    private fun bottone(testo: String, azione: (View) -> Unit) =
        lista.addView(Button(this).apply { text = testo; setOnClickListener(azione) })

    /** Raccoglie lo stato su un thread (rete e logcat) e lo mostra. */
    private fun aggiorna() = Thread {
        val risponde = runCatching {
            val c = URL("http://127.0.0.1:8123/").openConnection() as HttpURLConnection
            c.connectTimeout = 3_000
            c.readTimeout = 3_000
            c.responseCode.also { c.disconnect() } > 0
        }.getOrDefault(false)
        val ip = ip()
        val ultimo = Backup.ultimo(this)
        val testo = buildString {
            appendLine("Server: " + if (risponde) "attivo" else "non risponde")
            appendLine("Indirizzo: " + (ip?.let { "http://$it:8123" } ?: "nessuna rete"))
            appendLine("Home Assistant: " + (Supervisore.versioneHa(this@StatoActivity) ?: "non avviato"))
            appendLine("APK: " + BuildConfig.VERSION_NAME)
            appendLine("Ultimo backup: " + (ultimo?.let { "${it.name} (${DateFormat.getDateTimeInstance().format(Date(it.lastModified()))})" } ?: "nessuno"))
            appendLine("Copia esterna: " + (Supervisore.cartellaEsterna(this@StatoActivity)?.lastPathSegment ?: "non impostata"))
        }
        val log = Supervisore.registro(this).lines().takeLast(30).joinToString("\n") + "\n\n" + logcat()
        runOnUiThread { info.text = testo; registro.text = log }
    }.start()

    private fun ip(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }.firstOrNull { it is Inet4Address }?.hostAddress
    }.getOrNull()

    /** logcat legge senza permessi le righe della nostra app. */
    private fun logcat(): String = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "60", "-v", "time", "-s", "HAndroid:*", "python.stdout:*", "python.stderr:*"))
        p.inputStream.bufferedReader().use { it.readText() }
    }.getOrDefault("")

    private fun controllaAggiornamenti() = Thread {
        val ha = Supervisore.versioneHa(this)
        val haNuova = Supervisore.ultimaHaPypi()
        val apkNuovo = Supervisore.ultimoApk()
        runOnUiThread {
            if (haNuova != null && (ha == null || Regole.confronta(haNuova, ha) > 0)) {
                conferma("Home Assistant $haNuova disponibile (installata: ${ha ?: "nessuna"}). Prima si fa un backup; se dopo il riavvio HA non risponde si torna indietro da soli. Aggiornare?") {
                    Supervisore.invia(this, Supervisore.AGGIORNA_HA) { putExtra(Supervisore.EXTRA_VERSIONE, haNuova) }
                    avviso("Aggiornamento avviato: ci vorranno alcuni minuti")
                }
            } else if (apkNuovo != null && Regole.confronta(apkNuovo, BuildConfig.VERSION_NAME) > 0) {
                conferma("Nuovo APK $apkNuovo disponibile (installato: ${BuildConfig.VERSION_NAME}). Aprire la pagina della release?") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Supervisore.RELEASES)))
                }
            } else {
                avviso(if (haNuova == null && apkNuovo == null) "Controllo non riuscito: rete assente?" else "Tutto aggiornato")
            }
        }
    }.start()

    private fun batteria() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return avviso("Ottimizzazione già disattivata")
        @Suppress("BatteryLife")
        apri(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }

    /** Schermata MIUI/HyperOS dell'avvio automatico; se non c'è, i dettagli dell'app. */
    private fun avvioAutomatico() {
        val miui = Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
        if (!apri(miui)) apri(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun apri(i: Intent) = runCatching { startActivity(i) }.isSuccess

    private fun conferma(msg: String, si: () -> Unit) {
        AlertDialog.Builder(this).setMessage(msg).setPositiveButton("Sì") { _, _ -> si() }.setNegativeButton("No", null).show()
    }

    private fun avviso(msg: String) {
        AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK", null).show()
    }

    @Deprecated("API 29")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (requestCode != SCEGLI_CARTELLA || resultCode != RESULT_OK || uri == null) return
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        Supervisore.impostaCartellaEsterna(this, uri)
        aggiorna()
    }

    private companion object {
        const val SCEGLI_CARTELLA = 2
    }
}
