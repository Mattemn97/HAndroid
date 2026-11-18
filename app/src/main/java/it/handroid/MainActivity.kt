package it.handroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import it.handroid.esphome.EspHomeLan
import java.net.Inet4Address
import kotlin.concurrent.thread

/** Schermata dell'app: stato del server, comandi del supervisor, backup e aggiornamenti. */
class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var battery: Button
    private lateinit var lan: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminReceiver.applyPolicies(this)
        // notifiche e Bluetooth (proxy BLE della M2): senza, HA funziona lo stesso ma senza sensori BLE
        requestPermissions(runtimePermissions(), 0)
        // per i test senza toccare lo schermo: adb shell am start -n it.handroid/.MainActivity --ez avvia true
        // ...e se l'avvio automatico è attivo: MIUI a volte non consegna MY_PACKAGE_REPLACED dopo un
        // aggiornamento. Con HA già in funzione il servizio ignora la richiesta.
        if (intent.getBooleanExtra("avvia", false) || Supervisor.autostart(this)) HaService.start(this)

        status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, 32) }
        info = TextView(this).apply { textSize = 14f; setPadding(0, 32, 0, 0) }
        battery = button("Escludi dall'ottimizzazione batteria") { askBatteryExemption() }
        lan = Switch(this).apply {
            text = "Dispositivo ESPHome visibile agli altri HA della rete"
            isChecked = EspHomeLan.attivo(this@MainActivity)
            setPadding(0, 24, 0, 24)
            setOnCheckedChangeListener { _, on ->
                EspHomeLan.imposta(this@MainActivity, on)
                Toast.makeText(this@MainActivity, "Vale dal prossimo riavvio di HA", Toast.LENGTH_SHORT).show()
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(status)
            addView(title("Home Assistant"))
            addView(button("Apri l'interfaccia") { open("http://127.0.0.1:8123") })
            addView(button("Avvia") { HaService.start(this@MainActivity) })
            addView(button("Riavvia") { HaService.restart(this@MainActivity) })
            addView(button("Ferma") { HaService.stop(this@MainActivity) })
            addView(title("Da fuori casa"))
            addView(button("Cloudflare Tunnel (gratis, con un tuo dominio)") { cloudflare() })
            addView(button("Tailscale") { accessoRemoto() })
            addView(title("Problemi"))
            addView(button("Mostra il log") { mostraLog() })
            addView(button("Condividi il log") { condividiLog() })
            addView(button("Riavvia in modalità di ripristino") {
                Supervisor.setForcedRecovery(this@MainActivity, true); HaService.restart(this@MainActivity)
            })
            addView(button("Esci dalla modalità di ripristino") {
                Supervisor.setForcedRecovery(this@MainActivity, false); HaService.restart(this@MainActivity)
            })
            addView(title("Manutenzione"))
            addView(battery)
            addView(button("Copia l'ultimo backup in Download") { exportBackup() })
            addView(button("Spazio occupato") { spazio() })
            addView(button("Svuota la cache") { background { "Liberati ${Strumenti.formatta(Strumenti.svuotaCache(this@MainActivity))}" } })
            addView(button("Cerca aggiornamenti") { checkUpdates() })
            addView(title("Avanzate"))
            addView(lan)
            addView(button("Azzera la configurazione di HA…") { azzera() })
            addView(info)
        }
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresh)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refresh)
    }

    /** Ogni 2 secondi: ultimo passo dell'avvio, da quanto è fermo lì, e se l'interfaccia risponde. */
    private val refresh = object : Runnable {
        override fun run() {
            val file = HaService.statusFile(this@MainActivity)
            val step = if (file.exists()) file.readText() else "Home Assistant non avviato"
            val age = if (file.exists()) (System.currentTimeMillis() - file.lastModified()) / 1000 else 0
            thread {
                val up = HaService.responds()
                runOnUiThread {
                    status.text = (if (up) "● Interfaccia raggiungibile\n" else "○ Interfaccia non ancora raggiungibile\n") +
                        "Ultimo passo: $step\n(${age}s fa)"
                }
            }
            battery.isEnabled = !Supervisor.batteryExempt(this@MainActivity)
            info.text = info()
            handler.postDelayed(this, 2000)
        }
    }

    private fun info(): String {
        val ip = localIp()
        val remoto = AccessoRemoto.indirizzo()
        return "Dal telefono personale apri:\nhttp://${ip ?: "<ip del telefono>"}:8123\n\n" +
            "Da fuori casa: ${remoto?.let { AccessoRemoto.url(it) + " (Tailscale)" } ?: "Tailscale spento"}\n" +
            "Cloudflare Tunnel: ${Tunnel.stato(this)}\n" +
            "Modalità di ripristino: ${if (Supervisor.recoveryMode(this)) "sì (solo integrazioni essenziali)" else "no"}\n" +
            "Avvio automatico: ${if (Supervisor.autostart(this)) "sì" else "no (HA fermato a mano)"}\n" +
            "Ottimizzazione batteria: ${if (Supervisor.batteryExempt(this)) "esclusa" else "attiva: Android può chiudere HA"}\n" +
            "Device owner: ${if (AdminReceiver.isDeviceOwner(this)) "sì" else "no"}\n" +
            "Versione: ${BuildConfig.VERSION_NAME}\n\n" +
            "Log: adb logcat -s HAndroid python.stdout python.stderr"
    }

    /** Tailscale acceso: condivide l'indirizzo; installato: lo apre; altrimenti porta al download. */
    private fun accessoRemoto() {
        val ip = AccessoRemoto.salva(this)
        when {
            ip != null -> {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, AccessoRemoto.url(ip))
                }, "Indirizzo di Home Assistant da fuori casa"))
                Toast.makeText(this, "Apri ${AccessoRemoto.url(ip)} con Tailscale acceso anche sul telefono personale. " +
                    "HA lo usa come URL Internet dal prossimo riavvio.", Toast.LENGTH_LONG).show()
            }
            AccessoRemoto.tailscaleInstallato(this) -> {
                Toast.makeText(this, "Accedi in Tailscale e accendi la VPN, poi torna qui", Toast.LENGTH_LONG).show()
                startActivity(packageManager.getLaunchIntentForPackage(AccessoRemoto.PACCHETTO_TAILSCALE))
            }
            else -> {
                Toast.makeText(this, "Installa Tailscale (gratis): collega i tuoi dispositivi senza aprire porte", Toast.LENGTH_LONG).show()
                open(AccessoRemoto.DOWNLOAD_TAILSCALE)
            }
        }
    }

    /** Token del tunnel: si incolla quello che dà Cloudflare (anche il comando intero); vuoto = spegne. */
    private fun cloudflare() {
        val campo = EditText(this).apply { hint = "Token del tunnel (eyJ…)"; setText(Tunnel.token(this@MainActivity)) }
        AlertDialog.Builder(this)
            .setTitle("Cloudflare Tunnel")
            .setMessage("Su one.dash.cloudflare.com: Zero Trust → Reti → Tunnel → Crea un tunnel (Cloudflared), " +
                "copia il token e aggiungi un nome host pubblico verso http://localhost:8123. Guida completa: docs/accesso-remoto.md.")
            .setView(campo)
            .setPositiveButton("Salva e riavvia HA") { _, _ ->
                val testo = campo.text.toString()
                val token = Tunnel.estrai(testo)
                when {
                    testo.isBlank() -> { Tunnel.salva(this, null); HaService.restart(this); toast("Tunnel spento") }
                    token == null -> toast("Questo non sembra un token di Cloudflare Tunnel")
                    else -> { Tunnel.salva(this, token); HaService.restart(this); toast("Tunnel salvato: HA riparte") }
                }
            }
            .setNeutralButton("Apri Cloudflare") { _, _ -> open("https://one.dash.cloudflare.com/") }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun mostraLog() = thread {
        val testo = Strumenti.log(this)
        runOnUiThread {
            val vista = TextView(this).apply { text = testo; textSize = 11f; setTextIsSelectable(true); setPadding(32, 16, 32, 16) }
            val scorri = ScrollView(this).apply { addView(vista); post { fullScroll(ScrollView.FOCUS_DOWN) } }
            AlertDialog.Builder(this).setTitle("Log").setView(scorri).setPositiveButton("Chiudi", null).show()
        }
    }

    private fun condividiLog() = thread {
        // i messaggi di condivisione hanno un limite di dimensione: si mandano gli ultimi 400 KB
        val testo = Strumenti.log(this).takeLast(400_000)
        runOnUiThread {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Log di HAndroid")
                putExtra(Intent.EXTRA_TEXT, testo)
            }, "Condividi il log"))
        }
    }

    private fun spazio() = thread {
        val righe = Strumenti.spazio(this).joinToString("\n") { (nome, byte) -> "$nome: ${Strumenti.formatta(byte)}" }
        runOnUiThread { AlertDialog.Builder(this).setTitle("Spazio").setMessage(righe).setPositiveButton("OK", null).show() }
    }

    /** Due conferme: cancella utenti, integrazioni e storico. I backup restano. */
    private fun azzera() {
        AlertDialog.Builder(this)
            .setTitle("Azzerare la configurazione?")
            .setMessage("Cancella utenti, integrazioni, automazioni e storico di Home Assistant. I backup restano " +
                "(Impostazioni → Sistema → Backup per ripristinarli). Prima copia l'ultimo backup in Download, se non l'hai già fatto.")
            .setPositiveButton("Continua") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Sicuro?")
                    .setMessage("Non si può annullare.")
                    .setPositiveButton("Azzera") { _, _ ->
                        HaService.stop(this)
                        background {
                            Thread.sleep(3_000)  // il processo :ha si chiude
                            Strumenti.azzera(this)
                            HaService.start(this)
                            "Configurazione azzerata: HA riparte da zero"
                        }
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun toast(testo: String) = Toast.makeText(this, testo, Toast.LENGTH_LONG).show()

    private fun title(testo: String) = TextView(this).apply {
        text = testo; textSize = 18f; setPadding(0, 40, 0, 8)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    @SuppressLint("BatteryLife")  // un server sempre acceso è proprio il caso previsto da Android
    private fun askBatteryExemption() {
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }

    private fun exportBackup() = background {
        val name = Maintenance.exportLatestBackup(this)
        if (name == null) "Nessun backup: crealo in Home Assistant (Impostazioni → Sistema → Backup)"
        else "Copiato in Download/HAndroid: $name"
    }

    private fun checkUpdates() = background {
        val release = Maintenance.newerRelease()
        if (release == null) "Nessun aggiornamento: questa è l'ultima versione"
        else {
            runOnUiThread { open(release.second) }
            "Versione ${release.first} disponibile: scaricala e installala"
        }
    }

    /** Esegue [work] fuori dal thread dell'interfaccia e mostra il messaggio che restituisce. */
    private fun background(work: () -> String) = thread {
        val message = try {
            work()
        } catch (e: Exception) {
            "Errore: ${e.message}"
        }
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        setOnClickListener { action() }
    }

    private fun open(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun localIp(): String? {
        val cm = getSystemService(ConnectivityManager::class.java)
        val props = cm.getLinkProperties(cm.activeNetwork) ?: return null
        return props.linkAddresses.map { it.address }.firstOrNull { it is Inet4Address }?.hostAddress
    }

    companion object {
        /** Permessi chiesti all'utente (o concessi da soli se l'app è device owner). */
        fun runtimePermissions(): Array<String> = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)  // prima di Android 12 la scansione BLE la richiede
            }
        }.toTypedArray()
    }
}
