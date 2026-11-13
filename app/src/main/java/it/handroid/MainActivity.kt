package it.handroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.net.Inet4Address
import kotlin.concurrent.thread

/** Schermata dell'app: stato del server, comandi del supervisor, backup e aggiornamenti. */
class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var battery: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminReceiver.applyPolicies(this)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        // per i test senza toccare lo schermo: adb shell am start -n it.handroid/.MainActivity --ez avvia true
        if (intent.getBooleanExtra("avvia", false)) HaService.start(this)

        status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, 32) }
        info = TextView(this).apply { textSize = 14f; setPadding(0, 32, 0, 0) }
        battery = button("Escludi dall'ottimizzazione batteria") { askBatteryExemption() }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(status)
            addView(button("Avvia Home Assistant") { HaService.start(this@MainActivity) })
            addView(button("Ferma Home Assistant") { HaService.stop(this@MainActivity) })
            addView(button("Apri l'interfaccia") { open("http://127.0.0.1:8123") })
            addView(battery)
            addView(button("Copia l'ultimo backup in Download") { exportBackup() })
            addView(button("Cerca aggiornamenti") { checkUpdates() })
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
        return "Dal telefono personale apri:\nhttp://${ip ?: "<ip del telefono>"}:8123\n\n" +
            "Avvio automatico: ${if (Supervisor.autostart(this)) "sì" else "no (HA fermato a mano)"}\n" +
            "Ottimizzazione batteria: ${if (Supervisor.batteryExempt(this)) "esclusa" else "attiva: Android può chiudere HA"}\n" +
            "Device owner: ${if (AdminReceiver.isDeviceOwner(this)) "sì" else "no"}\n" +
            "Versione: ${BuildConfig.VERSION_NAME}\n\n" +
            "Log: adb logcat -s HAndroid python.stdout python.stderr"
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
}
