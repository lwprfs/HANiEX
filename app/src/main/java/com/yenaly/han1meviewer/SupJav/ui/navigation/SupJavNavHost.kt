package com.yenaly.han1meviewer.SupJav.ui.navigation

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
import com.yenaly.han1meviewer.SupJav.ui.history.SupJavHistoryScreen
import com.yenaly.han1meviewer.SupJav.ui.home.SupJavHomeScreen
import com.yenaly.han1meviewer.SupJav.ui.search.SupJavSearchScreen
import com.yenaly.han1meviewer.SupJav.ui.settings.SupJavSettingsScreen
import com.yenaly.han1meviewer.SupJav.ui.video.SupJavVideoScreen
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
            SiteSettingsHubScreen(
                site = SiteType.SUPJAV,
                onResetSite = { com.yenaly.han1meviewer.SupJav.common.SupJavSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.supjav_settings_playback_title),
                        subtitle = stringResource(R.string.supjav_settings_playback_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(SupJavSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.supjav_history_title),
                        subtitle = stringResource(R.string.supjav_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(SupJavHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.SUPJAV.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
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
