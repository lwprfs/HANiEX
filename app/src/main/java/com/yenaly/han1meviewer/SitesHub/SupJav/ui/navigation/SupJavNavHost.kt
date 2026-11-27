package com.yenaly.han1meviewer.SitesHub.SupJav.ui.navigation

import com.yenaly.han1meviewer.ui.navigation.settings.SettingsRedirect
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.history.SupJavHistoryScreen
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.home.SupJavHomeScreen
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.search.SupJavSearchScreen
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.settings.SupJavSettingsScreen
import com.yenaly.han1meviewer.SitesHub.SupJav.ui.video.SupJavVideoScreen
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object SupJavHomeRoute : NavKey

@Serializable
object SupJavSettingsHubRoute : NavKey

@Serializable
data class SupJavSearchRoute(val query: String? = null) : NavKey

@Serializable
data class SupJavVideoRoute(val videoCode: String, val resumePosition: Long = 0L) : NavKey

@Serializable
object SupJavHistoryRoute : NavKey

@Serializable
object SupJavSettingsRoute : NavKey

@Composable
fun SupJavNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val entryProvider = entryProvider<NavKey> {
        screen<SupJavHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                SupJavHomeScreen(
                    onNavigateToVideo = { code ->
                        navigator.navigateSafely(SupJavVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navigator.navigateSafely(SupJavSearchRoute(query))
                    },
                    onNavigateToHistory = { navigator.navigateSafely(SupJavHistoryRoute) },
                )
            }
        }

        screen<SupJavSettingsHubRoute> {
            SettingsRedirect(navigator)
        }

        screen<SupJavSettingsRoute> {
            SupJavSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<SupJavSearchRoute> {
            val route = it
            SupJavSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onOpenOtherSite = { request -> activity.confirmCrossSiteOpen(request) },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(SupJavVideoRoute(code))
                },
            )
        }

        screen<SupJavVideoRoute> {
            val route = it
            SupJavVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(SupJavVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navigator.navigateSafely(SupJavSearchRoute(query))
                },
            )
        }

        screen<SupJavHistoryRoute> {
            SupJavHistoryScreen(
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(SupJavVideoRoute(code))
                },
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.SUPJAV,
            config = { supJavHubConfig(navigator, activity) },
        )

        globalSettingsEntries(
            activity = activity,
            navigator = navigator,
            homeFallback = SiteProfileRoute,
        )
    }

    SiteNavDisplay(
        navigator = navigator,
        navigationState = navigationState,
        entryProvider = entryProvider,
        modifier = modifier,
    )
}
