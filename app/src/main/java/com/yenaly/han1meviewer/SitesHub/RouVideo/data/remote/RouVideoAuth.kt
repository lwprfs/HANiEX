package com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote

import android.webkit.CookieManager
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoConstants
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.model.RouVideoAuthState
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.model.RouVideoSessionCheck
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.model.RouVideoUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object RouVideoAuth {

    private const val KEY_USER_ID = "rouvideo_user_id"
    private const val KEY_USER_NAME = "rouvideo_user_name"
    private const val KEY_USER_EMAIL = "rouvideo_user_email"

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sp get() = Preferences.preferenceSp

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<RouVideoAuthState> = _state.asStateFlow()

    val isLoggedIn: Boolean get() = _state.value.loggedIn
    val user: RouVideoUser? get() = _state.value.user

    private fun host(): String =
        RouVideoConstants.BASE_URL.toHttpUrlOrNull()?.host ?: "rou.video"

    private fun readState(): RouVideoAuthState {
        val has = RouVideoCookieJar.hasSession(host())
        if (!has) return RouVideoAuthState()
        val id = sp.getString(KEY_USER_ID, null).orEmpty()
        val user = if (id.isNotBlank()) {
            RouVideoUser(
                id = id,
                name = sp.getString(KEY_USER_NAME, null).orEmpty(),
                email = sp.getString(KEY_USER_EMAIL, null).orEmpty(),
            )
        } else null
        return RouVideoAuthState(loggedIn = true, user = user)
    }

    fun reload() {
        _state.value = readState()
    }

    private fun saveUser(user: RouVideoUser?) {
        sp.edit().apply {
            if (user == null) {
                remove(KEY_USER_ID); remove(KEY_USER_NAME); remove(KEY_USER_EMAIL)
            } else {
                putString(KEY_USER_ID, user.id)
                putString(KEY_USER_NAME, user.name)
                putString(KEY_USER_EMAIL, user.email)
            }
        }.apply()
    }

    fun onWebLoginFinished(cookieHeader: String) {
        RouVideoCookieJar.importHeader(host(), cookieHeader)
        _state.value = RouVideoAuthState(loggedIn = RouVideoCookieJar.hasSession(host()), user = _state.value.user)
        appScope.launch { refresh() }
    }

    suspend fun refresh(): RouVideoSessionCheck {
        if (!RouVideoCookieJar.hasSession(host())) {
            clearLocal()
            return RouVideoSessionCheck.LoggedOut
        }
        val check = RouVideoAccountRepo.checkSession()
        when (check) {
            is RouVideoSessionCheck.LoggedOut -> clearLocal()
            is RouVideoSessionCheck.LoggedIn -> {
                val user = check.user ?: _state.value.user
                saveUser(user)
                _state.value = RouVideoAuthState(loggedIn = true, user = user)
            }
            is RouVideoSessionCheck.Unknown -> Unit
        }
        return check
    }

    fun onSessionRejected() {
        clearLocal()
    }

    suspend fun logout() {
        RouVideoAccountRepo.signOutRemote()
        clearLocal()
        withContext(Dispatchers.Main) { clearWebCookies() }
    }

    suspend fun updateProfileName(name: String): Result<Unit> {
        val current = user ?: return Result.failure(RouVideoNotSignedInException())
        val result = RouVideoAccountRepo.updateProfile(current, name.trim())
        if (result.isSuccess) logout()
        return result
    }

    private fun clearLocal() {
        RouVideoCookieJar.clearAuth()
        saveUser(null)
        _state.value = RouVideoAuthState()
    }

    private fun clearWebCookies() {
        runCatching {
            val manager = CookieManager.getInstance()
            listOf(
                RouVideoConstants.BASE_URL,
                "https://${RouVideoConstants.AUTH_HOST}",
            ).forEach { base ->
                manager.getCookie(base)?.split(';')?.forEach { part ->
                    val name = part.substringBefore('=').trim()
                    if (name.isNotEmpty()) {

                        listOf("/", "/auth/realms/rou/", "/auth/realms/rou").forEach { path ->
                            manager.setCookie(base, "$name=; Max-Age=0; Path=$path")
                        }
                    }
                }
            }
            manager.flush()
        }
    }
}
