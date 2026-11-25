package com.yenaly.han1meviewer.SitesHub.RedGifs.ui.account

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsLoginFlow
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private class LoginHolder {
    var webView: WebView? = null
    var start: RedGifsLoginFlow.WebStart? = null
    var handled: Boolean = false
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RedGifsLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    startOnRegister: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val success = stringResource(R.string.redgifs_login_success)
    val failed = stringResource(R.string.redgifs_login_failed)
    val pageConfig = buildJsonObject {
        put("signup", startOnRegister)
        put("dark", dark)
        put("api", RedGifsConstants.API_BASE)
        put("siteKey", RedGifsConstants.TURNSTILE_SITE_KEY)
        put("sessionId", RedGifsAuth.sessionId)
        put("s", buildJsonObject {
            put("loginTitle", stringResource(R.string.redgifs_login_title))
            put("loginSub", stringResource(R.string.redgifs_signin_hint))
            put("signupTitle", stringResource(R.string.redgifs_signup_title))
            put("signupSub", stringResource(R.string.redgifs_register_hint))
            put("emailOrUser", stringResource(R.string.redgifs_email_or_username))
            put("username", stringResource(R.string.redgifs_username))
            put("email", stringResource(R.string.redgifs_email))
            put("cont", stringResource(R.string.redgifs_continue))
            put("verifying", stringResource(R.string.redgifs_verifying))
            put("working", stringResource(R.string.redgifs_working))
            put("noAccount", stringResource(R.string.redgifs_no_account))
            put("createAccount", stringResource(R.string.redgifs_create_account))
            put("haveAccount", stringResource(R.string.redgifs_have_account))
            put("signIn", stringResource(R.string.redgifs_sign_in))
            put("captchaFailed", stringResource(R.string.redgifs_captcha_failed))
        })
    }.toString()
    val currentHtml by rememberUpdatedState(loginHtml(dark, pageConfig))
    val currentSuccess by rememberUpdatedState(success)
    val currentFailed by rememberUpdatedState(failed)
    val currentOnLoggedIn by rememberUpdatedState(onLoggedIn)

    val holder = remember { LoginHolder() }
    var generation by remember { mutableIntStateOf(0) }
    var progress by remember { mutableIntStateOf(0) }
    var onAuthPage by remember { mutableStateOf(false) }
    var exchanging by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var showCookie by rememberSaveable { mutableStateOf(false) }

    remember { RedGifsAuth.clearWebSession(); true }

    fun showLoginPage() {
        holder.start = null
        holder.handled = false
        loadError = false
        exchanging = false
        onAuthPage = false
        holder.webView?.loadDataWithBaseURL(
            "${RedGifsConstants.BASE_URL}/",
            currentHtml,
            "text/html",
            "utf-8",
            null,
        )
    }

    fun handleReturn(uri: Uri) {
        if (holder.handled) return
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val start = holder.start
        if (code.isNullOrBlank() || start == null || (state != null && state != start.state)) {
            GlobalToasts.show(
                uri.getQueryParameter("error_description") ?: currentFailed,
                level = GlobalToasts.ToastLevel.ERROR,
            )
            showLoginPage()
            return
        }
        holder.handled = true
        exchanging = true
        scope.launch {
            try {
                RedGifsLoginFlow.finishWeb(code, start.verifier)
                RedGifsRepo.me()
                GlobalToasts.show(currentSuccess, level = GlobalToasts.ToastLevel.SUCCESS)
                currentOnLoggedIn()
            } catch (e: RedGifsLoginFlow.FlowException) {
                GlobalToasts.show(e.message ?: currentFailed, level = GlobalToasts.ToastLevel.ERROR)
                showLoginPage()
            }
        }
    }

    fun submitFromPage(mode: String, login: String, username: String, email: String, token: String) {
        scope.launch {
            try {
                val start = RedGifsLoginFlow.startWeb(
                    login = if (mode == "signup") null else login,
                    username = if (mode == "signup") username else null,
                    email = if (mode == "signup") email else null,
                    captcha = token,
                )
                holder.start = start
                holder.handled = false
                holder.webView?.loadUrl(start.authorizeUrl, mapOf("Referer" to RedGifsConstants.REFERER))
            } catch (e: RedGifsLoginFlow.FlowException) {
                val msg = JsonPrimitive(e.message ?: currentFailed).toString()
                holder.webView?.evaluateJavascript("window.onNativeError($msg)", null)
            }
        }
    }

    BackHandler(enabled = onAuthPage && !exchanging) { showLoginPage() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.redgifs_sign_in)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    TextButton(onClick = { showCookie = true }) {
                        Text(stringResource(R.string.redgifs_cookie))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            key(generation) {
                val mine = remember { arrayOfNulls<WebView>(1) }
                DisposableEffect(Unit) {
                    onDispose {
                        mine[0]?.let { view ->
                            (view.parent as? ViewGroup)?.removeView(view)
                            view.stopLoading()
                            view.removeJavascriptInterface("RedGifsLogin")
                            view.destroy()
                            if (holder.webView === view) holder.webView = null
                        }
                        mine[0] = null
                    }
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        WebView(context).apply {
                            holder.webView = this
                            mine[0] = this
                            setBackgroundColor(if (dark) 0xFF0E0E12.toInt() else AndroidColor.WHITE)
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
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun submit(mode: String, login: String, username: String, email: String, token: String) {
                                    submitFromPage(mode, login, username, email, token)
                                }
                            }, "RedGifsLogin")
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    if (isOAuthReturn(request.url)) {
                                        handleReturn(request.url)
                                        return true
                                    }
                                    val scheme = request.url.scheme.orEmpty()
                                    return scheme != "http" && scheme != "https"
                                }

                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    val uri = Uri.parse(url.orEmpty())
                                    if (isOAuthReturn(uri)) {
                                        view.stopLoading()
                                        handleReturn(uri)
                                        return
                                    }
                                    onAuthPage = uri.host == Uri.parse(RedGifsConstants.AUTH_BASE).host
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError,
                                ) {
                                    if (request.isForMainFrame && !holder.handled) loadError = true
                                }

                                override fun onRenderProcessGone(
                                    view: WebView,
                                    detail: RenderProcessGoneDetail,
                                ): Boolean {
                                    holder.start = null
                                    holder.handled = false
                                    onAuthPage = false
                                    generation++
                                    return true
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }
                            }
                            loadDataWithBaseURL(
                                "${RedGifsConstants.BASE_URL}/",
                                currentHtml,
                                "text/html",
                                "utf-8",
                                null,
                            )
                        }
                    },
                )
            }

            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                )
            }

            if (loadError && !exchanging) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.redgifs_load_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { showLoginPage() }, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.redgifs_retry))
                    }
                }
            }

            if (exchanging) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.redgifs_working),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (showCookie) {
        CookieDialog(
            onDismiss = { showCookie = false },
            onLoggedIn = {
                showCookie = false
                GlobalToasts.show(success, level = GlobalToasts.ToastLevel.SUCCESS)
                onLoggedIn()
            },
        )
    }
}

