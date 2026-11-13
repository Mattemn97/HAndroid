package it.handroid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.URL
import kotlin.concurrent.thread

/** Schermata minima della M0: avvia il server e mostra l'indirizzo da aprire. */
class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        // per i test senza toccare lo schermo: adb shell am start -n it.handroid/.MainActivity --ez avvia true
        if (intent.getBooleanExtra("avvia", false)) HaService.start(this)

        val ip = localIp()
        val info = TextView(this).apply {
            textSize = 16f
            text = "Il primo avvio estrae Python e può richiedere qualche minuto.\n\n" +
                "Dal telefono personale apri:\nhttp://${ip ?: "<ip del telefono>"}:8123\n\n" +
                "Log: adb logcat -s HAndroid python.stdout python.stderr"
        }
        status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, 32) }
        val start = Button(this).apply {
            text = "Avvia Home Assistant"
            setOnClickListener { HaService.start(this@MainActivity) }
        }
        val open = Button(this).apply {
            text = "Apri l'interfaccia"
            setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8123"))) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(status)
            addView(info)
            addView(start)
            addView(open)
        })
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
                val up = responds()
                runOnUiThread {
                    status.text = (if (up) "● Interfaccia raggiungibile\n" else "○ Interfaccia non ancora raggiungibile\n") +
                        "Ultimo passo: $step\n(${age}s fa)"
                }
            }
            handler.postDelayed(this, 2000)
        }
    }

    private fun responds() = try {
        (URL("http://127.0.0.1:8123/manifest.json").openConnection() as HttpURLConnection).run {
            connectTimeout = 1000
            readTimeout = 1000
            (responseCode == 200).also { disconnect() }
        }
    } catch (e: Exception) {
        false
    }

    private fun localIp(): String? {
        val cm = getSystemService(ConnectivityManager::class.java)
        val props = cm.getLinkProperties(cm.activeNetwork) ?: return null
        return props.linkAddresses.map { it.address }.firstOrNull { it is Inet4Address }?.hostAddress
    }
}
