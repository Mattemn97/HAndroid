package it.handroid

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import java.io.File

/**
 * Regole del supervisor, condivise tra il processo dell'app e quello di HA (":ha").
 * Lo stato sta in file e non in SharedPreferences, che tra processi diversi non si aggiornano.
 */
object Supervisor {
    private const val TAG = "HAndroid"

    /** Codice con cui HA esce quando chiedi "Riavvia" dall'interfaccia. */
    const val RESTART_EXIT_CODE = 100
    const val SAFETY_NET_MS = 120_000L

    /** Crash entro questa finestra che fanno partire HA in modalità di ripristino. */
    const val MAX_CRASHES = 3
    private const val CRASH_WINDOW_MS = 15 * 60_000L

    /** Dopo quanto tempo di funzionamento regolare il conteggio dei crash si azzera. */
    const val STABLE_AFTER_MS = 10 * 60_000L

    private fun autostartFile(context: Context) = File(context.filesDir, "avvio-automatico")
    private fun crashFile(context: Context) = File(context.filesDir, "crash.txt")
    private fun runningFile(context: Context) = File(context.filesDir, "in-corso")

    /** Segna che HA sta girando. Restituisce true se il giro precedente non si era chiuso (processo ucciso). */
    fun markRunning(context: Context): Boolean = !runningFile(context).createNewFile()

    fun clearRunning(context: Context) {
        runningFile(context).delete()
    }

    /** Avvio automatico al boot e dopo un aggiornamento: attivo da quando HA è stato avviato una volta. */
    fun autostart(context: Context) = autostartFile(context).exists()

    fun setAutostart(context: Context, enabled: Boolean) {
        if (enabled) autostartFile(context).writeText("1") else autostartFile(context).delete()
    }

    /** Registra un crash e dice quanti ce ne sono stati nella finestra recente. */
    fun recordCrash(context: Context): Int {
        val now = System.currentTimeMillis()
        val recent = crashTimes(context).filter { now - it < CRASH_WINDOW_MS } + now
        crashFile(context).writeText(recent.joinToString("\n"))
        return recent.size
    }

    fun resetCrashes(context: Context) {
        crashFile(context).delete()
    }

    /** Troppi crash di fila: HA parte con --recovery-mode (solo le integrazioni essenziali). */
    fun recoveryMode(context: Context): Boolean {
        val now = System.currentTimeMillis()
        return crashTimes(context).count { now - it < CRASH_WINDOW_MS } >= MAX_CRASHES
    }

    private fun crashTimes(context: Context): List<Long> =
        crashFile(context).takeIf { it.exists() }?.readLines()?.mapNotNull { it.toLongOrNull() }.orEmpty()

    /**
     * Fa ripartire il servizio tra [delayMs]. Serve perché HA gira in un processo che a ogni giro
     * deve morire (Python non si reinizializza): la sveglia lo riavvia anche se il sistema non lo fa.
     */
    fun scheduleRestart(context: Context, delayMs: Long, quiet: Boolean = false) {
        if (!quiet) Log.i(TAG, "Riavvio di Home Assistant tra ${delayMs / 1000}s")
        val pending = restartIntent(context)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val at = SystemClock.elapsedRealtime() + delayMs
        // Da Android 12 un servizio in primo piano parte da una sveglia solo se è esatta, oppure se l'app
        // è esclusa dall'ottimizzazione batteria (lo chiede MainActivity)
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending)
        }
    }

    /**
     * Rete di sicurezza: finché HA è vivo il watchdog sposta in avanti questa sveglia. Se il processo
     * muore di colpo (kill del sistema, crash nativo) nessuno chiama scheduleRestart, e MIUI ignora
     * START_STICKY: la sveglia scade e fa ripartire il servizio. Se scatta con HA vivo non fa nulla.
     */
    fun scheduleSafetyNet(context: Context) = scheduleRestart(context, SAFETY_NET_MS, quiet = true)

    fun cancelRestart(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(restartIntent(context))
    }

    private fun restartIntent(context: Context): PendingIntent {
        val intent = Intent(context, HaService::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= 26) PendingIntent.getForegroundService(context, 0, intent, flags)
        else PendingIntent.getService(context, 0, intent, flags)
    }

    /** L'app è esclusa dall'ottimizzazione batteria: Android non la ferma e può riavviarla da dietro le quinte. */
    fun batteryExempt(context: Context) =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
}
