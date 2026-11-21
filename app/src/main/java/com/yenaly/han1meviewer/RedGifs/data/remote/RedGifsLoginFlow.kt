package com.yenaly.han1meviewer.RedGifs.data.remote

import android.util.Base64
import com.yenaly.han1meviewer.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Native RedGifs sign-in / registration, reproducing what the web app does (all of it is in the HAR):
 *
 * 1. Turnstile captcha token (the only part that needs a WebView, see RedGifsCaptcha).
 * 2. `POST /v2/auth/login` or `/v2/auth/signup` -> `login_hint` + `connection_id`.
 * 3. OAuth2 authorize (PKCE) on auth2.redgifs.com -> hosted "verify email" page.
 * 4. `POST .../otp/otp_code_form` (multipart) with the emailed code -> redirect.
 * 5. Follow the redirect to `https://www.redgifs.com?code=...` and `POST /oauth2/token`.
 *
 * Steps 3-4 scrape the hosted page for its CSRF token; the HAR does not contain that page's body, so
 * this part is the least verified.
 */
object RedGifsLoginFlow {

    class FlowException(message: String) : Exception(message)

    class Pending internal constructor(
        internal val client: OkHttpClient,
        internal val verifier: String,
        internal val state: String,
        internal val psid: String,
        internal val csrf: String,
        internal val pageUrl: String,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()
    private val random = SecureRandom()

    private class MemoryCookieJar : CookieJar {
        private val store = ConcurrentHashMap<String, CopyOnWriteArrayList<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = store.getOrPut(url.host) { CopyOnWriteArrayList() }
            cookies.forEach { c ->
                list.removeAll { it.name == c.name && it.path == c.path }
                if (c.expiresAt > System.currentTimeMillis()) list.add(c)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            store[url.host].orEmpty().filter { it.matches(url) }
    }

    private fun newClient(): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(MemoryCookieJar())
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", RedGifsConstants.USER_AGENT)
                    .build()
            )
        }
        .dns(HDns())
        .proxySelector(HProxySelector())
        .build()

