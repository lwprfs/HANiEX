package com.yenaly.han1meviewer.RedGifs.ui.account

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.delay

private val JWT_REGEX = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")

private const val SCAN_STORAGE_JS =
    "(function(){var out=[];function scan(s){try{for(var i=0;i<s.length;i++){var v=s.getItem(s.key(i));" +
            "if(v&&v.indexOf('eyJ')>=0){out.push(v);}}}catch(e){}}" +
            "scan(window.localStorage);scan(window.sessionStorage);return JSON.stringify(out);})()"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RedGifsLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    val context = LocalContext.current
    val success = stringResource(R.string.redgifs_login_success)
    var progress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pendingAccess by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
            webView = null
        }
    }

    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        var attemptedRefresh: String? = null
        while (!finished) {
            delay(1500)
            val refresh = RedGifsAuth.refreshTokenFromWebView()
            var ok = false
            if (refresh != null && refresh != attemptedRefresh) {
                attemptedRefresh = refresh
                ok = RedGifsAuth.completeLoginWithRefresh(refresh)
            }
            if (!ok) {
                val access = pendingAccess
                if (access != null) {
                    pendingAccess = null
                    ok = RedGifsAuth.completeLoginWithAccess(access)
                }
            }
            if (ok) {
                finished = true
                RedGifsRepo.me()
                GlobalToasts.show(success, level = GlobalToasts.ToastLevel.SUCCESS)
                onLoggedIn()
                return@LaunchedEffect
            }
            if (refresh == null) {
                view.evaluateJavascript(SCAN_STORAGE_JS) { raw ->
                    pendingAccess = JWT_REGEX.findAll(raw.orEmpty())
                        .map { it.value }
                        .firstOrNull { RedGifsAuth.isRedGifsAccessToken(it) }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.redgifs_sign_in)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        loadUrl(RedGifsConstants.BASE_URL)
                        webView = this
                    }
                },
            )
        }
    }
}
