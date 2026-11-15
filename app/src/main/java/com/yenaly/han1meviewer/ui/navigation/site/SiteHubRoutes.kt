package com.yenaly.han1meviewer.ui.navigation.site

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.serialization.Serializable

@Serializable
object SiteSubscriptionRoute

@Serializable
object SiteLibraryRoute

@Serializable
object SiteProfileRoute

@Serializable
object SiteCustomizeRoute

internal const val SITE_ROUTE_PKG = "com.yenaly.han1meviewer.ui.navigation.site"
const val SITE_SUBSCRIPTION_ROUTE_NAME = "$SITE_ROUTE_PKG.SiteSubscriptionRoute"
const val SITE_LIBRARY_ROUTE_NAME = "$SITE_ROUTE_PKG.SiteLibraryRoute"
const val SITE_PROFILE_ROUTE_NAME = "$SITE_ROUTE_PKG.SiteProfileRoute"

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

    }
}
