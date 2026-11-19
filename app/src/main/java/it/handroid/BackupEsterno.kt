package it.handroid

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import java.io.File

/**
 * Backup fuori dal telefono (spec 0010): l'ultimo backup di HA si copia in una cartella scelta dall'utente
 * con il selettore di Android (Drive, cartella di rete, chiavetta…). Si tengono gli ultimi [TIENI].
 * La cartella è un URI con permesso persistente, salvato in files/: lo usa anche il processo :ha.
 */
object BackupEsterno {
    private const val TAG = "HAndroid"
    const val TIENI = 7

    private fun cartellaFile(context: Context) = File(context.filesDir, "backup-cartella.txt")
    private fun copiatoFile(context: Context) = File(context.filesDir, "backup-copiato.txt")
    private fun backups(context: Context) = File(context.filesDir, "ha-config/backups")

    fun cartella(context: Context): Uri? = cartellaFile(context).takeIf { it.exists() }?.readText()?.let(Uri::parse)

    fun salvaCartella(context: Context, albero: Uri?) {
        if (albero == null) cartellaFile(context).delete() else cartellaFile(context).writeText(albero.toString())
        copiatoFile(context).delete()
    }

    fun stato(context: Context): String {
        val c = cartella(context) ?: return "spenta"
        val ultimo = copiatoFile(context).takeIf { it.exists() }?.readText()
        return "${c.lastPathSegment?.substringAfter(':') ?: c}; ultimo copiato: ${ultimo ?: "nessuno"}"
    }

    /** Copia l'ultimo backup se non è già stato copiato. Restituisce il nome copiato, o null. */
    fun copia(context: Context): String? {
        val albero = cartella(context) ?: return null
        val ultimo = backups(context).listFiles { f -> f.name.endsWith(".tar") }?.maxByOrNull { it.lastModified() } ?: return null
        if (copiatoFile(context).takeIf { it.exists() }?.readText() == ultimo.name) return null
        val cr = context.contentResolver
        val padre = DocumentsContract.buildDocumentUriUsingTree(albero, DocumentsContract.getTreeDocumentId(albero))
        val nuovo = DocumentsContract.createDocument(cr, padre, "application/x-tar", ultimo.name)
            ?: error("la cartella non accetta file nuovi")
        cr.openOutputStream(nuovo)!!.use { out -> ultimo.inputStream().use { it.copyTo(out) } }
        copiatoFile(context).writeText(ultimo.name)
        Log.i(TAG, "Backup copiato fuori dal telefono: ${ultimo.name}")
        sfoltisci(context, albero)
        return ultimo.name
    }

    /** Cancella i backup più vecchi nella cartella, oltre gli ultimi [TIENI]. Tocca solo i file .tar. */
    private fun sfoltisci(context: Context, albero: Uri) {
        val cr = context.contentResolver
        val figli = DocumentsContract.buildChildDocumentsUriUsingTree(albero, DocumentsContract.getTreeDocumentId(albero))
        val trovati = mutableListOf<Triple<String, String, Long>>()
        cr.query(figli, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { c ->
            while (c.moveToNext()) trovati += Triple(c.getString(0), c.getString(1) ?: "", c.getLong(2))
        }
        for (id in daCancellare(trovati.map { Triple(it.first, it.second, it.third) }, TIENI)) {
            runCatching { DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(albero, id)) }
        }
    }

    /** Id dei backup (.tar) da cancellare: tutti tranne i [tieni] più recenti. (id, nome, data) */
    fun daCancellare(file: List<Triple<String, String, Long>>, tieni: Int) =
        file.filter { it.second.endsWith(".tar") }.sortedByDescending { it.third }.drop(tieni).map { it.first }

    /** "Ripristina da file": porta un backup scelto dall'utente tra quelli di HA. Restituisce il nome. */
    fun importa(context: Context, file: Uri): String {
        val cr = context.contentResolver
        val nome = cr.query(file, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }?.substringAfterLast('/')?.takeIf { it.endsWith(".tar") } ?: "ripristino-${System.currentTimeMillis()}.tar"
        val dest = File(backups(context).apply { mkdirs() }, nome)
        cr.openInputStream(file)!!.use { inp -> dest.outputStream().use { inp.copyTo(it) } }
        return nome
    }
}
