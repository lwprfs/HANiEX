package com.yenaly.han1meviewer.FullVideos.ui.navigation

import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.screen.site.registerSiteHubRoutes
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.FullVideos.ui.history.FullVideosHistoryScreen
import com.yenaly.han1meviewer.FullVideos.ui.home.FullVideosBrowseScreen
import com.yenaly.han1meviewer.FullVideos.ui.home.FullVideosHomeScreen
import com.yenaly.han1meviewer.FullVideos.ui.home.FullVideosListScreen
import com.yenaly.han1meviewer.FullVideos.ui.search.FullVideosSearchScreen
import com.yenaly.han1meviewer.FullVideos.ui.settings.FullVideosSettingsScreen
import com.yenaly.han1meviewer.FullVideos.ui.video.FullVideosVideoScreen
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
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

@Serializable
object FullVideosHomeRoute

@Serializable
object FullVideosSettingsHubRoute

@Serializable
data class FullVideosSearchRoute(val query: String? = null)

@Serializable
data class FullVideosVideoRoute(val videoCode: String, val resumePosition: Long = 0L)

@Serializable
object FullVideosHistoryRoute

@Serializable
data class FullVideosBrowseRoute(val kind: String)

@Serializable
data class FullVideosListRoute(val title: String, val path: String)

@Serializable
object FullVideosSettingsRoute

@Composable
fun FullVideosNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.FULLVIDEOS)
    }

    NavHost(
        navController = navController,
        startDestination = FullVideosHomeRoute,
        modifier = modifier,
    ) {
        composable<FullVideosHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                FullVideosHomeScreen(
                    onNavigateToVideo = { code ->
                        navController.navigateSafely(FullVideosVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navController.navigateSafely(FullVideosSearchRoute(query))
                    },
                    onNavigateToHistory = { navController.navigateSafely(FullVideosHistoryRoute) },
                    onNavigateToBrowse = { kind ->
                        navController.navigateSafely(FullVideosBrowseRoute(kind))
                    },
                )
            }
        }

        composable<FullVideosSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.FULLVIDEOS,
                onResetSite = { com.yenaly.han1meviewer.FullVideos.common.FullVideosSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.fullvideos_settings_playback_title),
                        subtitle = stringResource(R.string.fullvideos_settings_playback_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navController.navigateSafely(FullVideosSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.fullvideos_history_title),
                        subtitle = stringResource(R.string.fullvideos_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(FullVideosHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.FULLVIDEOS.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<FullVideosSettingsRoute> {
            FullVideosSettingsScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<FullVideosSearchRoute> {
            val route = it.toRoute<FullVideosSearchRoute>()
            FullVideosSearchScreen(
                initialQuery = route.query,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(FullVideosVideoRoute(code))
                },
            )
        }

        composable<FullVideosVideoRoute> {
            val route = it.toRoute<FullVideosVideoRoute>()
            FullVideosVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(FullVideosVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navController.navigateSafely(FullVideosSearchRoute(query))
                },
                onNavigateToList = { title, path ->
                    navController.navigateSafely(FullVideosListRoute(title, path))
                },
            )
        }

        composable<FullVideosBrowseRoute> {
            val route = it.toRoute<FullVideosBrowseRoute>()
            FullVideosBrowseScreen(
                kindKey = route.kind,
                onBack = { navController.popBackStackSafely() },
                onOpenEntity = { title, path ->
                    navController.navigateSafely(FullVideosListRoute(title, path))
                },
            )
        }

        composable<FullVideosListRoute> {
            val route = it.toRoute<FullVideosListRoute>()
            FullVideosListScreen(
                title = route.title,
                path = route.path,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(FullVideosVideoRoute(code))
                },
            )
        }

        composable<FullVideosHistoryRoute> {
            FullVideosHistoryScreen(
                onNavigateToVideo = { code ->
                    navController.navigateSafely(FullVideosVideoRoute(code))
                },
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.FULLVIDEOS,
            config = { fullVideosHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
