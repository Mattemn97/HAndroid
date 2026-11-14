package it.handroid.addon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AddonTest {
    private val repo = File("../addons")

    @Test fun manifestUfficialeValido() {
        val m = Manifest.parse("mqtt", File(repo, "mqtt/config.yaml").readText())
        assertEquals("python", m.type)
        assertEquals(listOf(1883), m.ports)
        assertEquals(listOf("broker.py"), m.files)
        assertEquals("broker.py", m.entry)
        assertEquals(1883, m.options["port"])
        assertEquals("", m.options["username"])
        assertEquals("password?", m.schema["password"])
        assertEquals("auto", m.boot)
        assertTrue(m.pip.isEmpty())
    }

    @Test fun indiceUfficiale() {
        val i = Indice.parse(File(repo, "index.json").readText())
        assertTrue("mqtt" in i.addons)
        i.addons.forEach { assertTrue("manca $it/config.yaml", File(repo, "$it/config.yaml").isFile) }
        assertEquals(listOf("a", "b"), Indice.parse("""{"addons":["a",{"slug":"b"}]}""").addons)
    }

    @Test fun yamlAnnidatoEListe() {
        val y = MiniYaml.parse("""
            # commento
            name: "Prova: due punti"
            version: '1.0'
            ports:
              - 80
              - 443
            options:
              livello: info   # commento in coda
              colore: "#fff"
              vuota: []
            x: 1.5
        """.trimIndent())
        assertEquals("Prova: due punti", y["name"])
        assertEquals("1.0", y["version"])
        assertEquals(listOf(80, 443), y["ports"])
        assertEquals(mapOf("livello" to "info", "colore" to "#fff", "vuota" to emptyList<Any>()), y["options"])
        assertEquals(1.5, y["x"])
    }

    @Test fun manifestNonValidi() {
        val base = "name: A\nversion: 1\nentry: a.py\n"
        fun errore(yaml: String, slug: String = "a") = runCatching { Manifest.parse(slug, yaml) }.exceptionOrNull()?.message
        assertTrue(errore(base + "type: docker")!!.contains("Tipo"))
        assertTrue(errore(base + "type: python\nports: [70000]")!!.contains("Porta"))
        assertTrue(errore(base + "type: python\npermissions: [root]")!!.contains("Permesso"))
        assertTrue(errore(base + "type: python\nfiles: [../x.py]")!!.contains("file"))
        assertTrue(errore(base + "type: python", slug = "A B")!!.contains("Slug"))
        assertTrue(errore("name: A\nversion: 1\ntype: native\nentry: ../sh")!!.contains("nativo"))
        assertTrue(errore(base + "type: python\noptions:\n  p: x\nschema:\n  p: int")!!.contains("Opzioni"))
        assertNull(errore(base + "type: python"))
    }

    @Test fun validazioneOpzioni() {
        val s = mapOf("p" to "port", "b" to "bool", "l" to "list(debug|info)", "n" to "int(1,10)", "f" to "float?", "u" to "str?")
        assertTrue(Opzioni.valida(mapOf("p" to 1883, "b" to true, "l" to "info", "n" to 3), s).isEmpty())
        val e = Opzioni.valida(mapOf("p" to 0, "b" to "si", "l" to "trace", "n" to 11, "z" to 1), s)
        assertEquals(listOf("p: atteso port", "b: atteso bool", "l: atteso list(debug|info)", "n: atteso int(1,10)",
            "z: non previsto dallo schema"), e)
        assertEquals(listOf("p: obbligatorio", "b: obbligatorio", "l: obbligatorio", "n: obbligatorio"), Opzioni.valida(emptyMap(), s))
    }

    private fun manifest(slug: String, porta: Int) = Manifest.parse(slug,
        "name: $slug\nversion: 1\ntype: python\nentry: a.py\nports: [$porta]\noptions:\n  p: 1\nschema:\n  p: int")

    @Test fun statoPoolEPersistenza() {
        val dir = Files.createTempDirectory("addon").toFile()
        val s = AddonStato(dir, slot = 2)
        s.installa(manifest("a", 1000), "r")
        s.installa(manifest("b", 1000), "r")
        s.installa(manifest("c", 2000), "r")
        assertTrue(File(dir, "a/manifest.json").isFile)
        assertEquals(1, s.assegnaSlot("a"))
        assertEquals(1, s.assegnaSlot("a"))
        assertEquals(listOf(1000), s.conflittoPorte("b"))
        assertEquals(2, s.assegnaSlot("c"))
        assertNull(s.assegnaSlot("b"))
        s.liberaSlot("a")
        assertEquals(1, s.assegnaSlot("b"))
        assertTrue(s.conflittoPorte("a").isNotEmpty())

        assertEquals(listOf("p: atteso int"), s.impostaOpzioni("c", mapOf("p" to "x")))
        assertTrue(s.impostaOpzioni("c", mapOf("p" to 5), avvioAuto = true).isEmpty())
        s.aggiungiRepository("https://example.org/addons/")

        val r = AddonStato(dir, slot = 2)
        assertEquals(listOf("a", "b", "c"), r.installati().map { it.manifest.slug })
        assertEquals(5, r.installato("c")!!.opzioni["p"])
        assertTrue(r.installato("c")!!.avvioAuto)
        assertEquals(1, r.slotDi("b"))
        assertEquals("c", r.slugNelloSlot(2))
        assertEquals(listOf(AddonStato.REPO_UFFICIALE, "https://example.org/addons"), r.repository)

        assertTrue(r.rimuovi("b"))
        assertFalse(File(dir, "b").exists())
        assertNull(r.slotDi("b"))
        assertFalse(r.rimuovi("b"))
        dir.deleteRecursively()
    }

    @Test fun aggiornamentoTieneOpzioniValide() {
        val dir = Files.createTempDirectory("addon").toFile()
        val s = AddonStato(dir)
        s.installa(manifest("a", 1), "r")
        s.impostaOpzioni("a", mapOf("p" to 9))
        s.installa(manifest("a", 1), "r")
        assertEquals(9, s.installato("a")!!.opzioni["p"])
        // Schema nuovo incompatibile: si torna ai default del manifest.
        s.installa(Manifest.parse("a", "name: a\nversion: 2\ntype: python\nentry: a.py\noptions:\n  q: x\nschema:\n  q: str"), "r")
        assertEquals(mapOf("q" to "x"), s.installato("a")!!.opzioni)
        dir.deleteRecursively()
    }

    @Test fun token() {
        assertTrue(AddonApi.autorizzato("Bearer abc", "abc"))
        assertFalse(AddonApi.autorizzato("Bearer abd", "abc"))
        assertFalse(AddonApi.autorizzato(null, "abc"))
    }

    @Test fun binarioNativoSoloSeEsiste() {
        val dir = Files.createTempDirectory("lib").toFile()
        assertNull(AddonService.binarioNativo(dir.path, "mosquitto"))
        File(dir, "libmosquitto.so").writeText("")
        assertEquals("libmosquitto.so", AddonService.binarioNativo(dir.path, "mosquitto")!!.name)
        dir.deleteRecursively()
    }
}
