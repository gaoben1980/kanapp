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
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullScreenContainer: GestureControlView
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

        // 性能与缓存优化
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        // 去掉 WebView 标识，防止部分 CDN 拦截
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

                // 将视频 View 加入全屏容器
                fullScreenContainer.addView(view, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                ))
                fullScreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE
                bottomNavigationView.visibility = View.GONE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                // 传入 window，激活亮度/音量手势控制
                fullScreenContainer.attach(window)
            }

            override fun onHideCustomView() {
                if (customView == null) return
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
                    if (webView.canGoBack()) webView.goBack()
                    true
                }
                R.id.nav_forward -> {
                    if (webView.canGoForward()) webView.goForward()
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
