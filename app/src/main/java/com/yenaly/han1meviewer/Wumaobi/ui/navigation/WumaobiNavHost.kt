package com.yenaly.han1meviewer.Wumaobi.ui.navigation

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
object WumaobiHomeRoute

@Serializable
object WumaobiSettingsHubRoute

@Serializable
data class WumaobiSearchRoute(val query: String? = null)

@Serializable
data class WumaobiVideoRoute(val videoCode: String, val resumePosition: Long = 0L)

@Serializable
object WumaobiHistoryRoute

@Serializable
data class WumaobiBrowseRoute(val kind: String)

@Serializable
data class WumaobiListRoute(val title: String, val path: String, val sort: String? = null)

@Serializable
object WumaobiAccountRoute

@Serializable
object WumaobiFavoritesRoute

@Serializable
data class WumaobiFavFolderRoute(val name: String)

@Serializable
object WumaobiSettingsRoute

@Composable
fun WumaobiNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.WUMAOBI)
    }

    NavHost(
        navController = navController,
        startDestination = WumaobiHomeRoute,
        modifier = modifier,
    ) {
        composable<WumaobiHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                WumaobiHomeScreen(
                    onNavigateToVideo = { code ->
                        navController.navigateSafely(WumaobiVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navController.navigateSafely(WumaobiSearchRoute(query))
                    },
                    onNavigateToHistory = { navController.navigateSafely(WumaobiHistoryRoute) },
                    onNavigateToAccount = { navController.navigateSafely(WumaobiAccountRoute) },
                )
            }
        }

        composable<WumaobiSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.WUMAOBI,
                onResetSite = { com.yenaly.han1meviewer.Wumaobi.common.WumaobiSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.wumaobi_settings_playback_title),
                        subtitle = stringResource(R.string.wumaobi_settings_playback_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navController.navigateSafely(WumaobiSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.wumaobi_history_title),
                        subtitle = stringResource(R.string.wumaobi_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(WumaobiHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.WUMAOBI.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<WumaobiSettingsRoute> {
            WumaobiSettingsScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<WumaobiSearchRoute> {
            val route = it.toRoute<WumaobiSearchRoute>()
            WumaobiSearchScreen(
                initialQuery = route.query,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(WumaobiVideoRoute(code))
                },
            )
        }

        composable<WumaobiVideoRoute> {
            val route = it.toRoute<WumaobiVideoRoute>()
            WumaobiVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(WumaobiVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navController.navigateSafely(WumaobiSearchRoute(query))
                },
                onNavigateToList = { title, path ->
                    navController.navigateSafely(WumaobiListRoute(title, path))
                },
                onNavigateToAccount = { navController.navigateSafely(WumaobiAccountRoute) },
            )
        }

        composable<WumaobiAccountRoute> {
            WumaobiAccountScreen(
                onBack = { navController.popBackStackSafely() },
                onOpenFavorites = { navController.navigateSafely(WumaobiFavoritesRoute) },
            )
        }

        composable<WumaobiFavoritesRoute> {
            WumaobiFavoritesScreen(
                onBack = { navController.popBackStackSafely() },
                onOpenFolder = { name -> navController.navigateSafely(WumaobiFavFolderRoute(name)) },
            )
        }

        composable<WumaobiFavFolderRoute> {
            val route = it.toRoute<WumaobiFavFolderRoute>()
            WumaobiFavFolderScreen(
                name = route.name,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(WumaobiVideoRoute(code))
                },
            )
        }

        composable<WumaobiBrowseRoute> {
            val route = it.toRoute<WumaobiBrowseRoute>()
            WumaobiBrowseScreen(
                kindKey = route.kind,
                onBack = { navController.popBackStackSafely() },
                onOpenEntity = { title, path ->
                    navController.navigateSafely(WumaobiListRoute(title, path))
                },
            )
        }

        composable<WumaobiListRoute> {
            val route = it.toRoute<WumaobiListRoute>()
            WumaobiListScreen(
                title = route.title,
                path = route.path,
                initialSort = route.sort,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(WumaobiVideoRoute(code))
                },
            )
        }

        composable<WumaobiHistoryRoute> {
            WumaobiHistoryScreen(
                onNavigateToVideo = { code ->
                    navController.navigateSafely(WumaobiVideoRoute(code))
                },
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.WUMAOBI,
            config = { wumaobiHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
