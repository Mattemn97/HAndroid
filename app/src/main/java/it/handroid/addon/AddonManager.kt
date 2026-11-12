package it.handroid.addon

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import it.handroid.PythonRuntime
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestore degli add-on, nel processo `:ha`. È l'API Kotlin che usano sia il server HTTP
 * locale (per l'integrazione in HA) sia le schermate native del supervisore.
 * Installare, avviare, fermare e rimuovere non toccano il processo di Home Assistant.
 */
object AddonManager {
    private const val TAG = "HAndroid"
    private lateinit var app: Context
    lateinit var stato: AddonStato
        private set
    /** Ultimo catalogo letto dai repository: slug -> (repository, manifest). */
    @Volatile private var catalogo: Map<String, Pair<String, Manifest>> = emptyMap()
    @Volatile var errori: List<String> = emptyList()
        private set

    fun dir(ctx: Context) = File(PythonRuntime.base(ctx), "addons")

    /** Da chiamare una volta all'avvio del supervisore: API locale e avvio automatico. */
    @Synchronized fun avvia(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        stato = AddonStato(dir(app))
        AddonApi.avvia(dir(app))
        Thread({
            // Dopo un aggiornamento dell'APK `:ha` estrae Python: gli add-on aspettano che finisca.
            Thread.sleep(30_000)
            runCatching { riprendi() }.onFailure { Log.e(TAG, "Avvio automatico add-on fallito", it) }
            runCatching { aggiornaCatalogo() }
        }, "addon-avvio").start()
    }

    /** Gli add-on girano in processi propri: sopravvivono a un riavvio di HA. Qui si riallinea. */
    private fun riprendi() {
        for (i in stato.installati()) {
            val slug = i.manifest.slug
            val slot = stato.slotDi(slug)
            if (slot != null && inEsecuzione(slug)) continue
            if (i.avvioAuto || slot != null) runCatching { avviaAddon(slug) }
                .onFailure { Log.w(TAG, "Add-on $slug non riavviato: ${it.message}") }
        }
    }

    fun aggiornaCatalogo(): Map<String, Pair<String, Manifest>> {
        val nuovo = LinkedHashMap<String, Pair<String, Manifest>>()
        val err = mutableListOf<String>()
        for (repo in stato.repository.toList()) {
            runCatching {
                val indice = Indice.parse(scarica("$repo/index.json"))
                for (slug in indice.addons) {
                    runCatching { nuovo.putIfAbsent(slug, repo to Manifest.parse(slug, scarica("$repo/$slug/config.yaml"))) }
                        .onFailure { err += "$repo/$slug: ${it.message}" }
                }
            }.onFailure { err += "$repo: ${it.message}" }
        }
        catalogo = nuovo
        errori = err
        return nuovo
    }

    /** Scarica manifest e file dal repository e registra l'add-on; i pacchetti pip li installa l'add-on al primo avvio. */
    fun installa(slug: String): Manifest {
        val (repo, _) = catalogo[slug] ?: aggiornaCatalogo()[slug] ?: error("Add-on $slug non trovato nei repository")
        val m = Manifest.parse(slug, scarica("$repo/$slug/config.yaml"))
        if (m.type == "native") requireNotNull(AddonService.binarioNativo(app.applicationInfo.nativeLibraryDir, m.entry)) {
            "Add-on nativo: lib${m.entry}.so non è in questo APK"
        }
        val d = stato.cartella(slug).apply { mkdirs() }
        for (f in m.files) File(d, f).apply { parentFile?.mkdirs() }.writeText(scarica("$repo/$slug/$f"))
        val inEsecuzione = inEsecuzione(slug)
        stato.installa(m, repo)
        // Versione nuova: pip reinstalla i requisiti al prossimo avvio.
        File(d, ".pip").delete()
        if (inEsecuzione) { fermaAddon(slug); avviaAddon(slug) }
        return m
    }

    fun avviaAddon(slug: String) {
        val i = stato.installato(slug) ?: error("$slug non installato")
        if (inEsecuzione(slug)) return
        val conflitti = stato.conflittoPorte(slug)
        require(conflitti.isEmpty()) { "Porte già usate da un altro add-on: $conflitti" }
        val slot = stato.assegnaSlot(slug) ?: error("Tutti i ${stato.slot} processi add-on sono occupati")
        File(stato.cartella(slug), "uscita.txt").delete()
        Log.i(TAG, "Avvio add-on ${i.manifest.name} nel processo :addon$slot")
        app.startForegroundService(Intent(app, AddonService.classe(slot)).putExtra(AddonService.EXTRA_SLUG, slug))
    }

    fun fermaAddon(slug: String) {
        val slot = stato.slotDi(slug) ?: return
        app.stopService(Intent(app, AddonService.classe(slot)))
        // Se il processo non risponde a stopService lo si chiude per pid.
        processo(slot)?.let { android.os.Process.killProcess(it.pid) }
        stato.liberaSlot(slug)
    }

    fun rimuovi(slug: String) {
        fermaAddon(slug)
        stato.rimuovi(slug)
    }

    fun impostaOpzioni(slug: String, opzioni: Map<String, Any?>, avvioAuto: Boolean?): List<String> =
        stato.impostaOpzioni(slug, opzioni, avvioAuto)

    fun inEsecuzione(slug: String): Boolean = stato.slotDi(slug)?.let { processo(it) } != null

    private fun processo(slot: Int): ActivityManager.RunningAppProcessInfo? =
        app.getSystemService(ActivityManager::class.java).runningAppProcesses
            ?.firstOrNull { it.processName == "${app.packageName}:addon$slot" }

    /** Elenco per l'API e per le schermate: catalogo più installati, con stato. */
    fun elenco(): JSONObject {
        val arr = JSONArray()
        val slugs = LinkedHashSet(catalogo.keys).apply { addAll(stato.installati().map { it.manifest.slug }) }
        for (slug in slugs) {
            val inst = stato.installato(slug)
            val m = inst?.manifest ?: catalogo[slug]!!.second
            val uscita = File(stato.cartella(slug), "uscita.txt").takeIf { it.exists() }?.readText()?.trim()
            arr.put(m.toJson()
                .put("repository", inst?.repo ?: catalogo[slug]?.first)
                .put("disponibile", catalogo[slug]?.second?.version)
                .put("installato", inst != null)
                .put("stato", when {
                    inst == null -> "non_installato"
                    inEsecuzione(slug) -> "avviato"
                    uscita != null && uscita != "0" -> "errore"
                    else -> "fermo"
                })
                .put("codice_uscita", uscita)
                .put("opzioni_attuali", inst?.let { JSONObject(it.opzioni) })
                .put("avvio_auto", inst?.avvioAuto ?: false)
                .put("processo", stato.slotDi(slug)?.let { ":addon$it" }))
        }
        return JSONObject().put("addons", arr).put("repository", JSONArray(stato.repository))
            .put("errori", JSONArray(errori))
    }

    /** Ultime righe del log dell'add-on. */
    fun log(slug: String, righe: Int = 200): String =
        File(stato.cartella(slug), "log.txt").takeIf { it.exists() }?.readLines()?.takeLast(righe)?.joinToString("\n") ?: ""

    private fun scarica(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        try {
            require(c.responseCode == 200) { "HTTP ${c.responseCode} da $url" }
            return c.inputStream.bufferedReader().readText()
        } finally {
            c.disconnect()
        }
    }
}
