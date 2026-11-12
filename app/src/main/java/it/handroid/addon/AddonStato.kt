package it.handroid.addon

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stato persistente degli add-on in `<dir>/stato.json`: installati, opzioni, repository
 * e quale processo del pool (`:addon1`…`:addonN`) usa ciascuno. Solo logica, niente
 * Android: si prova con i test JVM. Lo scrive soltanto il processo `:ha`.
 */
class AddonStato(private val dir: File, val slot: Int = SLOT) {
    data class Installato(val manifest: Manifest, val repo: String, val opzioni: Map<String, Any?>, val avvioAuto: Boolean)

    private val file = File(dir, "stato.json")
    private val installati = LinkedHashMap<String, Installato>()
    private val slotDi = HashMap<String, Int>()
    val repository = mutableListOf(REPO_UFFICIALE)

    init {
        if (file.exists()) carica(JSONObject(file.readText()))
    }

    @Synchronized fun installati(): List<Installato> = installati.values.toList()
    @Synchronized fun installato(slug: String) = installati[slug]
    @Synchronized fun slotDi(slug: String) = slotDi[slug]
    @Synchronized fun slugNelloSlot(n: Int) = slotDi.entries.firstOrNull { it.value == n }?.key

    /** Cartella dell'add-on: manifest.json, opzioni.json, file scaricati e `site/` per pip. */
    fun cartella(slug: String) = File(dir, slug)

    @Synchronized fun installa(m: Manifest, repo: String) {
        val prima = installati[m.slug]
        // Un aggiornamento tiene le opzioni dell'utente ancora valide per il nuovo schema.
        val opzioni = prima?.opzioni?.takeIf { Opzioni.valida(it, m.schema).isEmpty() } ?: m.options
        installati[m.slug] = Installato(m, repo, opzioni, prima?.avvioAuto ?: (m.boot == "auto"))
        scriviCartella(m.slug)
        salva()
    }

    @Synchronized fun rimuovi(slug: String): Boolean {
        val c = installati.remove(slug) ?: return false
        slotDi.remove(slug)
        cartella(c.manifest.slug).deleteRecursively()
        salva()
        return true
    }

    /** Cambia le opzioni; restituisce gli errori di validazione (nulla cambia se ce ne sono). */
    @Synchronized fun impostaOpzioni(slug: String, opzioni: Map<String, Any?>, avvioAuto: Boolean? = null): List<String> {
        val c = installati[slug] ?: return listOf("$slug non installato")
        val errori = Opzioni.valida(opzioni, c.manifest.schema)
        if (errori.isNotEmpty()) return errori
        installati[slug] = c.copy(opzioni = opzioni, avvioAuto = avvioAuto ?: c.avvioAuto)
        scriviCartella(slug)
        salva()
        return emptyList()
    }

    /** Assegna un processo libero del pool (o quello che ha già); null se sono tutti occupati. */
    @Synchronized fun assegnaSlot(slug: String): Int? {
        require(slug in installati) { "$slug non installato" }
        slotDi[slug]?.let { return it }
        val n = (1..slot).firstOrNull { it !in slotDi.values } ?: return null
        slotDi[slug] = n
        salva()
        return n
    }

    @Synchronized fun liberaSlot(slug: String) {
        if (slotDi.remove(slug) != null) salva()
    }

    @Synchronized fun aggiungiRepository(url: String) {
        require(url.startsWith("https://")) { "Il repository deve essere https" }
        val u = url.trimEnd('/')
        if (u !in repository) { repository += u; salva() }
    }

    @Synchronized fun rimuoviRepository(url: String) {
        require(url != REPO_UFFICIALE) { "Il repository ufficiale non si rimuove" }
        if (repository.remove(url.trimEnd('/'))) salva()
    }

    /** Porte già usate da altri add-on installati: due add-on sulla stessa porta non partono insieme. */
    @Synchronized fun conflittoPorte(slug: String): List<Int> {
        val mie = installati[slug]?.manifest?.ports ?: return emptyList()
        return slotDi.keys.filter { it != slug }.flatMap { installati[it]?.manifest?.ports.orEmpty() }.filter { it in mie }
    }

    private fun scriviCartella(slug: String) {
        val c = installati[slug] ?: return
        val d = cartella(slug).apply { mkdirs() }
        File(d, "manifest.json").writeText(c.manifest.toJson().toString(2))
        File(d, "opzioni.json").writeText(JSONObject(c.opzioni).toString(2))
    }

    private fun salva() {
        dir.mkdirs()
        val inst = JSONObject()
        installati.values.forEach {
            inst.put(it.manifest.slug, JSONObject().put("manifest", it.manifest.toJson()).put("repo", it.repo)
                .put("opzioni", JSONObject(it.opzioni)).put("avvio_auto", it.avvioAuto))
        }
        val o = JSONObject().put("installati", inst).put("slot", JSONObject(slotDi as Map<*, *>))
            .put("repository", JSONArray(repository))
        val tmp = File(dir, "stato.json.tmp")
        tmp.writeText(o.toString(2))
        tmp.renameTo(file)
    }

    private fun carica(o: JSONObject) {
        val inst = o.optJSONObject("installati") ?: JSONObject()
        for (k in inst.keys()) {
            val e = inst.getJSONObject(k)
            installati[k] = Installato(Manifest.fromJson(e.getJSONObject("manifest")), e.optString("repo"),
                Json.mappa(e.optJSONObject("opzioni") ?: JSONObject()), e.optBoolean("avvio_auto"))
        }
        o.optJSONObject("slot")?.let { s -> s.keys().forEach { if (it in installati) slotDi[it] = s.getInt(it) } }
        o.optJSONArray("repository")?.let { r ->
            repository.clear()
            repository += REPO_UFFICIALE
            (0 until r.length()).map { r.getString(it) }.filter { it != REPO_UFFICIALE }.forEach { repository += it }
        }
    }

    companion object {
        /** Processi del pool dichiarati in AndroidManifest.xml (AddonService1..3). */
        const val SLOT = 3
        const val REPO_UFFICIALE = "https://raw.githubusercontent.com/Mattemn97/HAndroid/master/addons"
    }
}
