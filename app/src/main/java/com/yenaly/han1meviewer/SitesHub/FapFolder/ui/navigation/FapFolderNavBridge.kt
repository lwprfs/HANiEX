package com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import android.util.Log
import com.yenaly.han1meviewer.SitesHub.FapFolder.FapFolderSiteType

object FapFolderNavBridge {

    private const val TAG = "FapFolderNavBridge"

    @Volatile
    private var currentNavigator: Navigator? = null

    @Volatile
    private var isActive: Boolean = false

    fun attach(navigator: Navigator) {
        currentNavigator = navigator
        isActive = true
        Log.d(TAG, "attach: navigator bound")
    }

    fun detach() {
        currentNavigator = null
        isActive = false
        Log.d(TAG, "detach: navigator released")
    }

    fun isActive(): Boolean = isActive && currentNavigator != null

    fun navigateToHome() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderHomeRoute)
        }.onFailure { Log.w(TAG, "navigateToHome failed: ${it.message}") }
    }

    fun navigateToPost(slugOrId: String, autoPlay: Boolean = false) {
        val nav = currentNavigator ?: return
        if (slugOrId.isBlank()) return
        runCatching {
            nav.navigateSingleTop(FapFolderPostRoute(slugOrId = slugOrId, autoPlay = autoPlay))
        }.onFailure { Log.w(TAG, "navigateToPost failed: ${it.message}") }
    }

    fun navigateToSearch(query: String?, isHashtag: Boolean = false) {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(
                FapFolderSearchRoute(
                    query = query,
                    isHashtag = isHashtag,
                )
            )
        }.onFailure { Log.w(TAG, "navigateToSearch failed: ${it.message}") }
    }

    fun navigateToSearchHistory() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderSearchHistoryRoute)
        }.onFailure { Log.w(TAG, "navigateToSearchHistory failed: ${it.message}") }
    }

    fun navigateToHashtag(tag: String) {
        val nav = currentNavigator ?: return
        if (tag.isBlank()) return
        runCatching {
            nav.navigateSingleTop(FapFolderHashtagRoute(tag = tag.removePrefix("#")))
        }.onFailure { Log.w(TAG, "navigateToHashtag failed: ${it.message}") }
    }

    fun navigateToGroup(slug: String, tab: String = "posts") {
        val nav = currentNavigator ?: return
        if (slug.isBlank()) return
        runCatching {
            nav.navigateSingleTop(FapFolderGroupRoute(slug = slug, tab = tab))
        }.onFailure { Log.w(TAG, "navigateToGroup failed: ${it.message}") }
    }

    fun navigateToProfile(username: String, tab: String = "posts") {
        val nav = currentNavigator ?: return
        if (username.isBlank()) return
        runCatching {
            nav.navigateSingleTop(FapFolderProfileRoute(username = username, tab = tab))
        }.onFailure { Log.w(TAG, "navigateToProfile failed: ${it.message}") }
    }

    fun navigateToAlbum(albumId: String) {
        val nav = currentNavigator ?: return
        if (albumId.isBlank()) return
        runCatching {
            nav.navigateSingleTop(FapFolderAlbumRoute(albumId = albumId))
        }.onFailure { Log.w(TAG, "navigateToAlbum failed: ${it.message}") }
    }

    fun navigateToSettings() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderSettingsRoute)
        }.onFailure { Log.w(TAG, "navigateToSettings failed: ${it.message}") }
    }

    fun navigateToHistory() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderHistoryRoute)
        }.onFailure { Log.w(TAG, "navigateToHistory failed: ${it.message}") }
    }

    fun navigateToSaved() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderSavedRoute)
        }.onFailure { Log.w(TAG, "navigateToSaved failed: ${it.message}") }
    }

    fun navigateToFollowedCommunities() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderFollowedCommunitiesRoute)
        }.onFailure { Log.w(TAG, "navigateToFollowedCommunities failed: ${it.message}") }
    }

    fun navigateToCookies() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderCookiesRoute)
        }.onFailure { Log.w(TAG, "navigateToCookies failed: ${it.message}") }
    }

    fun navigateToLogin() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderLoginRoute)
        }.onFailure { Log.w(TAG, "navigateToLogin failed: ${it.message}") }
    }

    fun navigateToGate() {
        val nav = currentNavigator ?: return
        runCatching {
            nav.navigateSingleTop(FapFolderGateRoute)
        }.onFailure { Log.w(TAG, "navigateToGate failed: ${it.message}") }
    }

    fun popBackStack(): Boolean {
        val nav = currentNavigator ?: return false
        return runCatching { nav.goBack() }.getOrDefault(false)
    }

    fun isOnAnyRoute(): Boolean = currentNavigator != null

    val siteHostname: String get() = FapFolderSiteType.defaultHostname()
    val siteBaseUrl: String get() = FapFolderSiteType.baseUrl()
}
