package com.yenaly.han1meviewer.MissAV.ui.navigation
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.screen.site.registerSiteHubRoutes
import com.yenaly.han1meviewer.R
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.registerGlobalSettingsRoutes
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import com.yenaly.han1meviewer.MissAV.ui.home.MissAvHomeScreen
import com.yenaly.han1meviewer.MissAV.ui.home.MissAvHomeSettingsScreen
import com.yenaly.han1meviewer.MissAV.ui.home.MissAvHostSettingsScreen
import com.yenaly.han1meviewer.MissAV.ui.home.MissAvQualitySettingsScreen
import com.yenaly.han1meviewer.MissAV.ui.home.MissAvSubtitleSettingsScreen
import com.yenaly.han1meviewer.MissAV.ui.search.MissAvSearchScreen
import com.yenaly.han1meviewer.MissAV.ui.video.MissAvVideoScreen
import com.yenaly.han1meviewer.MissAV.ui.history.MissAvHistoryScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvAccountScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvActressScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvActressesScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvCreatePlaylistScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvLoginScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvPlaylistScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvPlaylistsScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvSavedScreen
import com.yenaly.han1meviewer.MissAV.ui.account.MissAvSiteHistoryScreen

import com.yenaly.han1meviewer.MissAV.ui.home.MissAvHomeViewModel
import com.yenaly.han1meviewer.MissAV.viewmodel.MissAvViewModel
@Serializable
object MissAvHomeRoute

@Serializable
object MissAvSettingsHubRoute

@Serializable
object MissAvHomeCategoriesRoute

@Serializable
object MissAvHostSettingsRoute

@Serializable
object MissAvQualitySettingsRoute

@Serializable
object MissAvSubtitleSettingsRoute

@Serializable
data class MissAvSearchRoute(val query: String? = null)

@Serializable
data class MissAvVideoRoute(val videoCode: String, val path: String)

@Serializable
object MissAvHistoryRoute

@Serializable
object MissAvLoginRoute

@Serializable
object MissAvAccountRoute

@Serializable
data class MissAvSavedRoute(val tab: Int = 0)

@Serializable
object MissAvSiteHistoryRoute

@Serializable
object MissAvPlaylistsRoute

@Serializable
data class MissAvPlaylistRoute(val key: String, val name: String)

@Serializable
object MissAvCreatePlaylistRoute

@Serializable
object MissAvActressesRoute

@Serializable
data class MissAvActressRoute(val path: String, val name: String)

