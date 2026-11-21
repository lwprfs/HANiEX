package com.yenaly.han1meviewer.RedGifs.ui.account

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Build
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsLoginFlow
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Stage { FORM, CAPTCHA, WORKING, CODE }

/** Sign in / Register (both with an emailed code, all in-app), sign in with a cookie, or the website. */
@Composable
fun RedGifsLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    startOnRegister: Boolean = false,
) {
    var tab by rememberSaveable { mutableIntStateOf(if (startOnRegister) 1 else 0) }
    var web by rememberSaveable { mutableStateOf(false) }
    val success = stringResource(R.string.redgifs_login_success)

    val done: () -> Unit = {
        GlobalToasts.show(success, level = GlobalToasts.ToastLevel.SUCCESS)
        onLoggedIn()
    }

    if (web) {
        RedGifsWebLogin(onBack = { web = false }, onLoggedIn = done)
        return
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
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.redgifs_sign_in)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.redgifs_register)) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.redgifs_cookie)) })
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (tab) {
                    0 -> EmailCodeForm(register = false, onLoggedIn = done)
                    1 -> EmailCodeForm(register = true, onLoggedIn = done)
                    else -> CookieForm(onLoggedIn = done)
                }
                TextButton(onClick = { web = true }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.redgifs_use_website))
                }
            }
        }
    }
}

@Composable
private fun EmailCodeForm(register: Boolean, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var login by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var stage by remember { mutableStateOf(Stage.FORM) }
    var pending by remember { mutableStateOf<RedGifsLoginFlow.Pending?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val captchaFailed = stringResource(R.string.redgifs_captcha_failed)

    fun reset() {
        stage = Stage.FORM
        pending = null
        code = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (stage) {
            Stage.FORM -> {
                Text(
                    stringResource(if (register) R.string.redgifs_register_hint else R.string.redgifs_signin_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (register) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.trim() },
                        label = { Text(stringResource(R.string.redgifs_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text(stringResource(R.string.redgifs_email)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = login,
                        onValueChange = { login = it.trim() },
                        label = { Text(stringResource(R.string.redgifs_email_or_username)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    enabled = if (register) username.isNotEmpty() && email.contains('@') else login.isNotEmpty(),
                    onClick = {
                        error = null
                        stage = Stage.CAPTCHA
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.redgifs_send_code)) }
            }

            Stage.CAPTCHA -> {
                Text(stringResource(R.string.redgifs_captcha_hint), style = MaterialTheme.typography.bodyMedium)
                RedGifsCaptcha(
                    onToken = { token ->
                        stage = Stage.WORKING
                        scope.launch {
                            try {
                                pending = RedGifsLoginFlow.begin(
                                    login = if (register) null else login,
                                    username = if (register) username else null,
                                    email = if (register) email else null,
                                    captcha = token,
                                )
                                stage = Stage.CODE
                            } catch (e: RedGifsLoginFlow.FlowException) {
                                error = e.message
                                stage = Stage.FORM
                            }
                        }
                    },
                    onError = {
                        error = captchaFailed
                        stage = Stage.FORM
                    },
                )
                TextButton(onClick = { reset() }) { Text(stringResource(R.string.cancel)) }
            }

            Stage.WORKING -> CircularProgressIndicator(Modifier.padding(24.dp))

            Stage.CODE -> {
                Text(
                    stringResource(R.string.redgifs_code_hint, if (register) email else login),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.trim() },
                    label = { Text(stringResource(R.string.redgifs_code)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    enabled = code.length >= 4,
                    onClick = {
                        val p = pending ?: return@Button
                        error = null
                        stage = Stage.WORKING
                        scope.launch {
                            try {
                                RedGifsLoginFlow.submitCode(p, code)
                                RedGifsRepo.me()
                                onLoggedIn()
                            } catch (e: RedGifsLoginFlow.FlowException) {
                                error = e.message
                                stage = Stage.CODE
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.redgifs_verify)) }
                TextButton(onClick = { reset() }) { Text(stringResource(R.string.redgifs_start_over)) }
            }
        }
    }
}

@Composable
private fun CookieForm(onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalid = stringResource(R.string.redgifs_cookie_invalid)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.redgifs_cookie_hint), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.redgifs_cookie_field)) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Button(
            enabled = text.isNotBlank() && !busy,
            onClick = {
                busy = true
                error = null
                scope.launch {
                    if (RedGifsAuth.importPasted(text)) {
                        RedGifsRepo.me()
                        onLoggedIn()
                    } else {
                        error = invalid
                    }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.redgifs_sign_in)) }
    }
}

private val JWT_REGEX = Regex("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")

private const val SCAN_STORAGE_JS =
    "(function(){var out=[];function scan(s){try{for(var i=0;i<s.length;i++){var v=s.getItem(s.key(i));" +
            "if(v&&v.indexOf('eyJ')>=0){out.push(v);}}}catch(e){}}" +
            "scan(window.localStorage);scan(window.sessionStorage);return JSON.stringify(out);})()"

/**
 * Last-resort sign in on the real site. Black-screen fixes: a fixed light background, no forced dark mode,
 * no popup windows, only http(s) navigation, and the render process is recovered instead of left blank.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RedGifsWebLogin(onBack: () -> Unit, onLoggedIn: () -> Unit) {
    val context = LocalContext.current
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
                title = { Text(stringResource(R.string.redgifs_use_website)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (progress in 1..99) {
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.WHITE)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = RedGifsConstants.USER_AGENT
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        @Suppress("DEPRECATION")
                        if (Build.VERSION.SDK_INT in 29..32) settings.forceDark = WebSettings.FORCE_DARK_OFF
                        if (Build.VERSION.SDK_INT >= 33) settings.isAlgorithmicDarkeningAllowed = false
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val scheme = request?.url?.scheme.orEmpty()
                                return scheme != "http" && scheme != "https"
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                view?.loadUrl(RedGifsConstants.BASE_URL)
                                return true
                            }
                        }
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
