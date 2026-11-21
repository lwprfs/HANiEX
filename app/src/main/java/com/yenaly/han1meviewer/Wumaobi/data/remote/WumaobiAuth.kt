package com.yenaly.han1meviewer.Wumaobi.data.remote

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.Wumaobi.common.WumaobiConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class WumaobiAuthState(
    val loggedIn: Boolean = false,
    val userName: String = "",
)

object WumaobiAuth {

    private const val KEY_USER_NAME = "wumaobi_user_name"

    private val sp get() = Preferences.preferenceSp

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<WumaobiAuthState> = _state.asStateFlow()

    val isLoggedIn: Boolean get() = _state.value.loggedIn

    private fun host(): String =
        WumaobiConstants.BASE_URL.toHttpUrlOrNull()?.host ?: "wumaobi.com"

    private fun readState(): WumaobiAuthState {
        val has = WumaobiCookieJar.hasSession(host())
        if (!has) return WumaobiAuthState()
        return WumaobiAuthState(
            loggedIn = true,
            userName = sp.getString(KEY_USER_NAME, null).orEmpty(),
        )
    }

    /** Re-read the session (e.g. after the mirror host changed). */
    fun reload() {
        _state.value = readState()
    }

    private fun saveUserName(name: String?) {
        sp.edit().apply {
            if (name == null) remove(KEY_USER_NAME) else putString(KEY_USER_NAME, name)
        }.apply()
    }

    suspend fun login(userName: String, password: String): Result<Unit> =
        WumaobiRepo.login(userName.trim(), password).onSuccess { onSignedIn(userName.trim()) }

    suspend fun register(userName: String, password: String, mail: String): Result<Unit> =
        WumaobiRepo.register(userName.trim(), password, mail.trim())
            .onSuccess { onSignedIn(userName.trim()) }

    private fun onSignedIn(userName: String) {
        saveUserName(userName)
        _state.value = WumaobiAuthState(
            loggedIn = WumaobiCookieJar.hasSession(host()),
            userName = userName,
        )
    }

    fun onSessionRejected() {
        clearLocal()
    }

    suspend fun logout() {
        WumaobiRepo.logoutRemote()
        clearLocal()
    }

    private fun clearLocal() {
        WumaobiCookieJar.clearAuth()
        saveUserName(null)
        _state.value = WumaobiAuthState()
    }
}
