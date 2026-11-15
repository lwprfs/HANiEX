package com.yenaly.han1meviewer.MissAV.data.remote

import android.util.Log
import com.yenaly.han1meviewer.MissAV.data.model.MissAvUser
import com.yenaly.han1meviewer.MissAV.data.parser.MissAvAccountParser
import com.yenaly.han1meviewer.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

class MissAvAuthException(message: String) : Exception(message)

/** MissAV account session: login via `POST /en/api/login` (XSRF + JSON), persisted via [MissAvCookieJar]. */
object MissAvAuth {

    private const val TAG = "MissAvAuth"
    private const val PREF_USER = "missav_auth_user"

    private val _user = MutableStateFlow<MissAvUser?>(null)
    val user: StateFlow<MissAvUser?> = _user.asStateFlow()

    @Volatile
    private var restored = false

    private fun ensureRestored() {
        if (restored) return
        restored = true
        runCatching {
            val raw = Preferences.preferenceSp.getString(PREF_USER, null) ?: return
            val o = JSONObject(raw)
            val host = Preferences.missAvBaseUrl.toHttpUrl().host
            // A stored user without session cookies cannot be logged in.
            if (MissAvCookieJar.hasSessionCookies(host)) {
                _user.value = MissAvUser(
                    name = o.optString("name").takeIf { it.isNotBlank() },
                    login = o.optString("login").takeIf { it.isNotBlank() },
                )
            }
        }
    }

    fun currentUser(): MissAvUser? {
        ensureRestored()
        return _user.value
    }

    val isLoggedIn: Boolean get() = currentUser() != null

    private fun setUser(user: MissAvUser?) {
        _user.value = user
        Preferences.preferenceSp.edit().apply {
            if (user == null) remove(PREF_USER)
            else putString(
                PREF_USER,
                JSONObject().put("name", user.name ?: "").put("login", user.login ?: "").toString()
            )
        }.apply()
    }

    private fun base(): String = Preferences.missAvBaseUrl.trimEnd('/')

    suspend fun login(login: String, password: String, remember: Boolean): Result<MissAvUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureRestored()
                val service = MissAvNetwork.missAvService
                val host = base().toHttpUrl().host
                // 1) load a page so the server sets session + XSRF-TOKEN cookies
                if (MissAvCookieJar.xsrfToken(host) == null) {
                    service.getPageUrl("${base()}/en")
                }
                val payload = JSONObject()
                    .put("email", login)
                    .put("password", password)
                    .put("remember", remember)
                    .toString()
                    .toRequestBody("application/json".toMediaType())

                var response = service.postJson("${base()}/en/api/login", payload, xsrf = MissAvCookieJar.xsrfToken(host))
                if (response.code() == 419) { // CSRF token expired → refresh once
                    service.getPageUrl("${base()}/en")
                    response = service.postJson("${base()}/en/api/login", payload, xsrf = MissAvCookieJar.xsrfToken(host))
                }
                val text = (if (response.isSuccessful) response.body() else response.errorBody())?.string().orEmpty()
                when {
                    response.isSuccessful -> {
                        val dto = MissAvAccountParser.parseLogin(text)
                        val user = MissAvUser(name = dto?.user?.name, login = login)
                        if (dto?.user == null && !text.contains("user")) {
                            throw MissAvAuthException("Unexpected login response")
                        }
                        setUser(user)
                        user
                    }
                    response.code() == 429 -> throw MissAvAuthException("Too many attempts, try again later")
                    response.code() == 403 -> throw MissAvAuthException("Blocked by Cloudflare (403). Open MissAV once to pass the check, then retry")
                    else -> throw MissAvAuthException(
                        MissAvAccountParser.parseErrorMessage(text) ?: "Login failed (${response.code()})"
                    )
                }
            }.onFailure { Log.w(TAG, "login failed", it) }
        }

    fun logout() {
        MissAvCookieJar.clearSessionCookies()
        setUser(null)
    }

    /** Called when an authenticated request is rejected (401/419/redirect to login). */
    fun onSessionExpired() {
        if (_user.value != null) {
            Log.w(TAG, "Session expired")
            logout()
        }
    }

    /** Updates the account name from an `/api/.../view` response that reports `user`. */
    fun onServerUserSeen(loggedIn: Boolean) {
        if (!loggedIn && _user.value != null) onSessionExpired()
    }
}
