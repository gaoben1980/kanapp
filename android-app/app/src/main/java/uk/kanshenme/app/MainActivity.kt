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

import android.content.pm.ActivityInfo
import android.view.View
import android.widget.FrameLayout
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullScreenContainer: FrameLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private val TARGET_URL = "https://kanshenme.uk"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.navigationBarColor = Color.parseColor("#0A0A0A")
        window.statusBarColor = Color.parseColor("#000000")

        webView = findViewById(R.id.webView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fullScreenContainer = findViewById(R.id.fullScreenContainer)

        val webSettings: WebSettings = webView.settings
        
        // 核心功能
        webSettings.javaScriptEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        // --- 性能与缓存优化 (Performance & Caching) ---
        // 1. 开启 DOM 存储 (H5 缓存所需，如 localStorage)
        webSettings.domStorageEnabled = true
        // 2. 开启 数据库 存储
        webSettings.databaseEnabled = true
        // 3. 设置缓存模式 (默认模式：根据 HTTP 头部协议决定是否从网络获取数据)
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        // 4. 允许混合内容 (提升兼容性，避免图片等静态资源被拦截)
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        // 5. 允许跨域访问
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        // 6. 伪装 User-Agent，去掉 WebView 标识，防止部分 CDN 防护机制拦截图片
        webSettings.userAgentString = webSettings.userAgentString.replace("; wv", "")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                android.util.Log.d("WebViewConsole", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                fullScreenContainer.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                fullScreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE
                bottomNavigationView.visibility = View.GONE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }

            override fun onHideCustomView() {
                if (customView == null) {
                    return
                }
                fullScreenContainer.removeView(customView)
                customView = null
                fullScreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                bottomNavigationView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
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
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
