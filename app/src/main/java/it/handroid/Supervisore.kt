package it.handroid

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Process
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

/**
 * Supervisore della M1 (spec 0006): backup notturno, aggiornamento di HA con rollback,
 * riavvio. Lo stato condiviso tra i processi sta in file nella cartella base, non nelle
 * SharedPreferences (che ogni processo tiene in cache).
 */
object Supervisore {
    const val BACKUP = "it.handroid.BACKUP"
    const val AGGIORNA_HA = "it.handroid.AGGIORNA_HA"
    const val RIAVVIA = "it.handroid.RIAVVIA"
    const val EXTRA_VERSIONE = "versione"
    const val EXTRA_MOTIVO = "motivo"
    private const val ORA_BACKUP = 3
    private const val MINUTO_BACKUP = 30
    const val RELEASES = "https://github.com/Mattemn97/HAndroid/releases/latest"

    private fun base(ctx: Context) = PythonRuntime.base(ctx)

    fun log(ctx: Context, msg: String) {
        Log.i(HaService.TAG, msg)
        runCatching {
            val f = File(base(ctx), "supervisore.log")
            // Tiene il registro corto: l'ultima metà quando supera i 64 KB.
            if (f.length() > 64 * 1024) f.writeText(f.readText().takeLast(32 * 1024))
            val ora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
            f.appendText("$ora $msg\n")
        }
    }

    fun registro(ctx: Context): String =
        File(base(ctx), "supervisore.log").takeIf { it.exists() }?.readText().orEmpty()

    fun versioneHa(ctx: Context): String? =
        File(base(ctx), "versione_ha").takeIf { it.exists() }?.readText()?.trim()

    fun cartellaEsterna(ctx: Context): Uri? =
        File(base(ctx), "cartella_esterna").takeIf { it.exists() }?.readText()?.let(Uri::parse)

    fun impostaCartellaEsterna(ctx: Context, uri: Uri) = File(base(ctx), "cartella_esterna").writeText(uri.toString())

    /** Allarme inesatto ma che scatta anche in Doze; si riarma a ogni esecuzione e a ogni avvio del servizio. */
    fun pianifica(ctx: Context) {
        val quando = Regole.prossima(System.currentTimeMillis(), ORA_BACKUP, MINUTO_BACKUP)
        ctx.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, azione(ctx, BACKUP) { putExtra(EXTRA_MOTIVO, "notturno") })
    }

    fun azione(ctx: Context, action: String, extra: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, action.hashCode(),
            Intent(action).setClass(ctx, SupervisoreReceiver::class.java).apply(extra),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun invia(ctx: Context, action: String, extra: Intent.() -> Unit = {}) =
        ctx.sendBroadcast(Intent(action).setClass(ctx, SupervisoreReceiver::class.java).apply(extra))

    /**
     * Chiamato da HaService all'avvio. Restituisce true se serve la modalità provvisoria.
     * Con un aggiornamento in prova che continua a fallire torna alla versione di prima.
     */
    fun alAvvio(ctx: Context, prefs: SharedPreferences, falliti: Int, max: Int): Boolean {
        pianifica(ctx)
        val prova = File(base(ctx), "ha.prova")
        return when (Regole.decidiAvvio(prova.exists(), falliti, max)) {
            Regole.Avvio.NORMALE -> false
            Regole.Avvio.PROVVISORIA -> true
            Regole.Avvio.ROLLBACK -> {
                val ha = File(base(ctx), "ha")
                val prev = File(base(ctx), "ha.prev")
                ha.deleteRecursively()
                if (prev.exists()) prev.renameTo(ha)
                prova.delete()
                prefs.edit().putInt("avvii_falliti", 1).apply()
                log(ctx, "Aggiornamento di HA non partito dopo $falliti tentativi: rollback alla versione precedente")
                false
            }
        }
    }

    /** Il watchdog ha visto HA rispondere: l'eventuale aggiornamento in prova è confermato. */
    fun sano(ctx: Context) {
        val prova = File(base(ctx), "ha.prova")
        if (!prova.exists()) return
        prova.delete()
        File(base(ctx), "ha.prev").deleteRecursively()
        log(ctx, "Home Assistant ${versioneHa(ctx) ?: ""} confermato dopo l'aggiornamento")
    }

    /** Backup, poi chiede a handroid_main.py di installare [versione] al prossimo avvio, e riavvia. */
    fun aggiornaHa(ctx: Context, versione: String) {
        Backup.esegui(ctx, "pre-aggiornamento")
        File(base(ctx), "aggiorna_ha").writeText(versione)
        log(ctx, "Aggiornamento a Home Assistant $versione richiesto, riavvio")
        riavvia()
    }

    /** Chiude il processo :ha: Android riavvia il servizio (START_STICKY), come fa il watchdog. */
    fun riavvia() = Process.killProcess(Process.myPid())

    fun ultimaHaPypi(): String? = runCatching {
        JSONObject(scarica("https://pypi.org/pypi/homeassistant/json")).getJSONObject("info").getString("version")
    }.getOrNull()

    fun ultimoApk(): String? = runCatching {
        JSONObject(scarica("https://api.github.com/repos/Mattemn97/HAndroid/releases/latest")).getString("tag_name")
    }.getOrNull()

    private fun scarica(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 15_000
        c.setRequestProperty("Accept", "application/json")
        return c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() }
    }
}

/** Gira nel processo :ha, tenuto vivo dal servizio in primo piano: il lavoro va su un thread. */
class SupervisoreReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val app = ctx.applicationContext
        Thread({
            runCatching {
                when (intent.action) {
                    Supervisore.BACKUP -> {
                        Supervisore.pianifica(app)
                        Backup.esegui(app, intent.getStringExtra(Supervisore.EXTRA_MOTIVO) ?: "manuale")
                    }
                    Supervisore.AGGIORNA_HA -> intent.getStringExtra(Supervisore.EXTRA_VERSIONE)?.let { Supervisore.aggiornaHa(app, it) }
                    Supervisore.RIAVVIA -> {
                        Supervisore.log(app, "Riavvio richiesto dall'utente")
                        Supervisore.riavvia()
                    }
                }
            }.onFailure { Supervisore.log(app, "Errore in ${intent.action}: ${it.message}") }
        }, "supervisore").start()
    }
}
