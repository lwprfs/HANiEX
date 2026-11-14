package com.yenaly.han1meviewer.ui.navigation.site

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.serialization.Serializable

/**
 * Shared primary destinations for every non-Hanime site (MissAV, HentaiMama, OnAir, FapFolder,
 * XHamster, RouVideo, SupJav). Together with each site's own home route they form the unified
 * four-item navigation: Home | My Subscription | Library | My Profile.
 *
 * Every site registers these four routes in its own NavHost and fills them with site-specific
 * content through [com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig].
 */
@Serializable
object SiteSubscriptionRoute

@Serializable
object SiteLibraryRoute

@Serializable
object SiteProfileRoute

/** Secondary page opened from My Profile (not a bottom-navigation item). */
@Serializable
object SiteCustomizeRoute

internal const val SITE_ROUTE_PKG = "com.yenaly.han1meviewer.ui.navigation.site"
const val SITE_SUBSCRIPTION_ROUTE_NAME = "$SITE_ROUTE_PKG.SiteSubscriptionRoute"
const val SITE_LIBRARY_ROUTE_NAME = "$SITE_ROUTE_PKG.SiteLibraryRoute"
const val SITE_PROFILE_ROUTE_NAME = "$SITE_ROUTE_PKG.SiteProfileRoute"

/**
 * Navigates to a primary destination the way a bottom bar should: pop back to the site's start
 * destination (its home), keep a single instance and save/restore the destination's state so
 * scroll positions are not lost when switching tabs.
 */
fun NavController.navigateSiteTopLevel(route: Any) {
    val state = currentBackStackEntry?.lifecycle?.currentState
    if (state != null && !state.isAtLeast(Lifecycle.State.STARTED)) return
    try {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } catch (_: IllegalArgumentException) {
        // Destination not registered in the current site's graph (e.g. mid site-switch).
    }
}
