package it.handroid

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/**
 * I pezzi della schermata, disegnati con le viste di Android: niente librerie in più (l'APK porta già
 * Python e HA). Schede arrotondate, righe toccabili con stato sotto il titolo, tema chiaro o scuro
 * secondo il telefono.
 */
class Ui(private val a: Activity) {
    enum class Livello { OK, ATTESA, SPENTO }

    private val scuro = a.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val sfondo = if (scuro) 0xFF111418.toInt() else 0xFFF3F5F8.toInt()
    private val superficie = if (scuro) 0xFF1D2127.toInt() else Color.WHITE
    private val testo = if (scuro) 0xFFE8EAED.toInt() else 0xFF1B1F24.toInt()
    private val secondario = if (scuro) 0xFF9AA3AD.toInt() else 0xFF5F6B76.toInt()
    private val linea = if (scuro) 0xFF2C323A.toInt() else 0xFFE4E8EC.toInt()
    private val primario = 0xFF03A9F4.toInt()  // l'azzurro di Home Assistant
    private val rosso = 0xFFE5484D.toInt()

    private fun dp(v: Int) = (v * a.resources.displayMetrics.density).toInt()

    private fun forma(colore: Int, raggio: Int = 20, bordo: Int? = null) = GradientDrawable().apply {
        setColor(colore); cornerRadius = dp(raggio).toFloat()
        bordo?.let { setStroke(dp(1), it) }
    }

    private fun tocco(colore: Int, raggio: Int = 20, bordo: Int? = null) =
        RippleDrawable(ColorStateList.valueOf(0x33808080), forma(colore, raggio, bordo), forma(Color.WHITE, raggio))