@Composable
private fun CookieDialog(onDismiss: () -> Unit, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalid = stringResource(R.string.redgifs_cookie_invalid)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.redgifs_cookie_field)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.redgifs_cookie_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.redgifs_cookie_field)) },
                    minLines = 3,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
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
            ) { Text(stringResource(R.string.redgifs_sign_in)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun isOAuthReturn(uri: Uri): Boolean {
    val host = uri.host ?: return false
    val base = Uri.parse(RedGifsConstants.ORIGIN).host ?: return false
    if (host.removePrefix("www.") != base.removePrefix("www.")) return false
    return uri.getQueryParameter("code") != null || uri.getQueryParameter("error") != null
}

private fun loginHtml(dark: Boolean, configJson: String): String = LOGIN_HTML
    .replace("%%THEME%%", if (dark) "dark" else "light")
    .replace("%%CFG%%", configJson.replace("<", "\\u003c"))

private val LOGIN_HTML = """
<!doctype html><html class="%%THEME%%"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<style>
:root{--bg:#fff;--fg:#14141a;--muted:#6b6b76;--line:#d9d9e0;--field:#f5f5f8;--brand:#e6264b;--err:#c62828}
html.dark{--bg:#0e0e12;--fg:#f2f2f6;--muted:#a0a0ad;--line:#2c2c36;--field:#17171d;--brand:#ff3b5f;--err:#ff6b6b}
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
html,body{margin:0;min-height:100%;background:var(--bg);color:var(--fg);font-family:Roboto,-apple-system,"Segoe UI",sans-serif}
.wrap{max-width:420px;margin:0 auto;padding:32px 24px 56px}
.brand{font-size:32px;font-weight:800;letter-spacing:-.5px;color:var(--brand);margin:0 0 28px}
h1{font-size:24px;margin:0 0 8px}
.sub{margin:0 0 26px;color:var(--muted);line-height:1.45;font-size:15px}
label{display:block;font-size:13px;font-weight:600;margin:0 0 6px}
input{width:100%;height:50px;padding:0 14px;margin:0 0 16px;border:1px solid var(--line);border-radius:12px;background:var(--field);color:var(--fg);font-size:16px;outline:none}
input:focus{border-color:var(--brand)}
#ts{min-height:65px;margin:4px 0 12px;display:flex;justify-content:center}
button{width:100%;height:52px;border:0;border-radius:26px;background:var(--brand);color:#fff;font-size:16px;font-weight:700}
button:disabled{opacity:.45}
.err{color:var(--err);font-size:14px;min-height:20px;margin:0 0 12px}
.sw{margin-top:24px;text-align:center;color:var(--muted);font-size:14px}
.sw a{color:var(--brand);font-weight:700;text-decoration:none;margin-left:4px}
</style>
<script>
var C=%%CFG%%;
window.onTs=function(){
  try{
    window.wid=turnstile.render('#ts',{
      sitekey:C.siteKey,theme:C.dark?'dark':'light',
      callback:function(t){window.token=t;document.getElementById('err').textContent='';upd();},
      'error-callback':function(){window.token=null;document.getElementById('err').textContent=C.s.captchaFailed;upd();},
      'expired-callback':function(){window.token=null;upd();}
    });
  }catch(e){document.getElementById('err').textContent=C.s.captchaFailed;}
};
</script>
<script src="https://challenges.cloudflare.com/turnstile/v0/api.js?onload=onTs&render=explicit" async defer></script>
</head><body>
<div class="wrap">
  <div class="brand">RedGifs</div>
  <h1 id="title"></h1>
  <p class="sub" id="sub"></p>
  <form id="f" autocomplete="on" novalidate>
    <div id="loginBox">
      <label for="login" id="lLogin"></label>
      <input id="login" name="username" type="text" autocomplete="username" autocapitalize="none" autocorrect="off" spellcheck="false" enterkeyhint="go">
    </div>
    <div id="signupBox" style="display:none">
      <label for="username" id="lUser"></label>
      <input id="username" type="text" autocomplete="username" autocapitalize="none" autocorrect="off" spellcheck="false">
      <label for="email" id="lEmail"></label>
      <input id="email" type="email" autocomplete="email" autocapitalize="none" autocorrect="off" spellcheck="false" enterkeyhint="go">
    </div>
    <div id="ts"></div>
    <div class="err" id="err"></div>
    <button id="btn" type="submit" disabled></button>
  </form>
  <div class="sw"><span id="swTxt"></span><a href="#" id="sw"></a></div>
</div>
<script>
var mode=C.signup?'signup':'login';
window.token=null;var busy=false;
function g(i){return document.getElementById(i);}
function ready(){
  if(!window.token)return false;
  if(mode==='signup')return !!g('username').value.trim()&&g('email').value.indexOf('@')>0;
  return !!g('login').value.trim();
}
function upd(){
  var b=g('btn');b.disabled=busy||!ready();
  b.textContent=busy?C.s.working:(window.token?C.s.cont:C.s.verifying);
}
function draw(){
  var s=mode==='signup';
  g('title').textContent=s?C.s.signupTitle:C.s.loginTitle;
  g('sub').textContent=s?C.s.signupSub:C.s.loginSub;
  g('lLogin').textContent=C.s.emailOrUser;g('lUser').textContent=C.s.username;g('lEmail').textContent=C.s.email;
  g('loginBox').style.display=s?'none':'block';
  g('signupBox').style.display=s?'block':'none';
  g('swTxt').textContent=s?C.s.haveAccount:C.s.noAccount;
  g('sw').textContent=s?C.s.signIn:C.s.createAccount;
  g('err').textContent='';upd();
}
window.onNativeError=function(m){
  busy=false;window.token=null;
  try{turnstile.reset(window.wid);}catch(e){}
  g('err').textContent=m||'';upd();
};
['login','username','email'].forEach(function(i){g(i).addEventListener('input',upd);});
g('sw').addEventListener('click',function(e){e.preventDefault();mode=mode==='signup'?'login':'signup';draw();});
g('f').addEventListener('submit',function(e){
  e.preventDefault();
  if(busy||!ready())return;
  busy=true;g('err').textContent='';upd();
  try{
    RedGifsLogin.submit(mode,g('login').value.trim(),g('username').value.trim(),g('email').value.trim(),window.token);
  }catch(x){window.onNativeError(String(x));}
});
draw();
</script>
</body></html>
""".trimIndent()
