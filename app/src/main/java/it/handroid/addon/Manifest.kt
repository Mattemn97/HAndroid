package it.handroid.addon

import org.json.JSONArray
import org.json.JSONObject

/** Manifest di un add-on (config.yaml), formato di docs/specs/0007-addon.md. */
data class Manifest(
    val slug: String,
    val name: String,
    val version: String,
    val type: String,               // python | native
    val description: String = "",
    val ports: List<Int> = emptyList(),
    val permissions: List<String> = emptyList(),
    val options: Map<String, Any?> = emptyMap(),
    val schema: Map<String, String> = emptyMap(),
    val boot: String = "manual",    // auto | manual
    val pip: List<String> = emptyList(),    // requisiti pip, solo wheel
    val files: List<String> = emptyList(),  // file scaricati accanto al manifest
    val entry: String = "",         // python: file .py o modulo; native: nome del binario
    val args: List<String> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("slug", slug).put("name", name).put("version", version).put("type", type)
        .put("description", description).put("ports", JSONArray(ports))
        .put("permissions", JSONArray(permissions)).put("options", JSONObject(options))
        .put("schema", JSONObject(schema)).put("boot", boot).put("pip", JSONArray(pip))
        .put("files", JSONArray(files)).put("entry", entry).put("args", JSONArray(args))

    companion object {
        val TIPI = setOf("python", "native")
        val PERMESSI = setOf("network", "bluetooth", "usb", "storage")
        private val SLUG = Regex("[a-z0-9_]{1,32}")
        private val FILE = Regex("[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*")

        /** Legge il manifest YAML; lancia IllegalArgumentException con un messaggio leggibile. */
        fun parse(slug: String, yaml: String): Manifest = da(slug, MiniYaml.parse(yaml))

        fun da(slug: String, m: Map<String, Any?>): Manifest {
            require(SLUG.matches(slug)) { "Slug non valido: $slug" }
            fun str(k: String, def: String? = null) = m[k]?.toString() ?: def ?: throw IllegalArgumentException("Manca '$k'")
            fun lista(k: String): List<Any?> = when (val v = m[k]) {
                null -> emptyList(); is List<*> -> v; else -> listOf(v)
            }
            @Suppress("UNCHECKED_CAST")
            fun mappa(k: String) = (m[k] as? Map<String, Any?>) ?: emptyMap()

            val tipo = str("type")
            require(tipo in TIPI) { "Tipo non valido: $tipo" }
            val porte = lista("ports").map { (it as? Number)?.toInt() ?: it.toString().toIntOrNull() ?: -1 }
            require(porte.all { it in 1..65535 }) { "Porta non valida" }
            val permessi = lista("permissions").map { it.toString() }
            require(permessi.all { it in PERMESSI }) { "Permesso sconosciuto: $permessi" }
            val files = lista("files").map { it.toString() }
            require(files.all { FILE.matches(it) && ".." !in it }) { "Nome file non valido: $files" }
            val schema = mappa("schema").mapValues { it.value.toString() }
            val entry = str("entry", "")
            if (tipo == "native") require(Regex("[a-z0-9_]+").matches(entry)) { "Binario nativo non valido: $entry" }
            else require(entry.isNotEmpty()) { "Manca 'entry'" }
            val opzioni = mappa("options")
            val errori = Opzioni.valida(opzioni, schema)
            require(errori.isEmpty()) { "Opzioni di default non valide: $errori" }
            return Manifest(
                slug = slug, name = str("name"), version = str("version"), type = tipo,
                description = str("description", ""), ports = porte, permissions = permessi,
                options = opzioni, schema = schema, boot = str("boot", "manual"),
                pip = lista("pip").map { it.toString() }, files = files, entry = entry,
                args = lista("args").map { it.toString() },
            )
        }

        fun fromJson(o: JSONObject): Manifest = da(o.getString("slug"), Json.mappa(o))
    }
}

/** Indice di un repository: index.json con l'elenco degli slug (ognuno ha <slug>/config.yaml). */
data class Indice(val name: String, val addons: List<String>) {
    companion object {
        fun parse(json: String): Indice {
            val o = JSONObject(json)
            val a = o.getJSONArray("addons")
            val slug = (0 until a.length()).map {
                val e = a.get(it)
                if (e is JSONObject) e.getString("slug") else e.toString()
            }
            return Indice(o.optString("name", "Repository"), slug)
        }
    }
}

/** Validazione delle opzioni contro lo schema, con i tipi degli add-on di HA OS. */
object Opzioni {
    private val LISTA = Regex("list\\((.+)\\)")
    private val INTERVALLO = Regex("(int|float)\\((-?[0-9.]*),(-?[0-9.]*)\\)")

    /** Restituisce gli errori (vuota se va bene). Un tipo con `?` finale è facoltativo. */
    fun valida(opzioni: Map<String, Any?>, schema: Map<String, String>): List<String> {
        val errori = mutableListOf<String>()
        for ((k, t0) in schema) {
            val facoltativo = t0.endsWith("?")
            val t = t0.removeSuffix("?")
            val v = opzioni[k]
            if (v == null) {
                if (!facoltativo) errori += "$k: obbligatorio"
                continue
            }
            if (!tipoOk(v, t)) errori += "$k: atteso $t"
        }
        opzioni.keys.filter { it !in schema }.forEach { errori += "$it: non previsto dallo schema" }
        return errori
    }

