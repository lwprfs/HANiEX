package com.yenaly.han1meviewer.Coser.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.Coser.ui.history.CoserHistoryScreen
import com.yenaly.han1meviewer.Coser.ui.home.CoserHomeScreen
import com.yenaly.han1meviewer.Coser.ui.list.CoserListScreen
import com.yenaly.han1meviewer.Coser.ui.post.CoserPhotosScreen
import com.yenaly.han1meviewer.Coser.ui.post.CoserPostScreen
import com.yenaly.han1meviewer.Coser.ui.search.CoserSearchScreen
import com.yenaly.han1meviewer.Coser.ui.settings.CoserSettingsScreen
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.registerGlobalSettingsRoutes
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import com.yenaly.han1meviewer.ui.screen.site.registerSiteHubRoutes
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object CoserHomeRoute

@Serializable
object CoserSettingsHubRoute

@Serializable
data class CoserSearchRoute(val query: String? = null)

@Serializable
data class CoserPostRoute(val slug: String, val resumePosition: Long = 0L)

@Serializable
data class CoserPhotosRoute(val slug: String)

@Serializable
data class CoserListRoute(
    val path: String,
    val title: String,
    val collectTerms: Boolean = false,
)

@Serializable
object CoserHistoryRoute

@Serializable
object CoserSettingsRoute

fun CoserTerm.toListRoute() = CoserListRoute(path = path, title = name)

@Composable
fun CoserNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.COSER)
    }

    val openPost: (String) -> Unit = { navController.navigateSafely(CoserPostRoute(it)) }
    val openTerm: (CoserTerm) -> Unit = { navController.navigateSafely(it.toListRoute()) }

    NavHost(
        navController = navController,
        startDestination = CoserHomeRoute,
        modifier = modifier,
    ) {
        composable<CoserHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                CoserHomeScreen(
                    onNavigateToPost = openPost,
                    onNavigateToSearch = { navController.navigateSafely(CoserSearchRoute(it)) },
                    onNavigateToHistory = { navController.navigateSafely(CoserHistoryRoute) },
                    onNavigateToTerm = openTerm,
                )
            }
        }

        composable<CoserSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.COSER,
                onResetSite = { com.yenaly.han1meviewer.Coser.common.CoserSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.coser_settings_title),
                        subtitle = stringResource(R.string.coser_settings_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navController.navigateSafely(CoserSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.coser_history_title),
                        subtitle = stringResource(R.string.coser_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(CoserHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.COSER.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<CoserSettingsRoute> {
            CoserSettingsScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<CoserSearchRoute> {
            val route = it.toRoute<CoserSearchRoute>()
            CoserSearchScreen(
                initialQuery = route.query,
                onBack = { navController.popBackStackSafely() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        composable<CoserListRoute> {
            val route = it.toRoute<CoserListRoute>()
            CoserListScreen(
                path = route.path,
                title = route.title,
                collectTerms = route.collectTerms,
                onBack = { navController.popBackStackSafely() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        composable<CoserPostRoute> {
            val route = it.toRoute<CoserPostRoute>()
            CoserPostScreen(
                slug = route.slug,
                resumePosition = route.resumePosition,
                onBack = { navController.popBackStackSafely() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
                onOpenPhotos = { navController.navigateSafely(CoserPhotosRoute(route.slug)) },
            )
        }

        composable<CoserPhotosRoute> {
            val route = it.toRoute<CoserPhotosRoute>()
            CoserPhotosScreen(
                slug = route.slug,
                onBack = { navController.popBackStackSafely() },
            )
        }

        composable<CoserHistoryRoute> {
            CoserHistoryScreen(
                onNavigateToPost = openPost,
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.COSER,
            config = { coserHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
