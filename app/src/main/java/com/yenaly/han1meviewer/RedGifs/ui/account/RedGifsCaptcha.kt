package com.yenaly.han1meviewer.RedGifs.ui.account

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.RedGifs.common.RedGifsConstants

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RedGifsCaptcha(
    onToken: (String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tokenCallback by rememberUpdatedState(onToken)
    val errorCallback by rememberUpdatedState(onError)
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
            webView = null
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().height(150.dp),
        factory = {
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = RedGifsConstants.USER_AGENT
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onToken(token: String) {
                        post { tokenCallback(token) }
                    }

                    @JavascriptInterface
                    fun onError() {
                        post { errorCallback() }
                    }
                }, "AndroidCaptcha")
                webViewClient = WebViewClient()
                loadDataWithBaseURL(
                    "${RedGifsConstants.BASE_URL}/",
                    captchaHtml(),
                    "text/html",
                    "utf-8",
                    null,
                )
                webView = this
            }
        },
    )
}

private fun captchaHtml(): String = """
<!doctype html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;background:transparent;display:flex;justify-content:center;align-items:center;min-height:100%}</style>
<script src="https://challenges.cloudflare.com/turnstile/v0/api.js?onload=onTs" async defer></script>
</head><body><div id="c"></div>
<script>
function onTs(){
  turnstile.render('#c',{
    sitekey:'${RedGifsConstants.TURNSTILE_SITE_KEY}',
    theme:'auto',
    callback:function(t){AndroidCaptcha.onToken(t)},
    'error-callback':function(){AndroidCaptcha.onError()},
    'expired-callback':function(){AndroidCaptcha.onError()}
  });
}
</script></body></html>
""".trimIndent()
