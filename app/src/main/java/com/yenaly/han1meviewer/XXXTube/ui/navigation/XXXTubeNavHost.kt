package com.yenaly.han1meviewer.XXXTube.ui.navigation

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
import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.XXXTube.ui.entities.XXXTubeEntityScreen
import com.yenaly.han1meviewer.XXXTube.ui.history.XXXTubeHistoryScreen
import com.yenaly.han1meviewer.XXXTube.ui.home.XXXTubeHomeScreen
import com.yenaly.han1meviewer.XXXTube.ui.home.XXXTubeListScreen
import com.yenaly.han1meviewer.XXXTube.ui.search.XXXTubeSearchScreen
import com.yenaly.han1meviewer.XXXTube.ui.settings.XXXTubeSettingsScreen
import com.yenaly.han1meviewer.XXXTube.ui.video.XXXTubeVideoScreen
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
object XXXTubeHomeRoute

@Serializable
object XXXTubeSettingsHubRoute

@Serializable
data class XXXTubeSearchRoute(val query: String? = null)

@Serializable
data class XXXTubeVideoRoute(val videoCode: String, val resumePosition: Long = 0L)

@Serializable
object XXXTubeHistoryRoute

@Serializable
data class XXXTubeListRoute(val title: String, val path: String)

@Serializable
data class XXXTubeEntityRoute(val kind: String)

@Serializable
object XXXTubeSettingsRoute

@Composable
fun XXXTubeNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.XXXTUBE)
    }

    NavHost(
        navController = navController,
        startDestination = XXXTubeHomeRoute,
        modifier = modifier,
    ) {
        composable<XXXTubeHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                XXXTubeHomeScreen(
                    onNavigateToVideo = { code ->
                        navController.navigateSafely(XXXTubeVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navController.navigateSafely(XXXTubeSearchRoute(query))
                    },
                    onNavigateToHistory = { navController.navigateSafely(XXXTubeHistoryRoute) },
                    onNavigateToEntities = { kind ->
                        navController.navigateSafely(XXXTubeEntityRoute(kind.key))
                    },
                )
            }
        }

        composable<XXXTubeSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.XXXTUBE,
                onResetSite = { com.yenaly.han1meviewer.XXXTube.common.XXXTubeSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.xxxtube_settings_playback_title),
                        subtitle = stringResource(R.string.xxxtube_settings_playback_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navController.navigateSafely(XXXTubeSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.xxxtube_history_title),
                        subtitle = stringResource(R.string.xxxtube_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(XXXTubeHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.XXXTUBE.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<XXXTubeSettingsRoute> {
            XXXTubeSettingsScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<XXXTubeSearchRoute> {
            val route = it.toRoute<XXXTubeSearchRoute>()
            XXXTubeSearchScreen(
                initialQuery = route.query,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(XXXTubeVideoRoute(code))
                },
                onOpenEntity = { entity ->
                    navController.navigateSafely(XXXTubeListRoute(entity.title, entity.listingPath))
                },
            )
        }

        composable<XXXTubeEntityRoute> {
            val route = it.toRoute<XXXTubeEntityRoute>()
            XXXTubeEntityScreen(
                kind = XXXTubeConstants.EntityKind.fromKey(route.kind),
                onBack = { navController.popBackStackSafely() },
                onOpen = { entity ->
                    navController.navigateSafely(XXXTubeListRoute(entity.title, entity.listingPath))
                },
            )
        }

        composable<XXXTubeVideoRoute> {
            val route = it.toRoute<XXXTubeVideoRoute>()
            XXXTubeVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(XXXTubeVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navController.navigateSafely(XXXTubeSearchRoute(query))
                },
                onNavigateToList = { title, path ->
                    navController.navigateSafely(XXXTubeListRoute(title, path))
                },
            )
        }

        composable<XXXTubeListRoute> {
            val route = it.toRoute<XXXTubeListRoute>()
            XXXTubeListScreen(
                title = route.title,
                path = route.path,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(XXXTubeVideoRoute(code))
                },
            )
        }

        composable<XXXTubeHistoryRoute> {
            XXXTubeHistoryScreen(
                onNavigateToVideo = { code ->
                    navController.navigateSafely(XXXTubeVideoRoute(code))
                },
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.XXXTUBE,
            config = { xxxTubeHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
