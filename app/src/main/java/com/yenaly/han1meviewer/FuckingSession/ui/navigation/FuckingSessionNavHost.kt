package com.yenaly.han1meviewer.FuckingSession.ui.navigation

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
import com.yenaly.han1meviewer.FuckingSession.ui.history.FuckingSessionHistoryScreen
import com.yenaly.han1meviewer.FuckingSession.ui.home.FuckingSessionHomeScreen
import com.yenaly.han1meviewer.FuckingSession.ui.home.FuckingSessionListScreen
import com.yenaly.han1meviewer.FuckingSession.ui.search.FuckingSessionSearchScreen
import com.yenaly.han1meviewer.FuckingSession.ui.settings.FuckingSessionSettingsScreen
import com.yenaly.han1meviewer.FuckingSession.ui.video.FuckingSessionVideoScreen
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
object FuckingSessionHomeRoute

@Serializable
object FuckingSessionSettingsHubRoute

@Serializable
data class FuckingSessionSearchRoute(val query: String? = null)

@Serializable
data class FuckingSessionVideoRoute(val videoCode: String, val resumePosition: Long = 0L)

@Serializable
object FuckingSessionHistoryRoute

@Serializable
data class FuckingSessionListRoute(val title: String, val path: String)

@Serializable
object FuckingSessionSettingsRoute

@Composable
fun FuckingSessionNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.FUCKINGSESSION)
    }

    NavHost(
        navController = navController,
        startDestination = FuckingSessionHomeRoute,
        modifier = modifier,
    ) {
        composable<FuckingSessionHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                FuckingSessionHomeScreen(
                    onNavigateToVideo = { code ->
                        navController.navigateSafely(FuckingSessionVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navController.navigateSafely(FuckingSessionSearchRoute(query))
                    },
                    onNavigateToHistory = { navController.navigateSafely(FuckingSessionHistoryRoute) },
                )
            }
        }

        composable<FuckingSessionSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.FUCKINGSESSION,
                onResetSite = { com.yenaly.han1meviewer.FuckingSession.common.FuckingSessionSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.fuckingsession_settings_playback_title),
                        subtitle = stringResource(R.string.fuckingsession_settings_playback_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navController.navigateSafely(FuckingSessionSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.fuckingsession_history_title),
                        subtitle = stringResource(R.string.fuckingsession_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(FuckingSessionHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.FUCKINGSESSION.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<FuckingSessionSettingsRoute> {
            FuckingSessionSettingsScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<FuckingSessionSearchRoute> {
            val route = it.toRoute<FuckingSessionSearchRoute>()
            FuckingSessionSearchScreen(
                initialQuery = route.query,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(FuckingSessionVideoRoute(code))
                },
            )
        }

        composable<FuckingSessionVideoRoute> {
            val route = it.toRoute<FuckingSessionVideoRoute>()
            FuckingSessionVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(FuckingSessionVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navController.navigateSafely(FuckingSessionSearchRoute(query))
                },
                onNavigateToList = { title, path ->
                    navController.navigateSafely(FuckingSessionListRoute(title, path))
                },
            )
        }

        composable<FuckingSessionListRoute> {
            val route = it.toRoute<FuckingSessionListRoute>()
            FuckingSessionListScreen(
                title = route.title,
                path = route.path,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { code ->
                    navController.navigateSafely(FuckingSessionVideoRoute(code))
                },
            )
        }

        composable<FuckingSessionHistoryRoute> {
            FuckingSessionHistoryScreen(
                onNavigateToVideo = { code ->
                    navController.navigateSafely(FuckingSessionVideoRoute(code))
                },
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.FUCKINGSESSION,
            config = { fuckingSessionHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
