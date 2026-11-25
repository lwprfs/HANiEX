package com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation

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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.SiteType
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
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.home.MissAvHomeScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.home.MissAvHomeSettingsScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.home.MissAvHostSettingsScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.home.MissAvQualitySettingsScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.home.MissAvSubtitleSettingsScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.search.MissAvSearchScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.video.MissAvVideoScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.history.MissAvHistoryScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvAccountScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvActressScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvActressesScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvCreatePlaylistScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvLoginScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvPlaylistScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvPlaylistsScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvSavedScreen
import com.yenaly.han1meviewer.SitesHub.MissAV.ui.account.MissAvSiteHistoryScreen

import com.yenaly.han1meviewer.SitesHub.MissAV.ui.home.MissAvHomeViewModel
import com.yenaly.han1meviewer.SitesHub.MissAV.viewmodel.MissAvViewModel
@Serializable
object MissAvHomeRoute : NavKey

@Serializable
object MissAvSettingsHubRoute : NavKey

@Serializable
object MissAvHomeCategoriesRoute : NavKey

@Serializable
object MissAvHostSettingsRoute : NavKey

@Serializable
object MissAvQualitySettingsRoute : NavKey

@Serializable
object MissAvSubtitleSettingsRoute : NavKey

@Serializable
data class MissAvSearchRoute(val query: String? = null) : NavKey

@Serializable
data class MissAvVideoRoute(val videoCode: String, val path: String) : NavKey

@Serializable
object MissAvHistoryRoute : NavKey

@Serializable
object MissAvLoginRoute : NavKey

@Serializable
object MissAvAccountRoute : NavKey

@Serializable
data class MissAvSavedRoute(val tab: Int = 0) : NavKey

@Serializable
object MissAvSiteHistoryRoute : NavKey

@Serializable
object MissAvPlaylistsRoute : NavKey

@Serializable
data class MissAvPlaylistRoute(val key: String, val name: String) : NavKey

@Serializable
object MissAvCreatePlaylistRoute : NavKey

@Serializable
object MissAvActressesRoute : NavKey

@Serializable
data class MissAvActressRoute(val path: String, val name: String) : NavKey

