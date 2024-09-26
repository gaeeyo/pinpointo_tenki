package nikeno.Tenki.activity

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import nikeno.Tenki.R

class HelpActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.help)

        val webView = findViewById<WebView>(R.id.webView)
        webView.loadUrl("file:///android_asset/help.html")
    }
}
