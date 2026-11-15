package com.yenaly.han1meviewer.Cosplaytele.ui.navigation

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
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.Cosplaytele.ui.history.CosplayteleHistoryScreen
import com.yenaly.han1meviewer.Cosplaytele.ui.home.CosplayteleHomeScreen
import com.yenaly.han1meviewer.Cosplaytele.ui.list.CosplayteleListScreen
import com.yenaly.han1meviewer.Cosplaytele.ui.post.CosplaytellePhotosScreen
import com.yenaly.han1meviewer.Cosplaytele.ui.post.CosplaytellePostScreen
import com.yenaly.han1meviewer.Cosplaytele.ui.search.CosplayteleSearchScreen
import com.yenaly.han1meviewer.Cosplaytele.ui.settings.CosplayteleSettingsScreen
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
object CosplayteleHomeRoute

@Serializable
object CosplayteleSettingsHubRoute

@Serializable
data class CosplayteleSearchRoute(val query: String? = null)

@Serializable
data class CosplaytelePostRoute(val slug: String, val resumePosition: Long = 0L)

@Serializable
data class CosplaytelePhotosRoute(val slug: String)

@Serializable
data class CosplayteleListRoute(
    val path: String,
    val title: String,
    val collectTerms: Boolean = false,
)

@Serializable
object CosplayteleHistoryRoute

@Serializable
object CosplayteleSettingsRoute

fun CosplayteleTerm.toListRoute() = CosplayteleListRoute(path = path, title = name)

@Composable
fun CosplayteleNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.COSPLAYTELE)
    }

    val openPost: (String) -> Unit = { navController.navigateSafely(CosplaytelePostRoute(it)) }
    val openTerm: (CosplayteleTerm) -> Unit = { navController.navigateSafely(it.toListRoute()) }

    NavHost(
        navController = navController,
        startDestination = CosplayteleHomeRoute,
        modifier = modifier,
    ) {
        composable<CosplayteleHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                CosplayteleHomeScreen(
                    onNavigateToPost = openPost,
                    onNavigateToSearch = { navController.navigateSafely(CosplayteleSearchRoute(it)) },
                    onNavigateToHistory = { navController.navigateSafely(CosplayteleHistoryRoute) },
                    onNavigateToTerm = openTerm,
                )
            }
        }

        composable<CosplayteleSettingsHubRoute> {
            SiteSettingsHubScreen(
                activity = activity,
                site = SiteType.COSPLAYTELE,
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.cosplaytele_settings_title),
                        subtitle = stringResource(R.string.cosplaytele_settings_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navController.navigateSafely(CosplayteleSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.cosplaytele_history_title),
                        subtitle = stringResource(R.string.cosplaytele_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(CosplayteleHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.COSPLAYTELE.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<CosplayteleSettingsRoute> {
            CosplayteleSettingsScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<CosplayteleSearchRoute> {
            val route = it.toRoute<CosplayteleSearchRoute>()
            CosplayteleSearchScreen(
                initialQuery = route.query,
                onBack = { navController.popBackStackSafely() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        composable<CosplayteleListRoute> {
            val route = it.toRoute<CosplayteleListRoute>()
            CosplayteleListScreen(
                path = route.path,
                title = route.title,
                collectTerms = route.collectTerms,
                onBack = { navController.popBackStackSafely() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        composable<CosplaytelePostRoute> {
            val route = it.toRoute<CosplaytelePostRoute>()
            CosplaytellePostScreen(
                slug = route.slug,
                resumePosition = route.resumePosition,
                onBack = { navController.popBackStackSafely() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
                onOpenPhotos = { navController.navigateSafely(CosplaytelePhotosRoute(route.slug)) },
            )
        }

        composable<CosplaytelePhotosRoute> {
            val route = it.toRoute<CosplaytelePhotosRoute>()
            CosplaytellePhotosScreen(
                slug = route.slug,
                onBack = { navController.popBackStackSafely() },
            )
        }

        composable<CosplayteleHistoryRoute> {
            CosplayteleHistoryScreen(
                onNavigateToPost = openPost,
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.COSPLAYTELE,
            config = { cosplayteleHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
