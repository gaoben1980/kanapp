package uk.kanshenme.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val TARGET_URL = "https://kanshenme.uk"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.navigationBarColor = Color.parseColor("#0A0A0A")
        window.statusBarColor = Color.parseColor("#000000")

        webView = findViewById(R.id.webView)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.setBackgroundColor(Color.parseColor("#000000"))

        webView.loadUrl(TARGET_URL)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_back -> {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    }
                    true
                }
                R.id.nav_forward -> {
                    if (webView.canGoForward()) {
                        webView.goForward()
                    }
                    true
                }
                R.id.nav_home -> {
                    webView.loadUrl(TARGET_URL)
                    item.isChecked = true
                    true
                }
                R.id.nav_refresh -> {
                    webView.reload()
                    true
                }
                R.id.nav_share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "看什么 - 影视网站")
                        putExtra(Intent.EXTRA_TEXT, webView.url ?: TARGET_URL)
                    }
                    startActivity(Intent.createChooser(shareIntent, "分享给朋友..."))
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