@Composable
fun MissAvNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.MISSAV)
    }

    val sharedMissAvViewModel: MissAvViewModel = viewModel()
    val sharedHomeViewModel: MissAvHomeViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = MissAvHomeRoute,
        modifier = modifier,
    ) {
        composable<MissAvHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                MissAvHomeScreen(
                    onNavigateToVideo = { code, path ->
                        navController.navigateSafely(MissAvVideoRoute(code, path))
                    },
                    onNavigateToSearch = { query ->
                        if (!query.isNullOrBlank()) {
                            sharedMissAvViewModel.setSearchQuery(query)
                        }
                        navController.navigateSafely(MissAvSearchRoute(query))
                    },
                    onNavigateToHistory = {
                        navController.navigateSafely(MissAvHistoryRoute)
                    },
                    viewModel = sharedHomeViewModel,
                )
            }
        }

        composable<MissAvSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.MISSAV,
                onResetSite = {
                    com.yenaly.han1meviewer.MissAV.ui.home.MissAvHomeCategoryRepo.resetToDefaults()
                    com.yenaly.han1meviewer.Preferences.resetMissAvCustomization()
                },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "home_categories",
                        title = stringResource(R.string.drawer_home_categories),
                        subtitle = stringResource(R.string.site_home_categories_subtitle),
                        icon = Icons.Default.Category,
                        onClick = { navController.navigateSafely(MissAvHomeCategoriesRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "host",
                        title = stringResource(R.string.drawer_host_mirror),
                        subtitle = stringResource(R.string.missav_host_subtitle),
                        icon = Icons.Default.Dns,
                        onClick = { navController.navigateSafely(MissAvHostSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "video_quality",
                        title = stringResource(R.string.missav_video_resolution),
                        subtitle = stringResource(R.string.missav_choose_auto_or_a_fixed_starting),
                        icon = Icons.Default.HighQuality,
                        onClick = { navController.navigateSafely(MissAvQualitySettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "video_subtitle",
                        title = stringResource(R.string.missav_subtitle_title),
                        subtitle = stringResource(R.string.missav_subtitle_entry_summary),
                        icon = Icons.Default.Subtitles,
                        onClick = { navController.navigateSafely(MissAvSubtitleSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.missav_history_title),
                        subtitle = stringResource(R.string.missav_history_subtitle),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(MissAvHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.MISSAV.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<MissAvHomeCategoriesRoute> {
            MissAvHomeSettingsScreen(
                viewModel = sharedHomeViewModel,
                onBack = { navController.popBackStackSafely() },
            )
        }

        composable<MissAvHostSettingsRoute> {
            MissAvHostSettingsScreen(
                onBack = { navController.popBackStackSafely() },
            )
        }

        composable<MissAvQualitySettingsRoute> {
            MissAvQualitySettingsScreen(
                onBack = { navController.popBackStackSafely() },
            )
        }

        composable<MissAvSubtitleSettingsRoute> {
            MissAvSubtitleSettingsScreen(
                onBack = { navController.popBackStackSafely() },
            )
        }

        composable<MissAvSearchRoute> {
            val route = it.toRoute<MissAvSearchRoute>()
            MissAvSearchScreen(
                initialQuery = route.query,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code, path ->
                    navController.navigateSafely(MissAvVideoRoute(code, path))
                },
                viewModel = sharedMissAvViewModel,
                onOpenOtherSite = { request -> activity.confirmCrossSiteOpen(request) },
            )
        }

        composable<MissAvVideoRoute> {
            val route = it.toRoute<MissAvVideoRoute>()
            MissAvVideoScreen(
                videoCode = route.videoCode,
                path = route.path,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code, path ->
                    navController.navigateSafely(MissAvVideoRoute(code, "/$code"))
                },
                onNavigateToSearch = { query ->
                    if (!query.isNullOrBlank()) {
                        sharedMissAvViewModel.setSearchQuery(query)
                    }
                    navController.navigateSafely(MissAvSearchRoute(query))
                },
                onNavigateToLogin = { navController.navigateSafely(MissAvLoginRoute) },
                onNavigateToActress = { actressPath, actressName ->
                    navController.navigateSafely(MissAvActressRoute(actressPath, actressName))
                },
                onNavigateToPlaylists = { navController.navigateSafely(MissAvPlaylistsRoute) },
                viewModel = sharedMissAvViewModel,
            )
        }

        composable<MissAvLoginRoute> {
            MissAvLoginScreen(
                onBack = { navController.popBackStackSafely() },
                onLoggedIn = { navController.popBackStackSafely() },
            )
        }

        composable<MissAvAccountRoute> {
            MissAvAccountScreen(
                onBack = { navController.popBackStackSafely() },
                onLogin = { navController.navigateSafely(MissAvLoginRoute) },
                onSaved = { navController.navigateSafely(MissAvSavedRoute(0)) },
                onSavedActresses = { navController.navigateSafely(MissAvSavedRoute(1)) },
                onPlaylists = { navController.navigateSafely(MissAvPlaylistsRoute) },
                onSiteHistory = { navController.navigateSafely(MissAvSiteHistoryRoute) },
                onLocalHistory = { navController.navigateSafely(MissAvHistoryRoute) },
                onActresses = { navController.navigateSafely(MissAvActressesRoute) },
            )
        }

        composable<MissAvSavedRoute> {
            val route = it.toRoute<MissAvSavedRoute>()
            MissAvSavedScreen(
                initialTab = route.tab,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code, path -> navController.navigateSafely(MissAvVideoRoute(code, path)) },
                onNavigateToActress = { p, n -> navController.navigateSafely(MissAvActressRoute(p, n)) },
                onLogin = { navController.navigateSafely(MissAvLoginRoute) },
            )
        }

        composable<MissAvSiteHistoryRoute> {
            MissAvSiteHistoryScreen(
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code, path -> navController.navigateSafely(MissAvVideoRoute(code, path)) },
                onLogin = { navController.navigateSafely(MissAvLoginRoute) },
            )
        }

        composable<MissAvPlaylistsRoute> {
            MissAvPlaylistsScreen(
                onBack = { navController.popBackStackSafely() },
                onOpenPlaylist = { key, name -> navController.navigateSafely(MissAvPlaylistRoute(key, name)) },
                onCreate = { navController.navigateSafely(MissAvCreatePlaylistRoute) },
                onLogin = { navController.navigateSafely(MissAvLoginRoute) },
            )
        }

        composable<MissAvPlaylistRoute> {
            val route = it.toRoute<MissAvPlaylistRoute>()
            MissAvPlaylistScreen(
                playlistKey = route.key,
                playlistName = route.name,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code, path -> navController.navigateSafely(MissAvVideoRoute(code, path)) },
                onLogin = { navController.navigateSafely(MissAvLoginRoute) },
            )
        }

        composable<MissAvCreatePlaylistRoute> {
            MissAvCreatePlaylistScreen(
                onBack = { navController.popBackStackSafely() },
                onLogin = { navController.navigateSafely(MissAvLoginRoute) },
            )
        }

        composable<MissAvActressesRoute> {
            MissAvActressesScreen(
                onBack = { navController.popBackStackSafely() },
                onNavigateToActress = { p, n -> navController.navigateSafely(MissAvActressRoute(p, n)) },
            )
        }

        composable<MissAvActressRoute> {
            val route = it.toRoute<MissAvActressRoute>()
            MissAvActressScreen(
                path = route.path,
                name = route.name,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code, path -> navController.navigateSafely(MissAvVideoRoute(code, path)) },
                onLogin = { navController.navigateSafely(MissAvLoginRoute) },
            )
        }

        composable<MissAvHistoryRoute> {
            MissAvHistoryScreen(
                onNavigateToVideo = { code, path ->
                    navController.navigateSafely(MissAvVideoRoute(code, path))
                },
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.MISSAV,
            config = { missAvHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
