package com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.remote

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.yenaly.han1meviewer.USER_AGENT
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object CosplayteleWebSniffer {

    private const val TIMEOUT_MS = 20_000L

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun sniff(embedUrl: String, referer: String): String? =
        withTimeoutOrNull(TIMEOUT_MS) {
            withContext(Dispatchers.Main) {
                var webView: WebView? = null
                try {
                    suspendCancellableCoroutine { cont ->
                        val wv = WebView(applicationContext)
                        webView = wv
                        wv.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            userAgentString = USER_AGENT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        wv.webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest,
                            ): WebResourceResponse? {
                                val url = request.url.toString()
                                if (url.substringBefore('?').endsWith(".m3u8") && cont.isActive) {
                                    cont.resume(url)
                                }
                                return null
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) = Unit

                            override fun onPageFinished(view: WebView, url: String?) {

                                view.evaluateJavascript(
                                    "(function(){try{var v=document.querySelector('video');if(v){v.muted=true;v.play();}" +
                                        "var b=document.querySelector('.jw-icon-display,.play,button');if(b)b.click();}catch(e){}})()",
                                    null,
                                )
                            }
                        }
                        wv.loadUrl(embedUrl, mapOf("Referer" to referer))
                        cont.invokeOnCancellation { webView?.post { webView?.destroy() } }
                    }
                } finally {
                    webView?.apply {
                        stopLoading()
                        destroy()
                    }
                }
            }
        }
}