    private fun scritta(t: String, sp: Float, colore: Int = testo, grassetto: Boolean = false) = TextView(a).apply {
        text = t; textSize = sp; setTextColor(colore)
        if (grassetto) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun verticale(vararg viste: View) = LinearLayout(a).apply {
        orientation = LinearLayout.VERTICAL
        viste.forEach { addView(it) }
    }

    private fun <V : View> margini(v: V, sopra: Int = 0, sotto: Int = 0) = v.apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(sopra); bottomMargin = dp(sotto) }
    }

    /** Tutta la schermata: scorre, con lo spazio per le barre di sistema. */
    fun pagina(vararg viste: View): View {
        a.window.statusBarColor = sfondo
        a.window.navigationBarColor = sfondo
        if (!scuro) a.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        return ScrollView(a).apply {
            setBackgroundColor(sfondo)
            isFillViewport = true
            addView(verticale(*viste).apply { setPadding(dp(16), dp(24), dp(16), dp(32)) })
        }
    }

    fun testata(titolo: String, sotto: String) = margini(verticale(
        scritta(titolo, 28f, grassetto = true),
        scritta(sotto, 14f, secondario),
    ).apply { setPadding(dp(4), 0, dp(4), 0) }, sotto = 16)

    fun piccolo(t: String) = margini(scritta(t, 12f, secondario).apply { setPadding(dp(4), 0, dp(4), 0) }, sopra = 8)

    private fun pulsante(t: String, pieno: Boolean, azione: () -> Unit) = scritta(t, 15f, if (pieno) Color.WHITE else primario, true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = if (pieno) tocco(primario, 24) else tocco(superficie, 24, linea)
        setOnClickListener { azione() }
    }

    /** La scheda in cima: pallino colorato, cosa sta facendo HA, e i tre comandi principali. */
    inner class Stato(apri: () -> Unit, riavvia: () -> Unit, accendiSpegni: () -> Unit) {
        private val pallino = View(a).apply { layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { marginEnd = dp(12) } }
        private val titolo = scritta("", 20f, grassetto = true)
        private val sotto = scritta("", 14f, secondario).apply { setPadding(0, dp(4), 0, 0) }
        private val accendi = pulsante("Ferma", false, accendiSpegni)
        private val scheda = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            addView(LinearLayout(a).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(pallino); addView(titolo)
            })
            addView(sotto)
            addView(margini(pulsante("Apri Home Assistant", true, apri), sopra = 20))
            addView(margini(LinearLayout(a).apply {
                addView(pulsante("Riavvia", false, riavvia), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
                addView(accendi, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
            }, sopra = 10))
        }
        val vista: View = margini(scheda, sotto = 16)

        fun mostra(livello: Livello, t: String, s: String, acceso: Boolean) {
            val colore = when (livello) {
                Livello.OK -> 0xFF30A46C.toInt()
                Livello.ATTESA -> 0xFFF5A623.toInt()
                Livello.SPENTO -> 0xFF8B949E.toInt()
            }
            pallino.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(colore) }
            // sfondo appena tinto del colore dello stato
            scheda.background = forma(mescola(colore, superficie, if (scuro) 0.16f else 0.10f), 24)
            titolo.text = t
            sotto.text = s
            accendi.text = if (acceso) "Ferma" else "Avvia"
        }
    }

    private fun mescola(c: Int, base: Int, q: Float): Int {
        fun m(sh: Int) = (((c shr sh) and 0xFF) * q + ((base shr sh) and 0xFF) * (1 - q)).toInt() shl sh
        return 0xFF000000.toInt() or m(16) or m(8) or m(0)
    }

    /** Scheda gialla con un'azione: compare solo quando serve. */
    fun avviso(titolo: String, t: String, bottone: String, azione: () -> Unit): View {
        val giallo = 0xFFF5A623.toInt()
        return margini(verticale(
            scritta(titolo, 16f, grassetto = true),
            scritta(t, 14f, secondario).apply { setPadding(0, dp(4), 0, dp(12)) },
            pulsante(bottone, true, azione).apply { background = tocco(giallo, 24); setTextColor(0xFF1B1F24.toInt()) },
        ).apply {
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = forma(mescola(giallo, superficie, if (scuro) 0.18f else 0.14f), 24)
        }, sotto = 16)
    }

    /** Titolo di sezione e una scheda con le righe separate da una linea sottile. */
    fun scheda(titolo: String, vararg righe: View): View {
        val corpo = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(superficie, 20)
            clipToOutline = true
            righe.forEachIndexed { i, r ->
                if (i > 0) addView(View(a).apply {
                    setBackgroundColor(linea)
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(1)).apply { marginStart = dp(20) }
                })
                addView(r)
            }
        }
        return margini(verticale(
            scritta(titolo.uppercase(), 12f, secondario, true).apply { letterSpacing = 0.08f; setPadding(dp(8), 0, 0, dp(8)) },
            corpo,
        ), sopra = 8, sotto = 8)
    }

    /** Riga toccabile: titolo, stato sotto (aggiornabile) e una freccia. */
    inner class Riga(titolo: String, sotto: String, azione: () -> Unit, pericolo: Boolean) {
        private val sottotitolo = scritta(sotto, 13f, secondario).apply { visibility = if (sotto.isEmpty()) View.GONE else View.VISIBLE }
        val vista: View = LinearLayout(a).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(16), dp(14))
            background = RippleDrawable(ColorStateList.valueOf(0x22808080), null, forma(Color.WHITE, 0))
            addView(verticale(scritta(titolo, 16f, if (pericolo) rosso else testo), sottotitolo),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(scritta("›", 22f, secondario))
            setOnClickListener { azione() }
        }

        fun sotto(t: String) {
            sottotitolo.text = t
            sottotitolo.visibility = if (t.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    fun riga(titolo: String, sotto: String, azione: () -> Unit) = Riga(titolo, sotto, azione, false)

    /** Come [riga], con il titolo in rosso: azioni che cancellano. */
    fun pericolo(titolo: String, sotto: String, azione: () -> Unit) = Riga(titolo, sotto, azione, true)

    fun interruttore(titolo: String, sotto: String, s: Switch): View = LinearLayout(a).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), dp(14), dp(16), dp(14))
        addView(verticale(scritta(titolo, 16f), scritta(sotto, 13f, secondario)), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        addView(s)
        setOnClickListener { s.toggle() }
    }

    companion object {
        /** "ora", "40 s fa", "3 min fa", "2 h fa". */
        fun fa(secondi: Long) = when {
            secondi < 5 -> "ora"
            secondi < 60 -> "$secondi s fa"
            secondi < 3600 -> "${secondi / 60} min fa"
            else -> "${secondi / 3600} h fa"
        }
    }
}
