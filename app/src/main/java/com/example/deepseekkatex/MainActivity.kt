package com.example.deepseekkatex

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class MainActivity : Activity() {

    companion object {
        private const val TARGET_URL = "https://chat.deepseek.com/"
        private const val REQ_FILE = 1001

        // KaTeX 对 \text{中文} 会输出 .cjk_fallback 类，补充中文字体回退
        private const val CJK_FALLBACK_CSS =
            ".katex .cjk_fallback{font-family:\"Noto Sans CJK SC\",\"Noto Sans SC\"," +
                "\"Source Han Sans SC\",\"Droid Sans Fallback\",sans-serif;}"
    }

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private lateinit var katexJs: String
    private lateinit var cssInjectJs: String
    private lateinit var rendererJs: String

    // 兜底：单页应用整页替换后若渲染器丢失，自动重新注入
    private val watchdog = object : Runnable {
        override fun run() {
            webView.evaluateJavascript("!!(window.__dkxReady && window.katex)") { r ->
                if (r != "true") inject()
            }
            handler.postDelayed(this, 5000)
        }
    }

    private fun readAsset(name: String): String =
        assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun inject() {
        webView.evaluateJavascript("!!window.katex") { r ->
            if (r != "true") webView.evaluateJavascript(katexJs, null)
            webView.evaluateJavascript(cssInjectJs, null)
            webView.evaluateJavascript(rendererJs, null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        katexJs = readAsset("katex.min.js")
        rendererJs = readAsset("renderer.js")
        val css = readAsset("katex.selfcontained.css") + CJK_FALLBACK_CSS
        cssInjectJs = "(function(){" +
            "if(document.getElementById('dkx-katex-css'))return;" +
            "var s=document.createElement('style');s.id='dkx-katex-css';" +
            "s.textContent=" + JSONObject.quote(css) + ";" +
            "(document.head||document.documentElement).appendChild(s);})();"

        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(webView)

        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportZoom(false)
        // 去掉 UA 里的 "; wv"，减少网站对 WebView 的区别对待
        s.userAgentString = s.userAgentString.replace("; wv", "")

        // 持久化 Cookie，保持登录状态
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                handler.postDelayed({ inject() }, 600)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme
                if (scheme == "http" || scheme == "https") return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    // 没有可处理的应用，忽略
                }
                return true
            }
        }

        // 支持网页里的文件上传
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback
                val intent = fileChooserParams?.createIntent()
                if (intent == null) {
                    fileCallback = null
                    return false
                }
                return try {
                    startActivityForResult(intent, REQ_FILE)
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }
        }

        webView.loadUrl(TARGET_URL)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_FILE) {
            fileCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
            fileCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        handler.postDelayed(watchdog, 5000)
    }

    override fun onPause() {
        handler.removeCallbacks(watchdog)
        CookieManager.getInstance().flush()
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webView.destroy()
        super.onDestroy()
    }
}
