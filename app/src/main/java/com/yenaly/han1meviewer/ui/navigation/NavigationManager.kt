package com.yenaly.han1meviewer.ui.navigation

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.util.logId

object NavigationManager {

    private const val TAG = "NavigationManager"

    private const val SWITCH_HOME_DELAY_MS = 300L
    private const val PENDING_OPEN_POLL_MS = 150L
    private const val PENDING_OPEN_MAX_ATTEMPTS = 240

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentNavigator: Navigator? = null

    private var navigatorSite: SiteType? = null
    private var currentSiteType: SiteType? = null

    private var pendingToken = 0L

    fun initializeNavigator(navigator: Navigator, siteType: SiteType) {
        currentNavigator = navigator
        navigatorSite = siteType
        currentSiteType = siteType
        Log.d(TAG, "Initialized navigator for site: $siteType")
    }

    fun getCurrentSiteType(): SiteType = currentSiteType ?: Preferences.siteType

    fun getNavigator(): Navigator? = currentNavigator

    fun navigateToVideo(
        videoCode: String,
        path: String? = null,
        resumePosition: Long = 0L,
        autoPlay: Boolean = false,
    ) {
        val navigator = currentNavigator ?: return
        try {
            navigator.navigateSafely(
                siteVideoKey(getCurrentSiteType(), videoCode, path, resumePosition, autoPlay)
            )
        } catch (e: Exception) {
            Log.e(TAG, "navigateToVideo failed: ${e.message}")
        }
    }

    fun navigateToSearch(query: String? = null) {
        val navigator = currentNavigator ?: return
        try {
            navigator.navigateSafely(siteSearchKey(getCurrentSiteType(), query))
        } catch (e: Exception) {
            Log.e(TAG, "navigateToSearch failed: ${e.message}")
        }
    }

    fun navigateToHome() {
        val navigator = currentNavigator ?: return
        val site = getCurrentSiteType()
        Log.d(TAG, "navigateToHome called for site: $site")
        try {
            if (!isAtHome(site)) navigator.goHome()
        } catch (e: Exception) {
            Log.e(TAG, "navigateToHome failed: ${e.message}")
            navigator.goHome()
        }
    }

    fun navigateBack() {
        currentNavigator?.goBack()
    }

    fun clearBackStack() {
        currentNavigator?.goHome()
    }

    fun switchSite(newSiteType: SiteType, thenOpen: PendingVideoOpen? = null) {
        Log.d(TAG, "switchSite called: $newSiteType (pending open: ${thenOpen?.videoCode.logId()})")
        currentSiteType = newSiteType
        Preferences.siteType = newSiteType
        pendingToken++
        currentNavigator?.goHome()
        mainHandler.postDelayed({
            navigateToHome()
            if (thenOpen != null) schedulePendingOpen(thenOpen)
        }, SWITCH_HOME_DELAY_MS)
    }

    /**
     * Opens [key] on [site]'s own navigation graph. Routes are registered per site, so pushing a
     * site-specific route (e.g. OnAir's cookie page) while another site's graph is showing, such
     * as from the shared Downloads screen, throws "Unknown screen". When [site] is not the one on
     * screen this switches to it first and navigates once its graph is ready.
     */
    fun navigateOnSite(site: SiteType, key: NavKey) {
        if (isNavigationReady(site)) {
            try {
                currentNavigator?.navigateSafely(key)
            } catch (e: Exception) {
                Log.e(TAG, "navigateOnSite failed: ${e.message}")
            }
            return
        }
        switchSite(site)
        scheduleWhenHome(site, "route") {
            try {
                currentNavigator?.navigateSafely(key)
            } catch (e: Exception) {
                Log.e(TAG, "navigateOnSite failed after switch: ${e.message}")
            }
        }
    }

    private fun schedulePendingOpen(request: PendingVideoOpen) {
        scheduleWhenHome(request.site, request.videoCode.logId()) {
            navigateToVideo(
                request.videoCode,
                request.path,
                request.resumePosition,
                request.autoPlay,
            )
        }
    }

    private fun scheduleWhenHome(site: SiteType, label: String, action: () -> Unit) {
        val token = ++pendingToken
        var attempts = 0
        val poll = object : Runnable {
            override fun run() {
                if (token != pendingToken) return
                if (getCurrentSiteType() != site) {
                    Log.d(TAG, "Pending open dropped: site changed again")
                    return
                }
                if (isNavigationReady(site) && isAtHome(site)) {
                    pendingToken++
                    action()
                    return
                }
                if (++attempts >= PENDING_OPEN_MAX_ATTEMPTS) {
                    Log.w(TAG, "Pending open timed out for $label")
                    return
                }
                mainHandler.postDelayed(this, PENDING_OPEN_POLL_MS)
            }
        }
        mainHandler.postDelayed(poll, PENDING_OPEN_POLL_MS)
    }

    private fun isNavigationReady(site: SiteType): Boolean =
        currentNavigator != null && navigatorSite == site

    private fun isAtHome(site: SiteType): Boolean {
        if (navigatorSite != site) return false
        return currentNavigator?.isAt(siteHomeKey(site)) == true
    }
}
