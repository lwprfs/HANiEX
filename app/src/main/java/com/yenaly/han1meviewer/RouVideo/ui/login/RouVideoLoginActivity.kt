package com.yenaly.han1meviewer.RouVideo.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RouVideo.common.RouVideoConstants
import com.yenaly.han1meviewer.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.RouVideo.data.remote.RouVideoCookieJar
import com.yenaly.han1meviewer.USER_AGENT
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Signs in to (or registers on) rou.video. The site delegates both to its own account server
 * (`auth.rou.one`), so the real web forms are shown in a WebView. When the site redirects
 * back with a session cookie, the cookies are handed to [RouVideoAuth] and the activity closes.
 */
class RouVideoLoginActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_REGISTER = "register"

        fun intent(context: Context, register: Boolean = false): Intent =
            Intent(context, RouVideoLoginActivity::class.java).putExtra(EXTRA_REGISTER, register)
    }

    private val progressState = mutableIntStateOf(0)
    private var finished = false
    private var registerClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val wantRegister = intent.getBooleanExtra(EXTRA_REGISTER, false)

        val composeView = ComposeView(this)
        setContentView(composeView)
        composeView.setContent {
            HanimeTheme {
                RouVideoLoginScreen(
                    register = wantRegister,
                    progress = progressState.intValue,
                    onClose = { finish() },
                    webViewFactory = { createWebView(wantRegister) },
                )
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(wantRegister: Boolean): WebView {
        val baseUrl = RouVideoConstants.BASE_URL
        val siteHost = baseUrl.toHttpUrlOrNull()?.host
        return WebView(this).apply {
            val wv = this
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                javaScriptCanOpenWindowsAutomatically = true
                userAgentString = USER_AGENT
            }
            val cookies = CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(wv, true)
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean = false

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val host = url?.toHttpUrlOrNull()?.host ?: return

                    if (host == RouVideoConstants.AUTH_HOST) {
                        // On the sign-in form: jump to the registration form when asked to.
                        if (wantRegister && !registerClicked && url.contains("openid-connect/auth")) {
                            registerClicked = true
                            view?.evaluateJavascript(CLICK_REGISTER_JS, null)
                        }
                        return
                    }

                    // Back on the site (callback finished): the session cookie is set.
                    if (siteHost != null && host == siteHost && !finished) {
                        val header = cookies.getCookie(baseUrl).orEmpty()
                        if (header.contains("${RouVideoCookieJar.SESSION_COOKIE}=")) {
                            finished = true
                            cookies.flush()
                            RouVideoAuth.onWebLoginFinished(header)
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressState.intValue = newProgress
                }
            }

            loadUrl(RouVideoConstants.signInUrl())
        }
    }
}

/** Clicks the "Register" link of the Keycloak sign-in form, whichever theme renders it. */
private const val CLICK_REGISTER_JS = """
(function () {
  var link = document.querySelector('a[href*="login-actions/registration"]')
          || document.querySelector('#kc-registration a');
  if (link) { link.click(); return true; }
  return false;
})();
"""

@Composable
private fun RouVideoLoginScreen(
    register: Boolean,
    progress: Int,
    onClose: () -> Unit,
    webViewFactory: () -> WebView,
) {
    HanimeScaffold(
        title = stringResource(
            if (register) R.string.rouvideo_register else R.string.rouvideo_sign_in
        ),
        onBack = onClose,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { webViewFactory() },
                modifier = Modifier.fillMaxSize(),
            )
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    strokeCap = StrokeCap.Round,
                )
            }
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.rouvideo_login_tip),
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * Returns a function that opens the sign-in (or registration) page; [onFinished] runs when the
 * page closes, with `true` when the viewer ended up signed in.
 */
@Composable
fun rememberRouVideoLoginLauncher(onFinished: (Boolean) -> Unit = {}): (register: Boolean) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        RouVideoAuth.reload()
        onFinished(RouVideoAuth.isLoggedIn)
    }
    return remember(launcher, context) {
        { register: Boolean -> launcher.launch(RouVideoLoginActivity.intent(context, register)) }
    }
}
