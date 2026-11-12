package it.handroid

import android.Manifest
import android.app.Activity
import android.os.Build
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/** L'interfaccia è quella vera di Home Assistant, servita dal telefono stesso. */
class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceOwner.configura(this)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        HaService.avvia(this)

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                // Finché il server non è pronto riprova ogni 3 secondi.
                override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                    if (req.isForMainFrame) view.postDelayed({ view.reload() }, 3_000)
                }
            }
        }
        setContentView(web)
        if (savedInstanceState == null) web.loadUrl(URL) else web.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    /** Tenere premuto Indietro apre la schermata di stato (spec 0006). */
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK) return super.onKeyLongPress(keyCode, event)
        startActivity(Intent(this, StatoActivity::class.java))
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) event.startTracking()
        return super.onKeyDown(keyCode, event)
    }

    @Deprecated("Gestione del tasto indietro compatibile con API 29")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    private companion object {
        const val URL = "http://127.0.0.1:8123/"
    }
}
