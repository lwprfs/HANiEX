package com.yenaly.han1meviewer.Wumaobi.ui.navigation

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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.Wumaobi.data.remote.WumaobiAuth
import com.yenaly.han1meviewer.ui.component.content.LoginGate
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.Wumaobi.ui.account.WumaobiAccountScreen
import com.yenaly.han1meviewer.Wumaobi.ui.favorites.WumaobiFavFolderScreen
import com.yenaly.han1meviewer.Wumaobi.ui.favorites.WumaobiFavoritesScreen
import com.yenaly.han1meviewer.Wumaobi.ui.history.WumaobiHistoryScreen
import com.yenaly.han1meviewer.Wumaobi.ui.home.WumaobiBrowseScreen
import com.yenaly.han1meviewer.Wumaobi.ui.home.WumaobiHomeScreen
import com.yenaly.han1meviewer.Wumaobi.ui.home.WumaobiListScreen
import com.yenaly.han1meviewer.Wumaobi.ui.search.WumaobiSearchScreen
import com.yenaly.han1meviewer.Wumaobi.ui.settings.WumaobiSettingsScreen
import com.yenaly.han1meviewer.Wumaobi.ui.video.WumaobiVideoScreen
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
object WumaobiHomeRoute : NavKey

@Serializable
object WumaobiSettingsHubRoute : NavKey

@Serializable
data class WumaobiSearchRoute(val query: String? = null) : NavKey

@Serializable
data class WumaobiVideoRoute(val videoCode: String, val resumePosition: Long = 0L) : NavKey

@Serializable
object WumaobiHistoryRoute : NavKey

@Serializable
data class WumaobiBrowseRoute(val kind: String) : NavKey

@Serializable
data class WumaobiListRoute(val title: String, val path: String, val sort: String? = null) : NavKey

@Serializable
object WumaobiAccountRoute : NavKey

@Serializable
object WumaobiFavoritesRoute : NavKey

@Serializable
data class WumaobiFavFolderRoute(val name: String) : NavKey

@Serializable
object WumaobiSettingsRoute : NavKey

@Composable
fun WumaobiNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val entryProvider = entryProvider<NavKey> {
        screen<WumaobiHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                WumaobiHomeScreen(
                    onNavigateToVideo = { code ->
                        navigator.navigateSafely(WumaobiVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navigator.navigateSafely(WumaobiSearchRoute(query))
                    },
                    onNavigateToHistory = { navigator.navigateSafely(WumaobiHistoryRoute) },
                    onNavigateToAccount = { navigator.navigateSafely(WumaobiAccountRoute) },
                )
            }
        }

        screen<WumaobiSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.WUMAOBI,
                onResetSite = { com.yenaly.han1meviewer.Wumaobi.common.WumaobiSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.wumaobi_settings_playback_title),
                        subtitle = stringResource(R.string.wumaobi_settings_playback_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(WumaobiSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.wumaobi_history_title),
                        subtitle = stringResource(R.string.wumaobi_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(WumaobiHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.WUMAOBI.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<WumaobiSettingsRoute> {
            WumaobiSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<WumaobiSearchRoute> {
            val route = it
            WumaobiSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(WumaobiVideoRoute(code))
                },
            )
        }

        screen<WumaobiVideoRoute> {
            val route = it
            WumaobiVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(WumaobiVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navigator.navigateSafely(WumaobiSearchRoute(query))
                },
                onNavigateToList = { title, path ->
                    navigator.navigateSafely(WumaobiListRoute(title, path))
                },
                onNavigateToAccount = { navigator.navigateSafely(WumaobiAccountRoute) },
            )
        }

        screen<WumaobiAccountRoute> {
            WumaobiAccountScreen(
                onBack = { navigator.goBack() },
                onOpenFavorites = { navigator.navigateSafely(WumaobiFavoritesRoute) },
            )
        }

        screen<WumaobiFavoritesRoute> {
            val auth by WumaobiAuth.state.collectAsStateWithLifecycle()
            LoginGate(
                loggedIn = auth.loggedIn,
                title = stringResource(R.string.wumaobi_favorites),
                onBack = { navigator.goBack() },
                onSignIn = { navigator.navigateSafely(WumaobiAccountRoute) },
            ) {
                WumaobiFavoritesScreen(
                    onBack = { navigator.goBack() },
                    onOpenFolder = { name -> navigator.navigateSafely(WumaobiFavFolderRoute(name)) },
                )
            }
        }

        screen<WumaobiFavFolderRoute> {
            val route = it
            val auth by WumaobiAuth.state.collectAsStateWithLifecycle()
            LoginGate(
                loggedIn = auth.loggedIn,
                title = route.name,
                onBack = { navigator.goBack() },
                onSignIn = { navigator.navigateSafely(WumaobiAccountRoute) },
            ) {
                WumaobiFavFolderScreen(
                    name = route.name,
                    onBack = { navigator.goBack() },
                    onNavigateToVideo = { code ->
                        navigator.navigateSafely(WumaobiVideoRoute(code))
                    },
                )
            }
        }

        screen<WumaobiBrowseRoute> {
            val route = it
            WumaobiBrowseScreen(
                kindKey = route.kind,
                onBack = { navigator.goBack() },
                onOpenEntity = { title, path ->
                    navigator.navigateSafely(WumaobiListRoute(title, path))
                },
            )
        }

        screen<WumaobiListRoute> {
            val route = it
            WumaobiListScreen(
                title = route.title,
                path = route.path,
                initialSort = route.sort,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(WumaobiVideoRoute(code))
                },
            )
        }

        screen<WumaobiHistoryRoute> {
            WumaobiHistoryScreen(
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(WumaobiVideoRoute(code))
                },
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.WUMAOBI,
            config = { wumaobiHubConfig(navigator, activity) },
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
