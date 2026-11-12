package it.handroid.esphome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.security.SecureRandom

/**
 * Il telefono come dispositivo ESPHome "HAndroid" (spec 0004): avvia il server API,
 * lo annuncia via mDNS e collega i sensori di Android alle entità.
 */
object EspHome {
    private const val TAG = ApiServer.TAG
    // Versione ESPHome dichiarata a HA: serve solo a mostrarla, non attiva funzioni.
    private const val VERSIONE = "2025.11.0"

    @Volatile var server: ApiServer? = null
        private set

    fun avvia(ctx: Context) = runCatching {
        val prefs = ctx.createDeviceProtectedStorageContext().getSharedPreferences("esphome", Context.MODE_PRIVATE)
        val info = InfoDispositivo(
            nome = "handroid", nomeVisibile = "HAndroid", mac = mac(prefs),
            modello = "${Build.MANUFACTURER} ${Build.MODEL}", produttore = "HAndroid", versione = VERSIONE,
        )
        val registro = Registro()
        val thread = HandlerThread("esphome-sensori").apply { start() }
        Sensori(ctx, registro, Handler(thread.looper)).collega()
        val s = ApiServer(info, registro, soloLocale = !prefs.getBoolean("lan", false))
        s.avvia()
        server = s
        annuncia(ctx, info, s.portaEffettiva)
        Log.i(TAG, "Dispositivo ESPHome attivo su :${s.portaEffettiva}, ${registro.entita.size} entità")
    }.onFailure { Log.e(TAG, "Server ESPHome non avviato", it) }

    /** Android non dà il MAC vero: se ne genera uno stabile, locale (bit 0x02), salvato una volta. */
    private fun mac(prefs: android.content.SharedPreferences): String {
        prefs.getString("mac", null)?.let { return it }
        val b = ByteArray(6).also { SecureRandom().nextBytes(it) }
        b[0] = ((b[0].toInt() and 0xFC) or 0x02).toByte()
        return b.joinToString(":") { "%02X".format(it) }.also { prefs.edit().putString("mac", it).apply() }
    }

    private fun annuncia(ctx: Context, info: InfoDispositivo, porta: Int) {
        val si = NsdServiceInfo().apply {
            serviceName = info.nome
            serviceType = "_esphomelib._tcp"
            port = porta
            // Gli stessi attributi TXT di un ESPHome vero; HA usa "mac" per riconoscerlo.
            setAttribute("mac", info.mac.replace(":", "").lowercase())
            setAttribute("version", info.versione)
            setAttribute("platform", "Android")
            setAttribute("network", "wifi")
            setAttribute("friendly_name", info.nomeVisibile)
        }
        ctx.getSystemService(NsdManager::class.java).registerService(si, NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(s: NsdServiceInfo) = Log.i(TAG, "mDNS: ${s.serviceName} annunciato")
                override fun onRegistrationFailed(s: NsdServiceInfo, e: Int) { Log.w(TAG, "mDNS non registrato: $e") }
                override fun onServiceUnregistered(s: NsdServiceInfo) {}
                override fun onUnregistrationFailed(s: NsdServiceInfo, e: Int) {}
            })
    }
}

/** Collega batteria, sensori fisici, WiFi, uptime e torcia alle entità del registro. */
private class Sensori(private val ctx: Context, private val r: Registro, private val h: Handler) {
    private val batteria = r.aggiungi(Sensore("batteria", "Batteria", unita = "%", deviceClass = "battery"))
    private val inCarica = r.aggiungi(SensoreBinario("in_carica", "In carica", deviceClass = "battery_charging"))
    private val tempBatteria = r.aggiungi(Sensore("temperatura_batteria", "Temperatura batteria", unita = "°C",
        decimali = 1, deviceClass = "temperature", diagnostica = true, soglia = 0.5f))
    private val rssi = r.aggiungi(Sensore("segnale_wifi", "Segnale WiFi", unita = "dBm", deviceClass = "signal_strength",
        diagnostica = true, soglia = 3f))
    private val uptime = r.aggiungi(Sensore("uptime", "Uptime", unita = "s", deviceClass = "duration",
        stateClass = 2, diagnostica = true, icona = "mdi:timer-outline"))

    fun collega() {
        val sm = ctx.getSystemService(SensorManager::class.java)
        sm.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { s ->
            val e = r.aggiungi(Sensore("luce", "Luce ambiente", unita = "lx", deviceClass = "illuminance", soglia = 5f))
            ascolta(sm, s) { r.aggiorna(e, it[0]) }
        }
        sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { s ->
            val e = r.aggiungi(SensoreBinario("prossimita", "Prossimità", deviceClass = "occupancy"))
            ascolta(sm, s) { r.aggiorna(e, it[0] < s.maximumRange) }
        }
        torcia()
        r.aggiungi(Pulsante("riavvia_ha", "Riavvia Home Assistant", deviceClass = "restart", diagnostica = true) {
            // START_STICKY riapre il processo :ha, che riparte da capo (spec 0001, D4).
            h.postDelayed({ Process.killProcess(Process.myPid()) }, 500)
        })

        ctx.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = leggiBatteria(i)
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED), null, h)?.let { leggiBatteria(it) }
        h.post(object : Runnable {
            override fun run() {
                r.aggiorna(uptime, (SystemClock.elapsedRealtime() / 1000).toFloat())
                @Suppress("DEPRECATION")
                val w = ctx.getSystemService(WifiManager::class.java).connectionInfo
                if (w != null && w.rssi > -127) r.aggiorna(rssi, w.rssi.toFloat())
                h.postDelayed(this, 60_000)
            }
        })
    }

    private fun leggiBatteria(i: Intent) {
        val livello = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scala = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (livello >= 0) r.aggiorna(batteria, 100f * livello / scala)
        r.aggiorna(inCarica, i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
        val t = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        if (t != Int.MIN_VALUE) r.aggiorna(tempBatteria, t / 10f)
    }

    private fun ascolta(sm: SensorManager, s: Sensor, f: (FloatArray) -> Unit) {
        // Campionamento lento (1 s, consegna a lotti fino a 10 s): il registro limita comunque gli invii.
        sm.registerListener(object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) = f(e.values)
            override fun onAccuracyChanged(s: Sensor, a: Int) {}
        }, s, 1_000_000, 10_000_000, h)
    }

    private fun torcia() {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return
        lateinit var e: Interruttore
        e = r.aggiungi(Interruttore("torcia", "Torcia", icona = "mdi:flashlight") { acceso ->
            runCatching { cm.setTorchMode(id, acceso) }.onFailure { Log.w(ApiServer.TAG, "Torcia: ${it.message}"); r.aggiorna(e, !acceso) }
        })
        r.aggiorna(e, false)
        cm.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { if (cameraId == id) r.aggiorna(e, enabled) }
            override fun onTorchModeUnavailable(cameraId: String) {}
        }, h)
    }
}
