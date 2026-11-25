package com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.navigation

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
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
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
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
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
            SiteSettingsHubScreen(
                site = SiteType.FUCKINGSESSION,
                onResetSite = { com.yenaly.han1meviewer.SitesHub.FuckingSession.common.FuckingSessionSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.fuckingsession_settings_playback_title),
                        subtitle = stringResource(R.string.fuckingsession_settings_playback_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(FuckingSessionSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.fuckingsession_history_title),
                        subtitle = stringResource(R.string.fuckingsession_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(FuckingSessionHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.FUCKINGSESSION.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
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