    private fun randomUrlSafe(bytes: Int): String {
        val b = ByteArray(bytes).also { random.nextBytes(it) }
        return Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun challengeOf(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun apiError(code: Int, body: String): String {
        val msg = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("description")
                ?.let { (it as? JsonPrimitive)?.contentOrNull }
        }.getOrNull()
        return msg ?: "HTTP $code"
    }

    /**
     * Steps 2-3. Pass [username] + [email] to register, or only [login] (email or username) to sign in.
     * Returns the pending state that [submitCode] needs once the user has the emailed code.
     */
    suspend fun begin(
        login: String?,
        username: String?,
        email: String?,
        captcha: String,
    ): Pending = withContext(Dispatchers.IO) {
        val client = newClient()
        val signup = username != null
        val payload = buildJsonObject {
            if (signup) {
                put("username", username.orEmpty())
                put("email", email.orEmpty())
            } else {
                put("blocked", false)
                put("login", login.orEmpty())
            }
            put("captcha", captcha)
            put("session_id", RedGifsAuth.sessionId)
        }.toString()
        val apiReq = Request.Builder()
            .url("${RedGifsConstants.API_BASE}/v2/auth/${if (signup) "signup" else "login"}")
            .post(payload.toRequestBody(jsonMedia))
            .header("Origin", RedGifsConstants.ORIGIN)
            .header("Referer", RedGifsConstants.REFERER)
            .header("Accept", "application/json")
            .build()
        val (loginHint, connectionId) = try {
            client.newCall(apiReq).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw FlowException(apiError(resp.code, text))
                val o = json.parseToJsonElement(text).jsonObject
                val hint = (o["login_hint"] as? JsonPrimitive)?.contentOrNull
                val conn = (o["connection_id"] as? JsonPrimitive)?.contentOrNull
                if (hint.isNullOrBlank() || conn.isNullOrBlank()) throw FlowException("Unexpected server response")
                hint to conn
            }
        } catch (e: IOException) {
            throw FlowException(e.message ?: "Network error")
        }

        val verifier = randomUrlSafe(48)
        val state = randomUrlSafe(24)
        val authorize = "${RedGifsConstants.AUTH_BASE}/oauth2/auth".toHttpUrl().newBuilder()
            .addQueryParameter("login_hint", loginHint)
            .addQueryParameter("connection_id", connectionId)
            .addQueryParameter("redirect_uri", RedGifsConstants.ORIGIN)
            .addQueryParameter("client_id", RedGifsConstants.CLIENT_ID)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", RedGifsConstants.OAUTH_SCOPE)
            .addQueryParameter("code_challenge", challengeOf(verifier))
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("state", state)
            .build()

        var url: HttpUrl = authorize
        var html = ""
        var landed: HttpUrl = authorize
        var hops = 0
        try {
            while (hops++ < 8) {
                val req = Request.Builder().url(url)
                    .header("Referer", RedGifsConstants.REFERER)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isRedirect) {
                        val next = resp.header("Location")?.let { url.resolve(it) }
                            ?: throw FlowException("Bad redirect")
                        url = next
                    } else {
                        html = resp.body?.string().orEmpty()
                        landed = resp.request.url
                        hops = Int.MAX_VALUE - 1
                    }
                }
            }
        } catch (e: IOException) {
            throw FlowException(e.message ?: "Network error")
        }

        val psid = Regex("psid[:=]([A-Za-z0-9]+)").find(landed.toString())?.groupValues?.get(1)
            ?: inputValue(html, "p_psid")
        val csrf = inputValue(html, "x_csrf_token")
        if (psid.isNullOrBlank() || csrf.isNullOrBlank()) {
            throw FlowException("Could not read the verification page")
        }
        Pending(client, verifier, state, psid, csrf, landed.toString())
    }

    /** Value of `<input name="[name]" value="...">`, whichever order the attributes are in. */
    private fun inputValue(html: String, name: String): String? {
        for (m in Regex("<input[^>]*>", RegexOption.IGNORE_CASE).findAll(html)) {
            val tag = m.value
            if (!Regex("""name\s*=\s*["']$name["']""").containsMatchIn(tag)) continue
            Regex("""value\s*=\s*["']([^"']*)["']""").find(tag)?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    /** Steps 4-5. Throws [FlowException] with a readable message when the code is wrong or expired. */
    suspend fun submitCode(pending: Pending, code: String): Unit = withContext(Dispatchers.IO) {
        val client = pending.client
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("x_csrf_token", pending.csrf)
            .addFormDataPart("p_psid", pending.psid)
            .addFormDataPart("p_confirmation_code", code.trim())
            .build()
        val req = Request.Builder()
            .url("${RedGifsConstants.AUTH_BASE}/end_user_pages/widgets/partials/otp/otp_code_form")
            .post(form)
            .header("Origin", RedGifsConstants.AUTH_BASE)
            .header("Referer", pending.pageUrl)
            .build()
        val location: String = try {
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val header = resp.header("Location")
                val inner = runCatching {
                    val outer = json.parseToJsonElement(text).jsonObject
                    (outer["json"] as? JsonPrimitive)?.contentOrNull?.let { json.parseToJsonElement(it).jsonObject }
                }.getOrNull()
                val action = (inner?.get("action") as? JsonPrimitive)?.contentOrNull
                val loc = (inner?.get("location") as? JsonPrimitive)?.contentOrNull ?: header
                if (loc.isNullOrBlank() || (action != null && action != "redirect")) {
                    throw FlowException("Invalid or expired code")
                }
                loc
            }
        } catch (e: IOException) {
            throw FlowException(e.message ?: "Network error")
        }

        var url: HttpUrl = location.toHttpUrl()
        var authCode: String? = null
        var returnedState: String? = null
        try {
            var hops = 0
            while (hops++ < 8 && authCode == null) {
                if (url.toString().startsWith(RedGifsConstants.ORIGIN)) {
                    authCode = url.queryParameter("code")
                    returnedState = url.queryParameter("state")
                    break
                }
                val get = Request.Builder().url(url).header("Referer", pending.pageUrl).build()
                client.newCall(get).execute().use { resp ->
                    val next = resp.header("Location")
                    if (resp.isRedirect && next != null) {
                        url = url.resolve(next) ?: throw FlowException("Bad redirect")
                    } else {
                        hops = Int.MAX_VALUE - 1
                    }
                }
            }
        } catch (e: IOException) {
            throw FlowException(e.message ?: "Network error")
        }
        if (authCode.isNullOrBlank()) throw FlowException("Sign-in did not finish")
        if (returnedState != null && returnedState != pending.state) throw FlowException("State mismatch")

        val body = FormBody.Builder()
            .add("client_id", RedGifsConstants.CLIENT_ID)
            .add("code", authCode)
            .add("code_verifier", pending.verifier)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", RedGifsConstants.ORIGIN)
            .build()
        val tokenReq = Request.Builder()
            .url("${RedGifsConstants.AUTH_BASE}/oauth2/token")
            .post(body)
            .header("Origin", RedGifsConstants.ORIGIN)
            .header("Referer", RedGifsConstants.REFERER)
            .header("Accept", "application/json")
            .build()
        val (access, refresh) = try {
            client.newCall(tokenReq).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw FlowException("Token exchange failed (HTTP ${resp.code})")
                val o = json.parseToJsonElement(text).jsonObject
                ((o["access_token"] as? JsonPrimitive)?.contentOrNull.orEmpty()) to
                        (o["refresh_token"] as? JsonPrimitive)?.contentOrNull
            }
        } catch (e: IOException) {
            throw FlowException(e.message ?: "Network error")
        }
        if (!RedGifsAuth.completeLoginWithTokens(access, refresh)) throw FlowException("No access token received")
    }
}
