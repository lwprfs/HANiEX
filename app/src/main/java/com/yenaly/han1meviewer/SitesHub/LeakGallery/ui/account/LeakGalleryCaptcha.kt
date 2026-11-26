package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account

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
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.USER_AGENT

/**
 * Cloudflare Turnstile widget. LeakGallery's `auth/login` and `auth/register` need the
 * widget's token in the `token` field, so it is rendered in a small WebView whose base URL is
 * the site itself (Turnstile only issues tokens for the site's own hostname).
 *
 * Tokens are single use: change [resetKey] to render a fresh widget after every attempt.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeakGalleryCaptcha(
    onToken: (String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Int = 0,
    dark: Boolean = true,
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
        modifier = modifier.fillMaxWidth().height(100.dp),
        factory = {
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = USER_AGENT
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
                tag = -1
                webView = this
            }
        },
        update = { view ->
            if (view.tag != resetKey) {
                view.tag = resetKey
                view.loadDataWithBaseURL(
                    "${LeakGalleryConstants.BASE_URL}/",
                    captchaHtml(dark),
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
    )
}

private fun captchaHtml(dark: Boolean): String = """
<!doctype html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;background:transparent;display:flex;justify-content:center;align-items:center;min-height:100%}</style>
<script src="https://challenges.cloudflare.com/turnstile/v0/api.js?onload=onTs" async defer></script>
</head><body><div id="c"></div>
<script>
function onTs(){
  turnstile.render('#c',{
    sitekey:'${LeakGalleryConstants.TURNSTILE_SITE_KEY}',
    theme:'${if (dark) "dark" else "light"}',
    callback:function(t){AndroidCaptcha.onToken(t)},
    'error-callback':function(){AndroidCaptcha.onError()},
    'expired-callback':function(){AndroidCaptcha.onToken('')}
  });
}
</script></body></html>
""".trimIndent()
