package com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote

import android.util.Log
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork.baseUrl
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork.service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Response

object StrawberryAuthRepo {

    private const val TAG = "StrawberryAuthRepo"

    data class CaptchaChallenge(
        val csrfToken: String,
        val honeypotName: String,
        val captchaImageUrl: String,
        val extraFields: Map<String, String> = emptyMap(),
        val formAction: String = "",
        val usernameField: String = "login",
        val passwordField: String = "password",
        val confirmField: String = "confirm",
        val captchaField: String = "captcha",
        val submitName: String = "submit",
        val submitValue: String = "Login",
        val hiddenFields: Map<String, String> = emptyMap(),
    )

    sealed interface AuthResult {
        data object Success : AuthResult
        data class FormError(val message: String) : AuthResult
        data class NetworkError(val throwable: Throwable) : AuthResult
        data object LoginRequired : AuthResult
    }

    fun fetchLoginCaptcha() = flow {
        try {
            val response = service.getRaw(baseUrl.trimEnd('/') + "/login.php")
            val html = response.checkedHtml()
            emit(parseCaptchaChallenge(html, "login"))
        } catch (e: Exception) {
            Log.e(TAG, "fetchLoginCaptcha failed", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    fun fetchRegisterCaptcha() = flow {
        try {
            val response = service.getRaw(baseUrl.trimEnd('/') + "/register")
            val html = response.checkedHtml()
            emit(parseCaptchaChallenge(html, "register"))
        } catch (e: Exception) {
            Log.e(TAG, "fetchRegisterCaptcha failed", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    fun login(
        challenge: CaptchaChallenge,
        username: String,
        password: String,
        captchaAnswer: String,
    ) = flow {
        try {
            val bodyBuilder = FormBody.Builder()
            challenge.hiddenFields.forEach { (name, value) -> bodyBuilder.add(name, value) }
            bodyBuilder
                .add(challenge.usernameField, username)
                .add(challenge.passwordField, password)
                .add(challenge.captchaField, captchaAnswer)
                .add(challenge.submitName, challenge.submitValue.ifBlank { "Login" })
            val body = bodyBuilder.build()

            val request = Request.Builder()
                .url(challenge.formAction.ifBlank { baseUrl.trimEnd('/') + "/login.php" })
                .post(body)
                .build()

            StrawberryNetwork.sharedOkHttpClient.newCall(request).execute().use { res ->
                val code = res.code
                val location = res.header("Location").orEmpty()
                val finalHtml = res.body.string().orEmpty()

                Log.d(
                    TAG,
                    "login response: code=$code, location=$location, htmlLen=${finalHtml.length}",
                )

                val looksLikeLoginPage = isLoginForm(finalHtml)
                val isAuthenticatedPage = hasAuthenticatedMarker(finalHtml)

                if (code in 200..399 && isAuthenticatedPage) {
                    saveJarSnapshot()
                    emit(AuthResult.Success)
                    return@use
                }

                if (code in 200..399 && !looksLikeLoginPage) {
                    saveJarSnapshot()
                    emit(AuthResult.Success)
                    return@use
                }

                if (code == 401 || code == 403) {
                    emit(AuthResult.LoginRequired)
                    return@use
                }

                val error = parseFormError(finalHtml)
                emit(
                    AuthResult.FormError(
                        error ?: buildFallback("Login failed", code, location)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "login failed", e)
            emit(AuthResult.NetworkError(e))
        }
    }.flowOn(Dispatchers.IO)

    fun register(
        challenge: CaptchaChallenge,
        username: String,
        password: String,
        confirm: String,
        captchaAnswer: String,
        refCode: String = "",
    ) = flow {
        try {
            val bodyBuilder = FormBody.Builder()
            challenge.hiddenFields.forEach { (name, value) -> bodyBuilder.add(name, value) }
            challenge.extraFields.forEach { (name, value) ->
                if (name !in challenge.hiddenFields) bodyBuilder.add(name, if (name == "ref_code") refCode else value)
            }
            if (challenge.honeypotName.isNotBlank()) bodyBuilder.add(challenge.honeypotName, "")
            bodyBuilder
                .add(challenge.usernameField, username)
                .add(challenge.passwordField, password)
                .add(challenge.confirmField, confirm)
                .add(challenge.captchaField, captchaAnswer)
                .add(challenge.submitName, challenge.submitValue.ifBlank { "Register" })
            val body = bodyBuilder.build()

            val request = Request.Builder()
                .url(challenge.formAction.ifBlank { baseUrl.trimEnd('/') + "/register" })
                .post(body)
                .build()

            StrawberryNetwork.sharedOkHttpClient.newCall(request).execute().use { res ->
                val code = res.code
                val location = res.header("Location").orEmpty()
                val finalHtml = res.body.string().orEmpty()

                Log.d(
                    TAG,
                    "register response: code=$code, location=$location, htmlLen=${finalHtml.length}",
                )

                val looksLikeRegisterPage = isRegisterForm(finalHtml)
                val isAuthenticatedPage = hasAuthenticatedMarker(finalHtml)

                if (code in 200..399 && isAuthenticatedPage) {
                    saveJarSnapshot()
                    emit(AuthResult.Success)
                    return@use
                }

                if (code in 200..399 && !looksLikeRegisterPage) {
                    saveJarSnapshot()
                    emit(AuthResult.Success)
                    return@use
                }

                if (code == 401 || code == 403) {
                    emit(AuthResult.LoginRequired)
                    return@use
                }

                val error = parseFormError(finalHtml)
                emit(
                    AuthResult.FormError(
                        error ?: buildFallback("Registration failed", code, location)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "register failed", e)
            emit(AuthResult.NetworkError(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun performLogout(): AuthResult {
        StrawberryCookieManager.clearCookies()
        return AuthResult.Success
    }

    fun currentUsernameHint(): String? {
        val raw = StrawberryCookieManager.cookiesRaw
        if (raw.isBlank()) return null
        val remember = StrawberryCookieManager.parseToCookies(raw, baseUrl)
            .firstOrNull { it.name.startsWith("remember_web_") }
        if (remember != null) {
            val decoded = runCatching {
                java.net.URLDecoder.decode(remember.value, "UTF-8")
            }.getOrNull()
            val m2 = decoded?.let { Regex(""""username":"([^"]+)"""").find(it) }
            if (m2 != null) return m2.groupValues[1]
        }
        return null
    }

    fun captchaImageUrl(): String = baseUrl.trimEnd('/') + "/captcha.png"

    private class FormBuilder {
        private val inner = FormBody.Builder()
        fun add(name: String, value: String) = apply { inner.add(name, value) }
        fun build(): FormBody = inner.build()
    }

    private fun Response<ResponseBody>.checkedHtml(): String {
        if (!isSuccessful) throw IllegalStateException("HTTP ${code()}: ${message()}")
        return body()?.string().orEmpty()
    }

    private fun saveJarSnapshot() {
        StrawberryCookieManager.replaceWithJarSnapshot()
        Log.d(
            TAG,
            "saveJarSnapshot: names=${
                StrawberryCookieManager.parseToCookies(
                    StrawberryCookieManager.cookiesRaw, baseUrl
                ).map { it.name }
            }",
        )
    }

    private fun parseCaptchaChallenge(html: String, kind: String): CaptchaChallenge {
        val doc = Jsoup.parse(html, baseUrl)
        val form = doc.selectFirst("form")
        val hiddenFields = form?.select("input[type=hidden][name]")
            ?.associate { it.attr("name") to it.attr("value") }.orEmpty()
        val csrfToken = hiddenFields["csrf_token"].orEmpty()
        val inputs = form?.select("input[name]").orEmpty()
        val passwordInputs = inputs.filter { it.attr("type").equals("password", true) }
        val usernameInput = inputs.firstOrNull {
            it.attr("type").equals("text", true) ||
                it.attr("name").contains("login", true) ||
                it.attr("name").contains("user", true)
        }
        val captchaInput = inputs.firstOrNull {
            it.attr("name").contains("captcha", true) || it.attr("name").contains("code", true)
        }
        val submitInput = inputs.firstOrNull { it.attr("type").equals("submit", true) }
        val honeypot = inputs.firstOrNull {
            it.attr("name").startsWith("hp_") || it.attr("name") in setOf("company", "website", "url")
        }?.attr("name").orEmpty()
        val refCode = inputs.firstOrNull { it.attr("name").contains("ref", true) }
            ?.attr("value").orEmpty()
        val formAction = form?.absUrl("action")?.takeIf { it.isNotBlank() }
            ?: baseUrl.trimEnd('/') + if (kind == "login") "/login.php" else "/register"
        val captchaImage = doc.selectFirst("img[src*=captcha], img[id*=captcha]")
            ?.absUrl("src")?.takeIf { it.isNotBlank() }
            ?: baseUrl.trimEnd('/') + "/captcha.php"
        return CaptchaChallenge(
            csrfToken = csrfToken,
            honeypotName = honeypot,
            captchaImageUrl = captchaImage,
            extraFields = if (refCode.isNotBlank()) mapOf("ref_code" to refCode) else emptyMap(),
            formAction = formAction,
            usernameField = usernameInput?.attr("name")?.takeIf { it.isNotBlank() }
                ?: if (kind == "login") "login" else "username",
            passwordField = passwordInputs.firstOrNull()?.attr("name")?.takeIf { it.isNotBlank() } ?: "password",
            confirmField = passwordInputs.getOrNull(1)?.attr("name")?.takeIf { it.isNotBlank() } ?: "confirm",
            captchaField = captchaInput?.attr("name")?.takeIf { it.isNotBlank() }
                ?: if (kind == "login") "captcha" else "captcha_answer",
            submitName = submitInput?.attr("name")?.takeIf { it.isNotBlank() } ?: "submit",
            submitValue = submitInput?.attr("value")?.takeIf { it.isNotBlank() }
                ?: if (kind == "login") "Login" else "Register",
            hiddenFields = hiddenFields,
        )
    }

    private fun isLoginForm(html: String): Boolean {
        if (html.isBlank()) return false
        val hasPassword = html.contains("name=\"password\"", ignoreCase = true)
        val hasLogin = html.contains("name=\"login\"", ignoreCase = true)
        if (!hasPassword || !hasLogin) return false
        val titleLogin = html.contains(">Log in<", ignoreCase = true) ||
                html.contains(">Login<", ignoreCase = true)
        val actionLogin = html.contains("login.php", ignoreCase = true)
        return titleLogin || actionLogin
    }

    private fun isRegisterForm(html: String): Boolean {
        if (html.isBlank()) return false
        val hasConfirm = html.contains("name=\"confirm\"", ignoreCase = true) ||
            Regex("<input[^>]+type=[\"']password[\"']", RegexOption.IGNORE_CASE)
                .findAll(html).count() >= 2
        if (!hasConfirm) return false
        val titleSignup = html.contains(">Sign up<", ignoreCase = true) ||
                html.contains(">Register<", ignoreCase = true)
        val actionRegister = html.contains("register", ignoreCase = true)
        return titleSignup || actionRegister
    }

    private fun hasAuthenticatedMarker(html: String): Boolean {
        if (html.isBlank()) return false
        return html.contains("summary class=\"nav-username\"", ignoreCase = true) ||
                html.contains("class=\"nav-username\"", ignoreCase = true) ||
                html.contains("action=\"/logout\"", ignoreCase = true) ||
                html.contains("/exit.php", ignoreCase = true) ||
                html.contains("/user/settings.php", ignoreCase = true)
    }

    private fun buildFallback(prefix: String, code: Int, location: String): String {
        val suffix = if (location.isNotBlank()) " → $location" else ""
        return "$prefix (HTTP $code$suffix)"
    }

    private fun parseFormError(html: String): String? {
        if (html.isBlank()) return null
        val doc = Jsoup.parse(html, baseUrl)
        val selectors = listOf(
            ".form-error",
            "p.form-error",
            ".alert-danger",
            ".invalid-feedback",
            ".text-danger",
            ".help-block",
            ".error",
            ".error-message",
            ".msg-error",
            ".field-error",
            ".flash-error",
            "#error",
            "#form-error",
        )
        for (sel in selectors) {
            doc.select(sel).map { it.text().trim() }
                .firstOrNull { it.isNotBlank() }
                ?.let { return it }
        }
        val attrCandidates = doc.select("[data-error], [data-error-message]")
        for (el in attrCandidates) {
            val txt = el.attr("data-error").ifBlank { el.attr("data-error-message") }.trim()
            if (txt.isNotBlank()) return txt
        }
        return null
    }
}
