package it.handroid

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.GZIPOutputStream

/**
 * Backup della cartella config nel formato di Home Assistant (spec 0006, F1):
 * un .tar con backup.json e homeassistant.tar.gz (la config sotto "data/"), salvato in
 * config/backups, dove anche HA lo vede e lo può ripristinare. Copia opzionale in una
 * cartella scelta dall'utente (Storage Access Framework).
 */
object Backup {
    private val ESCLUSI = setOf("backups", "tts", "deps", "__pycache__", ".cache")

    fun cartella(ctx: Context) = File(PythonRuntime.base(ctx), "config/backups")

    fun ultimo(ctx: Context): File? =
        cartella(ctx).listFiles { f -> f.name.endsWith(".tar") }?.maxByOrNull { it.lastModified() }

    /** Crea il backup, ruota gli automatici e lo copia nella cartella esterna. Restituisce il file. */
    @Synchronized
    fun esegui(ctx: Context, motivo: String): File {
        val config = File(PythonRuntime.base(ctx), "config")
        val dir = cartella(ctx).apply { mkdirs() }
        val nome = Regole.PREFISSO_BACKUP + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        val interno = File(ctx.cacheDir, "homeassistant.tar.gz")
        GZIPOutputStream(FileOutputStream(interno)).use { gz ->
            val tar = Tar(gz)
            config.walkTopDown()
                .onEnter { it == config || it.name !in ESCLUSI }
                // Il database SQLite si copia così com'è: -wal e -shm sono temporanei.
                .filter { it.isFile && !it.name.endsWith("-wal") && !it.name.endsWith("-shm") }
                .forEach { f -> tar.file("data/" + f.relativeTo(config).invariantSeparatorsPath, f) }
            tar.fine()
        }
        val tmp = File(dir, "$nome.tar.tmp")
        FileOutputStream(tmp).use { out ->
            val tar = Tar(out)
            tar.dati("./backup.json", descrizione(ctx, nome, motivo).toByteArray())
            tar.file("./homeassistant.tar.gz", interno)
            tar.fine()
        }
        interno.delete()
        val finale = File(dir, "$nome.tar")
        tmp.renameTo(finale)
        Regole.backupDaEliminare(dir.list().orEmpty().toList()).forEach { File(dir, it).delete() }
        runCatching { esporta(ctx, finale) }.onFailure { Supervisore.log(ctx, "Copia esterna fallita: ${it.message}") }
        Supervisore.log(ctx, "Backup $motivo: ${finale.name} (${finale.length() / 1024} KB)")
        return finale
    }

    private fun descrizione(ctx: Context, nome: String, motivo: String): String {
        val data = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).format(Date())
        val slug = UUID.randomUUID().toString().take(8)
        val ha = Supervisore.versioneHa(ctx) ?: "sconosciuta"
        return """{"slug":"$slug","name":"HAndroid $motivo $nome","date":"$data","type":"partial",""" +
            """"protected":false,"compressed":true,"version":2,"addons":[],"folders":[],""" +
            """"homeassistant":{"version":"$ha","exclude_database":false},""" +
            """"extra":{"instance_id":"handroid","with_automatic_settings":false}}"""
    }

    /** Copia nella cartella scelta con ACTION_OPEN_DOCUMENT_TREE, se c'è. */
    private fun esporta(ctx: Context, file: File) {
        val tree = Supervisore.cartellaEsterna(ctx) ?: return
        val r = ctx.contentResolver
        val padre = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val doc: Uri = DocumentsContract.createDocument(r, padre, "application/x-tar", file.name) ?: return
        r.openOutputStream(doc)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
    }
}

/** Scrittore tar minimo (ustar + intestazioni PAX per i nomi lunghi): niente librerie esterne. */
class Tar(private val out: OutputStream) {
    fun file(nome: String, f: File) {
        intestazione(nome, f.length(), f.lastModified() / 1000)
        f.inputStream().use { it.copyTo(out) }
        riempi(f.length())
    }

    fun dati(nome: String, b: ByteArray) {
        intestazione(nome, b.size.toLong(), System.currentTimeMillis() / 1000)
        out.write(b)
        riempi(b.size.toLong())
    }

    fun fine() {
        out.write(ByteArray(1024))
        out.flush()
    }

    private fun intestazione(nome: String, size: Long, mtime: Long, tipo: Char = '0') {
        val n = nome.toByteArray()
        if (n.size > 100 && tipo == '0') {
            val rec = pax("path", nome)
            intestazione("PaxHeader", rec.size.toLong(), mtime, 'x')
            out.write(rec)
            riempi(rec.size.toLong())
        }
        val h = ByteArray(512)
        n.copyInto(h, 0, 0, minOf(n.size, 100))
        ottale(h, 100, 8, 0b110_100_100)
        ottale(h, 108, 8, 0)
        ottale(h, 116, 8, 0)
        ottale(h, 124, 12, size)
        ottale(h, 136, 12, mtime)
        h[156] = tipo.code.toByte()
        "ustar\u000000".toByteArray().copyInto(h, 257)
        for (i in 148 until 156) h[i] = ' '.code.toByte()
        val somma = h.sumOf { it.toInt() and 0xff }
        ottale(h, 148, 7, somma.toLong())
        out.write(h)
    }

    private fun pax(chiave: String, valore: String): ByteArray {
        val corpo = " $chiave=$valore\n".toByteArray().size
        var len = corpo + 1
        while ("$len".length + corpo != len) len = "$len".length + corpo
        return "$len $chiave=$valore\n".toByteArray()
    }

    private fun ottale(h: ByteArray, pos: Int, len: Int, v: Long) {
        val s = java.lang.Long.toOctalString(v).padStart(len - 1, '0')
        s.toByteArray().copyInto(h, pos)
        h[pos + len - 1] = 0
    }

    private fun riempi(size: Long) {
        val resto = (512 - size % 512) % 512
        if (resto > 0) out.write(ByteArray(resto.toInt()))
    }
}
