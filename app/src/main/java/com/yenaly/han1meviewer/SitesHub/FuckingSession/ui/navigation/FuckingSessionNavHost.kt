package com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.navigation

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
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.history.FuckingSessionHistoryScreen
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.home.FuckingSessionHomeScreen
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.home.FuckingSessionListScreen
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.search.FuckingSessionSearchScreen
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.settings.FuckingSessionSettingsScreen
import com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.video.FuckingSessionVideoScreen
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object FuckingSessionHomeRoute : NavKey

@Serializable
object FuckingSessionSettingsHubRoute : NavKey

@Serializable
data class FuckingSessionSearchRoute(val query: String? = null) : NavKey

@Serializable
data class FuckingSessionVideoRoute(val videoCode: String, val resumePosition: Long = 0L) : NavKey

@Serializable
object FuckingSessionHistoryRoute : NavKey

@Serializable
data class FuckingSessionListRoute(val title: String, val path: String) : NavKey

@Serializable
object FuckingSessionSettingsRoute : NavKey

@Composable
fun FuckingSessionNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val entryProvider = entryProvider<NavKey> {
        screen<FuckingSessionHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                FuckingSessionHomeScreen(
                    onNavigateToVideo = { code ->
                        navigator.navigateSafely(FuckingSessionVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navigator.navigateSafely(FuckingSessionSearchRoute(query))
                    },
                    onNavigateToHistory = { navigator.navigateSafely(FuckingSessionHistoryRoute) },
                )
            }
        }

        screen<FuckingSessionSettingsHubRoute> {
            SettingsRedirect(navigator)
        }

        screen<FuckingSessionSettingsRoute> {
            FuckingSessionSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<FuckingSessionSearchRoute> {
            val route = it
            FuckingSessionSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(FuckingSessionVideoRoute(code))
                },
            )
        }

        screen<FuckingSessionVideoRoute> {
            val route = it
            FuckingSessionVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(FuckingSessionVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navigator.navigateSafely(FuckingSessionSearchRoute(query))
                },
                onNavigateToList = { title, path ->
                    navigator.navigateSafely(FuckingSessionListRoute(title, path))
                },
            )
        }

        screen<FuckingSessionListRoute> {
            val route = it
            FuckingSessionListScreen(
                title = route.title,
                path = route.path,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(FuckingSessionVideoRoute(code))
                },
            )
        }

        screen<FuckingSessionHistoryRoute> {
            FuckingSessionHistoryScreen(
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(FuckingSessionVideoRoute(code))
                },
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.FUCKINGSESSION,
            config = { fuckingSessionHubConfig(navigator, activity) },
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
