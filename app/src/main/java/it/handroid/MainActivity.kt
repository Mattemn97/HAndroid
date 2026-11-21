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
import android.view.View
import android.widget.EditText
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
    private lateinit var stato: Ui.Stato
    private lateinit var batteria: View
    private lateinit var indirizzo: Ui.Riga
    private lateinit var cloudflareRiga: Ui.Riga
    private lateinit var tailscaleRiga: Ui.Riga
    private lateinit var backupRiga: Ui.Riga
    private lateinit var avvisoRiga: Ui.Riga
    private lateinit var ripristinoRiga: Ui.Riga
    private lateinit var dettagli: TextView
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

        lan = Switch(this).apply {
            isChecked = EspHomeLan.attivo(this@MainActivity)
            setOnCheckedChangeListener { _, on ->
                EspHomeLan.imposta(this@MainActivity, on)
                toast("Vale dal prossimo riavvio di HA")
            }
        }
        val ui = Ui(this)
        stato = ui.Stato(
            apri = { open("http://127.0.0.1:8123") },
            riavvia = { HaService.restart(this); toast("Home Assistant riparte") },
            accendiSpegni = {
                if (Supervisor.autostart(this)) { HaService.stop(this); toast("Home Assistant si ferma") }
                else { HaService.start(this); toast("Home Assistant si avvia") }
            },
        )
        batteria = ui.avviso("Android può spegnere Home Assistant",
            "Senza l'esclusione dalla batteria il server non riparte dopo un riavvio o un errore.", "Escludi") { askBatteryExemption() }
        indirizzo = ui.riga("Dalla rete di casa", "") { condividi(indirizzoCasa(), "Indirizzo di Home Assistant") }
        cloudflareRiga = ui.riga("Cloudflare Tunnel", "", ::cloudflare)
        tailscaleRiga = ui.riga("Tailscale", "", ::accessoRemoto)
        backupRiga = ui.riga("Backup automatici in una cartella", "", ::cartellaBackup)
        avvisoRiga = ui.riga("Avviso se il server si ferma", "", ::avviso)
        ripristinoRiga = ui.riga("Modalità di ripristino", "") {
            val on = !Supervisor.recoveryMode(this)
            Supervisor.setForcedRecovery(this, on); HaService.restart(this)
            toast(if (on) "Riparto con le sole integrazioni essenziali" else "Riparto normalmente")
        }
        dettagli = ui.piccolo("")
        setContentView(ui.pagina(
            ui.testata("HAndroid", "Home Assistant sul telefono"),
            stato.vista,
            batteria,
            ui.scheda("Da fuori casa", indirizzo.vista, cloudflareRiga.vista, tailscaleRiga.vista),
            ui.scheda("Backup",
                backupRiga.vista,
                ui.riga("Copia l'ultimo backup in Download", "Esce dalla memoria privata dell'app", ::exportBackup).vista,
                ui.riga("Ripristina un backup da file", "Lo porta tra i backup di HA") {
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), RIPRISTINA)
                }.vista),
            ui.scheda("Se qualcosa non va",
                ui.riga("Mostra il log", "Le ultime righe di Android e di HA", ::mostraLog).vista,
                ui.riga("Condividi il log", "Per chiedere aiuto", ::condividiLog).vista,
                avvisoRiga.vista,
                ripristinoRiga.vista),
            ui.scheda("Manutenzione", *listOfNotNull(
                ui.riga("Spazio occupato", "Python, configurazione, backup", ::spazio).vista,
                ui.riga("Svuota la cache", "Pacchetti scaricati e file temporanei") {
                    background { "Liberati ${Strumenti.formatta(Strumenti.svuotaCache(this@MainActivity))}" }
                }.vista,
                // versione Play: aggiorna Play; versione GitHub: avvisa che c'è un codice più nuovo da compilare
                if (BuildConfig.PLAY) null else ui.riga("Cerca aggiornamenti", "Versione ${BuildConfig.VERSION_NAME}", ::checkUpdates).vista,
            ).toTypedArray()),
            ui.scheda("Avanzate",
                ui.interruttore("Telefono visibile agli altri HA", "Il dispositivo ESPHome del telefono si annuncia in rete", lan),
                ui.pericolo("Azzera la configurazione di HA", "Cancella tutto tranne i backup", ::azzera).vista),
            dettagli,
        ))
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresh)
        // senza esclusione Android vieta di riavviare il servizio da dietro le quinte: un riavvio chiesto
        // da HA (o dopo un crash) lascerebbe il server spento. Si chiede una volta per apertura.
        if (!avvisoBatteria && !Supervisor.batteryExempt(this)) {
            avvisoBatteria = true
            AlertDialog.Builder(this)
                .setTitle("Serve l'esclusione dalla batteria")
                .setMessage("Senza, Android non lascia ripartire Home Assistant dopo un riavvio o un errore: il server resterebbe spento.")
                .setPositiveButton("Escludi") { _, _ -> askBatteryExemption() }
                .setNegativeButton("Più tardi", null)
                .show()
        }
    }

    private var avvisoBatteria = false

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refresh)
    }

    /** Ogni 2 secondi: stato del server, ultimo passo dell'avvio e stato di ogni funzione. */
    private val refresh = object : Runnable {
        override fun run() {
            val file = HaService.statusFile(this@MainActivity)
            val step = if (file.exists()) file.readText() else "Home Assistant non avviato"
            val age = if (file.exists()) (System.currentTimeMillis() - file.lastModified()) / 1000 else 0
            thread {
                val up = HaService.responds()
                runOnUiThread {
                    val acceso = Supervisor.autostart(this@MainActivity)
                    stato.mostra(
                        when {
                            up -> Ui.Livello.OK
                            acceso -> Ui.Livello.ATTESA
                            else -> Ui.Livello.SPENTO
                        },
                        when {
                            up -> "Home Assistant è acceso"
                            acceso -> "Home Assistant si sta avviando"
                            else -> "Home Assistant è fermo"
                        },
                        if (up) indirizzoCasa() else "$step · ${Ui.fa(age)}",
                        acceso,
                    )
                }
            }
            batteria.visibility = if (Supervisor.batteryExempt(this@MainActivity)) View.GONE else View.VISIBLE
            indirizzo.sotto(indirizzoCasa() + " · tocca per condividerlo")
            cloudflareRiga.sotto(Tunnel.stato(this@MainActivity))
            tailscaleRiga.sotto(AccessoRemoto.indirizzo()?.let { AccessoRemoto.url(it) } ?: "Spento · gratis, senza aprire porte")
            backupRiga.sotto(BackupEsterno.stato(this@MainActivity))
            avvisoRiga.sotto(if (Controllo.url(this@MainActivity).isEmpty()) "Spento · gratis con healthchecks.io" else "Attivo")
            ripristinoRiga.sotto(if (Supervisor.recoveryMode(this@MainActivity)) "Attiva: solo integrazioni essenziali · tocca per uscire"
                else "Spenta · riparte con le sole integrazioni essenziali")
            dettagli.text = "Versione ${BuildConfig.VERSION_NAME}" +
                (if (AdminReceiver.isDeviceOwner(this@MainActivity)) " · device owner" else "") +
                "\nLog dal PC: adb logcat -s HAndroid python.stdout python.stderr"
            handler.postDelayed(this, 2000)
        }
    }

    private fun indirizzoCasa() = "http://${localIp() ?: "<ip del telefono>"}:8123"

    private fun condividi(testo: String, titolo: String) = startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, testo)
    }, titolo))

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

    private fun cartellaBackup() {
        AlertDialog.Builder(this)
            .setTitle("Backup automatici in una cartella")
            .setMessage("Ogni ora HAndroid copia l'ultimo backup di HA nella cartella che scegli (Drive, cartella di rete, " +
                "chiavetta USB) e tiene gli ultimi ${BackupEsterno.TIENI}. I backup automatici si attivano in HA: " +
                "Impostazioni → Sistema → Backup.\n\nOra: ${BackupEsterno.stato(this)}")
            .setPositiveButton("Scegli la cartella") { _, _ -> startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), CARTELLA) }
            .setNeutralButton("Spegni") { _, _ -> BackupEsterno.salvaCartella(this, null); toast("Copia dei backup spenta") }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun avviso() {
        val campo = EditText(this).apply { hint = "https://hc-ping.com/…"; setText(Controllo.url(this@MainActivity)) }
        AlertDialog.Builder(this)
            .setTitle("Avviso se il server si ferma")
            .setMessage("Crea un controllo gratuito su healthchecks.io (periodo 5 minuti, tolleranza 10) e incolla qui il suo URL. " +
                "Finché HA risponde, HAndroid lo chiama ogni 5 minuti; se smette, healthchecks.io ti scrive. Vuoto = spento.")
            .setView(campo)
            .setPositiveButton("Salva") { _, _ ->
                val url = campo.text.toString().trim()
                when {
                    url.isEmpty() -> { Controllo.salva(this, null); toast("Avviso spento") }
                    !Controllo.valido(url) -> toast("Questo non sembra un indirizzo web")
                    else -> { Controllo.salva(this, url); thread { Controllo.ping(this) }; toast("Avviso attivo: primo segnale inviato") }
                }
            }
            .setNeutralButton("Apri healthchecks.io") { _, _ -> open("https://healthchecks.io/") }
            .setNegativeButton("Annulla", null)
            .show()
    }

    @Deprecated("Activity senza AndroidX: l'API dei risultati qui basta")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            CARTELLA -> {
                contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                BackupEsterno.salvaCartella(this, uri)
                background { BackupEsterno.copia(this)?.let { "Cartella salvata, copiato $it" } ?: "Cartella salvata: copio al prossimo backup" }
            }
            RIPRISTINA -> background {
                val nome = BackupEsterno.importa(this, uri)
                HaService.restart(this)
                "Backup $nome pronto: dopo il riavvio vai in HA → Impostazioni → Sistema → Backup e ripristinalo"
            }
        }
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
            "Versione ${release.first} disponibile: aggiorna il codice e ricompila (guida-sviluppo.md)"
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

    private fun open(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun localIp(): String? {
        val cm = getSystemService(ConnectivityManager::class.java)
        val props = cm.getLinkProperties(cm.activeNetwork) ?: return null
        return props.linkAddresses.map { it.address }.firstOrNull { it is Inet4Address }?.hostAddress
    }

    companion object {
        private const val CARTELLA = 1
        private const val RIPRISTINA = 2

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