@Composable
fun MissAvNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val sharedMissAvViewModel: MissAvViewModel = viewModel()
    val sharedHomeViewModel: MissAvHomeViewModel = viewModel()

    val entryProvider = entryProvider<NavKey> {
        screen<MissAvHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                MissAvHomeScreen(
                    onNavigateToVideo = { code, path ->
                        navigator.navigateSafely(MissAvVideoRoute(code, path))
                    },
                    onNavigateToSearch = { query ->
                        if (!query.isNullOrBlank()) {
                            sharedMissAvViewModel.setSearchQuery(query)
                        }
                        navigator.navigateSafely(MissAvSearchRoute(query))
                    },
                    onNavigateToHistory = {
                        navigator.navigateSafely(MissAvHistoryRoute)
                    },
                    viewModel = sharedHomeViewModel,
                )
            }
        }

        screen<MissAvSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.MISSAV,
                onResetSite = {
                    com.yenaly.han1meviewer.SitesHub.MissAV.ui.home.MissAvHomeCategoryRepo.resetToDefaults()
                    com.yenaly.han1meviewer.Preferences.resetMissAvCustomization()
                },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "home_categories",
                        title = stringResource(R.string.drawer_home_categories),
                        subtitle = stringResource(R.string.site_home_categories_subtitle),
                        icon = Icons.Default.Category,
                        onClick = { navigator.navigateSafely(MissAvHomeCategoriesRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "host",
                        title = stringResource(R.string.drawer_host_mirror),
                        subtitle = stringResource(R.string.missav_host_subtitle),
                        icon = Icons.Default.Dns,
                        onClick = { navigator.navigateSafely(MissAvHostSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "video_quality",
                        title = stringResource(R.string.missav_video_resolution),
                        subtitle = stringResource(R.string.missav_choose_auto_or_a_fixed_starting),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(MissAvQualitySettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "video_subtitle",
                        title = stringResource(R.string.missav_subtitle_title),
                        subtitle = stringResource(R.string.missav_subtitle_entry_summary),
                        icon = Icons.Default.Subtitles,
                        onClick = { navigator.navigateSafely(MissAvSubtitleSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.missav_history_title),
                        subtitle = stringResource(R.string.missav_history_subtitle),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(MissAvHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.MISSAV.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<MissAvHomeCategoriesRoute> {
            MissAvHomeSettingsScreen(
                viewModel = sharedHomeViewModel,
                onBack = { navigator.goBack() },
            )
        }

        screen<MissAvHostSettingsRoute> {
            MissAvHostSettingsScreen(
                onBack = { navigator.goBack() },
            )
        }

        screen<MissAvQualitySettingsRoute> {
            MissAvQualitySettingsScreen(
                onBack = { navigator.goBack() },
            )
        }

        screen<MissAvSubtitleSettingsRoute> {
            MissAvSubtitleSettingsScreen(
                onBack = { navigator.goBack() },
            )
        }

        screen<MissAvSearchRoute> {
            val route = it
            MissAvSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code, path ->
                    navigator.navigateSafely(MissAvVideoRoute(code, path))
                },
                viewModel = sharedMissAvViewModel,
                onOpenOtherSite = { request -> activity.confirmCrossSiteOpen(request) },
            )
        }

        screen<MissAvVideoRoute> {
            val route = it
            MissAvVideoScreen(
                videoCode = route.videoCode,
                path = route.path,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code, path ->
                    navigator.navigateSafely(MissAvVideoRoute(code, "/$code"))
                },
                onNavigateToSearch = { query ->
                    if (!query.isNullOrBlank()) {
                        sharedMissAvViewModel.setSearchQuery(query)
                    }
                    navigator.navigateSafely(MissAvSearchRoute(query))
                },
                onNavigateToLogin = { navigator.navigateSafely(MissAvLoginRoute) },
                onNavigateToActress = { actressPath, actressName ->
                    navigator.navigateSafely(MissAvActressRoute(actressPath, actressName))
                },
                onNavigateToPlaylists = { navigator.navigateSafely(MissAvPlaylistsRoute) },
                viewModel = sharedMissAvViewModel,
            )
        }

        screen<MissAvLoginRoute> {
            MissAvLoginScreen(
                onBack = { navigator.goBack() },
                onLoggedIn = { navigator.goBack() },
            )
        }

        screen<MissAvAccountRoute> {
            MissAvAccountScreen(
                onBack = { navigator.goBack() },
                onLogin = { navigator.navigateSafely(MissAvLoginRoute) },
                onSaved = { navigator.navigateSafely(MissAvSavedRoute(0)) },
                onSavedActresses = { navigator.navigateSafely(MissAvSavedRoute(1)) },
                onPlaylists = { navigator.navigateSafely(MissAvPlaylistsRoute) },
                onSiteHistory = { navigator.navigateSafely(MissAvSiteHistoryRoute) },
                onLocalHistory = { navigator.navigateSafely(MissAvHistoryRoute) },
                onActresses = { navigator.navigateSafely(MissAvActressesRoute) },
            )
        }

        screen<MissAvSavedRoute> {
            val route = it
            MissAvSavedScreen(
                initialTab = route.tab,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code, path -> navigator.navigateSafely(MissAvVideoRoute(code, path)) },
                onNavigateToActress = { p, n -> navigator.navigateSafely(MissAvActressRoute(p, n)) },
                onLogin = { navigator.navigateSafely(MissAvLoginRoute) },
            )
        }

        screen<MissAvSiteHistoryRoute> {
            MissAvSiteHistoryScreen(
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code, path -> navigator.navigateSafely(MissAvVideoRoute(code, path)) },
                onLogin = { navigator.navigateSafely(MissAvLoginRoute) },
            )
        }

        screen<MissAvPlaylistsRoute> {
            MissAvPlaylistsScreen(
                onBack = { navigator.goBack() },
                onOpenPlaylist = { key, name -> navigator.navigateSafely(MissAvPlaylistRoute(key, name)) },
                onCreate = { navigator.navigateSafely(MissAvCreatePlaylistRoute) },
                onLogin = { navigator.navigateSafely(MissAvLoginRoute) },
            )
        }

        screen<MissAvPlaylistRoute> {
            val route = it
            MissAvPlaylistScreen(
                playlistKey = route.key,
                playlistName = route.name,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code, path -> navigator.navigateSafely(MissAvVideoRoute(code, path)) },
                onLogin = { navigator.navigateSafely(MissAvLoginRoute) },
            )
        }

        screen<MissAvCreatePlaylistRoute> {
            MissAvCreatePlaylistScreen(
                onBack = { navigator.goBack() },
                onLogin = { navigator.navigateSafely(MissAvLoginRoute) },
            )
        }

        screen<MissAvActressesRoute> {
            MissAvActressesScreen(
                onBack = { navigator.goBack() },
                onNavigateToActress = { p, n -> navigator.navigateSafely(MissAvActressRoute(p, n)) },
            )
        }

        screen<MissAvActressRoute> {
            val route = it
            MissAvActressScreen(
                path = route.path,
                name = route.name,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code, path -> navigator.navigateSafely(MissAvVideoRoute(code, path)) },
                onLogin = { navigator.navigateSafely(MissAvLoginRoute) },
            )
        }

        screen<MissAvHistoryRoute> {
            MissAvHistoryScreen(
                onNavigateToVideo = { code, path ->
                    navigator.navigateSafely(MissAvVideoRoute(code, path))
                },
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.MISSAV,
            config = { missAvHubConfig(navigator, activity) },
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
