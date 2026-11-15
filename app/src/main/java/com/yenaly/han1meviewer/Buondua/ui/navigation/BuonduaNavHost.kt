package com.yenaly.han1meviewer.Buondua.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.Buondua.ui.album.BuonduaAlbumScreen
import com.yenaly.han1meviewer.Buondua.ui.album.BuonduaPhotosScreen
import com.yenaly.han1meviewer.Buondua.ui.history.BuonduaHistoryScreen
import com.yenaly.han1meviewer.Buondua.ui.home.BuonduaHomeScreen
import com.yenaly.han1meviewer.Buondua.ui.home.BuonduaTagScreen
import com.yenaly.han1meviewer.Buondua.ui.search.BuonduaSearchScreen
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
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.registerSiteHubRoutes
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object BuonduaHomeRoute

@Serializable
object BuonduaSettingsHubRoute

@Serializable
data class BuonduaSearchRoute(val query: String? = null)

@Serializable
data class BuonduaAlbumRoute(val videoCode: String, val resumePosition: Long = 0L)

/** Photos-only screen of an album (used for albums that also have videos). */
@Serializable
data class BuonduaPhotosRoute(val code: String)

@Serializable
data class BuonduaTagRoute(val url: String, val title: String)

@Serializable
object BuonduaHistoryRoute

@Composable
fun buonduaHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.BUONDUA,
        navController = navController,
        activity = activity,
        siteSettingsRoute = BuonduaSettingsHubRoute,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.buondua_history_title),
                subtitle = stringResource(R.string.buondua_history_summary),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(BuonduaHistoryRoute) },
            ),
        ),
        customizeEntries = emptyList(),
    )

@Composable
fun BuonduaNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.BUONDUA)
    }

    NavHost(
        navController = navController,
        startDestination = BuonduaHomeRoute,
        modifier = modifier,
    ) {
        composable<BuonduaHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                BuonduaHomeScreen(
                    onNavigateToAlbum = { code -> navController.navigateSafely(BuonduaAlbumRoute(code)) },
                    onNavigateToSearch = { q -> navController.navigateSafely(BuonduaSearchRoute(q)) },
                    onNavigateToHistory = { navController.navigateSafely(BuonduaHistoryRoute) },
                )
            }
        }

        composable<BuonduaSettingsHubRoute> {
            SiteSettingsHubScreen(
                activity = activity,
                site = SiteType.BUONDUA,
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.buondua_history_title),
                        subtitle = stringResource(R.string.buondua_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(BuonduaHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.BUONDUA.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<BuonduaSearchRoute> {
            val route = it.toRoute<BuonduaSearchRoute>()
            BuonduaSearchScreen(
                initialQuery = route.query,
                onBack = { navController.popBackStackSafely() },
                onNavigateToAlbum = { code -> navController.navigateSafely(BuonduaAlbumRoute(code)) },
            )
        }

        composable<BuonduaTagRoute> {
            val route = it.toRoute<BuonduaTagRoute>()
            BuonduaTagScreen(
                url = route.url,
                title = route.title,
                onBack = { navController.popBackStackSafely() },
                onNavigateToAlbum = { code -> navController.navigateSafely(BuonduaAlbumRoute(code)) },
            )
        }

        composable<BuonduaAlbumRoute> {
            val route = it.toRoute<BuonduaAlbumRoute>()
            BuonduaAlbumScreen(
                code = route.videoCode,
                onBack = { navController.popBackStackSafely() },
                onNavigateToAlbum = { code -> navController.navigateSafely(BuonduaAlbumRoute(code)) },
                onNavigateToTag = { url, title -> navController.navigateSafely(BuonduaTagRoute(url, title)) },
                onOpenPhotos = { navController.navigateSafely(BuonduaPhotosRoute(route.videoCode)) },
            )
        }

        composable<BuonduaPhotosRoute> {
            val route = it.toRoute<BuonduaPhotosRoute>()
            BuonduaPhotosScreen(
                code = route.code,
                onBack = { navController.popBackStackSafely() },
            )
        }

        composable<BuonduaHistoryRoute> {
            BuonduaHistoryScreen(
                onNavigateToAlbum = { code -> navController.navigateSafely(BuonduaAlbumRoute(code)) },
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.BUONDUA,
            config = { buonduaHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