    private fun tipoOk(v: Any, t: String): Boolean {
        INTERVALLO.matchEntire(t)?.let { m ->
            val n = (v as? Number)?.toDouble() ?: return false
            if (m.groupValues[1] == "int" && n % 1.0 != 0.0) return false
            val min = m.groupValues[2].toDoubleOrNull()
            val max = m.groupValues[3].toDoubleOrNull()
            return (min == null || n >= min) && (max == null || n <= max)
        }
        LISTA.matchEntire(t)?.let { return v.toString() in it.groupValues[1].split("|") }
        return when (t) {
            "bool" -> v is Boolean
            "int" -> v is Number && v.toDouble() % 1.0 == 0.0
            "float" -> v is Number
            "str", "password", "email", "url" -> v is String
            "port" -> v is Number && v.toInt() in 1..65535
            else -> false
        }
    }
}

/** Conversioni tra org.json e mappe Kotlin. */
object Json {
    fun mappa(o: JSONObject): Map<String, Any?> = o.keys().asSequence().associateWith { valore(o.get(it)) }
    fun valore(v: Any?): Any? = when (v) {
        JSONObject.NULL -> null
        is JSONObject -> mappa(v)
        is JSONArray -> (0 until v.length()).map { valore(v.get(it)) }
        else -> v
    }
}

/**
 * Sottoinsieme di YAML che basta ai manifest: mappe annidate per indentazione, liste
 * `[a, b]` o `- a`, stringhe (anche tra virgolette), numeri, booleani, commenti `#`.
 * Niente librerie: l'app non ha dipendenze.
 */
object MiniYaml {
    private class Riga(val ind: Int, val testo: String, val n: Int)

    fun parse(yaml: String): Map<String, Any?> {
        val righe = yaml.lines().mapIndexedNotNull { i, r ->
            val t = togliCommento(r).trimEnd()
            if (t.isBlank()) null else Riga(t.length - t.trimStart().length, t.trim(), i + 1)
        }
        val pos = intArrayOf(0)
        @Suppress("UNCHECKED_CAST")
        return (blocco(righe, pos, 0) as? Map<String, Any?>) ?: emptyMap()
    }

    private fun blocco(righe: List<Riga>, pos: IntArray, ind: Int): Any? {
        if (pos[0] >= righe.size) return null
        return if (righe[pos[0]].testo.startsWith("- ") || righe[pos[0]].testo == "-") lista(righe, pos, righe[pos[0]].ind)
        else mappa(righe, pos, righe[pos[0]].ind.coerceAtLeast(ind))
    }

    private fun mappa(righe: List<Riga>, pos: IntArray, ind: Int): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        while (pos[0] < righe.size && righe[pos[0]].ind == ind) {
            val r = righe[pos[0]]
            val i = r.testo.indexOf(':')
            require(i > 0) { "Riga ${r.n}: attesa 'chiave: valore'" }
            val k = scalare(r.testo.substring(0, i).trim()).toString()
            val resto = r.testo.substring(i + 1).trim()
            pos[0]++
            m[k] = if (resto.isNotEmpty()) valore(resto)
            else if (pos[0] < righe.size && righe[pos[0]].ind > ind) blocco(righe, pos, righe[pos[0]].ind)
            else if (pos[0] < righe.size && righe[pos[0]].ind == ind && righe[pos[0]].testo.startsWith("-")) lista(righe, pos, ind)
            else null
        }
        require(pos[0] >= righe.size || righe[pos[0]].ind < ind) { "Riga ${righe[pos[0]].n}: indentazione inattesa" }
        return m
    }

    private fun lista(righe: List<Riga>, pos: IntArray, ind: Int): List<Any?> {
        val l = mutableListOf<Any?>()
        while (pos[0] < righe.size && righe[pos[0]].ind == ind && righe[pos[0]].testo.startsWith("-")) {
            l += valore(righe[pos[0]].testo.removePrefix("-").trim())
            pos[0]++
        }
        return l
    }

    private fun valore(s: String): Any? =
        if (s.startsWith("[") && s.endsWith("]")) {
            val dentro = s.substring(1, s.length - 1).trim()
            if (dentro.isEmpty()) emptyList() else dividi(dentro).map { scalare(it.trim()) }
        } else if (s == "{}") emptyMap<String, Any?>() else scalare(s)

    private fun scalare(s: String): Any? = when {
        s.length >= 2 && (s[0] == '"' && s.last() == '"' || s[0] == '\'' && s.last() == '\'') -> s.substring(1, s.length - 1)
        s == "true" || s == "True" -> true
        s == "false" || s == "False" -> false
        s == "null" || s == "~" || s.isEmpty() -> null
        s.matches(Regex("-?[0-9]+")) -> s.toLongOrNull()?.let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
        s.matches(Regex("-?[0-9]+\\.[0-9]+")) -> s.toDouble()
        else -> s
    }

    /** Divide per virgole fuori dalle virgolette. */
    private fun dividi(s: String): List<String> {
        val out = mutableListOf<String>()
        var q: Char? = null
        val cur = StringBuilder()
        for (c in s) {
            if (q != null) { if (c == q) q = null; cur.append(c) }
            else if (c == '"' || c == '\'') { q = c; cur.append(c) }
            else if (c == ',') { out += cur.toString(); cur.clear() }
            else cur.append(c)
        }
        out += cur.toString()
        return out
    }

    /** Toglie `# commento` fuori dalle virgolette (un # dentro una parola resta). */
    private fun togliCommento(r: String): String {
        var q: Char? = null
        for ((i, c) in r.withIndex()) {
            if (q != null) { if (c == q) q = null }
            else if (c == '"' || c == '\'') q = c
            else if (c == '#' && (i == 0 || r[i - 1] == ' ')) return r.substring(0, i)
        }
        return r
    }
}
