package com.yenaly.han1meviewer.RouVideo.ui.navigation

import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.screen.site.registerSiteHubRoutes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.util.translation.AppTranslation
import com.yenaly.han1meviewer.ui.navigation.settings.TranslationSettingsRoute
import com.yenaly.han1meviewer.RouVideo.ui.account.RouVideoAccountScreen
import com.yenaly.han1meviewer.RouVideo.ui.account.RouVideoProfileEditScreen
import com.yenaly.han1meviewer.RouVideo.ui.library.RouVideoLibraryScreen
import com.yenaly.han1meviewer.RouVideo.ui.library.RouVideoLibraryTab
import com.yenaly.han1meviewer.RouVideo.ui.categories.RouVideoCategoriesScreen
import com.yenaly.han1meviewer.RouVideo.ui.history.RouVideoHistoryScreen
import com.yenaly.han1meviewer.RouVideo.ui.home.RouVideoHomeScreen
import com.yenaly.han1meviewer.RouVideo.ui.home.RouVideoTagScreen
import com.yenaly.han1meviewer.RouVideo.ui.search.RouVideoSearchScreen
import com.yenaly.han1meviewer.RouVideo.ui.settings.RouVideoHostSettingsScreen
import com.yenaly.han1meviewer.RouVideo.ui.settings.RouVideoSettingsScreen
import com.yenaly.han1meviewer.RouVideo.ui.video.RouVideoVideoScreen
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
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object RouVideoHomeRoute

@Serializable
object RouVideoSettingsHubRoute

@Serializable
data class RouVideoSearchRoute(val query: String? = null, val tag: String? = null)

@Serializable
data class RouVideoVideoRoute(val videoCode: String, val resumePosition: Long = 0L)

@Serializable
data class RouVideoTagRoute(val tag: String)

@Serializable
object RouVideoCategoriesRoute

@Serializable
object RouVideoHistoryRoute

@Serializable
object RouVideoSettingsRoute

@Serializable
object RouVideoAccountRoute

@Serializable
object RouVideoProfileEditRoute

@Serializable
data class RouVideoLibraryRoute(val tab: String = RouVideoLibraryTab.FOLLOWS.name)

@Serializable
object RouVideoHostSettingsRoute

@Composable
fun RouVideoNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.ROUVIDEO)
    }

    NavHost(
        navController = navController,
        startDestination = RouVideoHomeRoute,
        modifier = modifier,
    ) {
        composable<RouVideoHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                RouVideoHomeScreen(
                    onNavigateToVideo = { id -> navController.navigateSafely(RouVideoVideoRoute(id)) },
                    onNavigateToSearch = { query ->
                        navController.navigateSafely(RouVideoSearchRoute(query))
                    },
                    onNavigateToCategories = { navController.navigateSafely(RouVideoCategoriesRoute) },
                )
            }
        }

        composable<RouVideoSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.ROUVIDEO,
                onResetSite = { com.yenaly.han1meviewer.RouVideo.common.RouVideoSettings.resetToDefaults() },
                siteEntries = listOfNotNull(
                    SiteSettingsEntry(
                        key = "account",
                        title = stringResource(R.string.rouvideo_account_title),
                        subtitle = stringResource(R.string.rouvideo_account_summary),
                        icon = Icons.Default.AccountCircle,
                        onClick = { navController.navigateSafely(RouVideoAccountRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "library",
                        title = stringResource(R.string.rouvideo_my_library),
                        subtitle = stringResource(R.string.rouvideo_library_summary),
                        icon = Icons.Default.VideoLibrary,
                        onClick = { navController.navigateSafely(RouVideoLibraryRoute()) },
                    ),
                    SiteSettingsEntry(
                        key = "host",
                        title = stringResource(R.string.drawer_host_mirror),
                        subtitle = stringResource(R.string.rouvideo_host_subtitle),
                        icon = Icons.Default.Dns,
                        onClick = { navController.navigateSafely(RouVideoHostSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "site_settings",
                        title = stringResource(R.string.rouvideo_settings_title),
                        subtitle = stringResource(R.string.rouvideo_settings_summary),
                        icon = Icons.Default.Tune,
                        onClick = { navController.navigateSafely(RouVideoSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "translation",
                        title = stringResource(R.string.rouvideo_tr_title),
                        subtitle = stringResource(R.string.rouvideo_tr_privacy),
                        icon = Icons.Default.Translate,
                        onClick = { navController.navigateSafely(TranslationSettingsRoute) },
                    ).takeIf { AppTranslation.available },
                    SiteSettingsEntry(
                        key = "categories",
                        title = stringResource(R.string.rouvideo_categories),
                        subtitle = stringResource(R.string.rouvideo_categories_summary),
                        icon = Icons.Default.Category,
                        onClick = { navController.navigateSafely(RouVideoCategoriesRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.rouvideo_history_title),
                        subtitle = stringResource(R.string.rouvideo_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navController.navigateSafely(RouVideoHistoryRoute) },
                    ),
                ),
                onBack = { navController.popBackStackSafely() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = {
                    navController.navigateSafely(SitePlayerSettingsRoute(SiteType.ROUVIDEO.name))
                },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        composable<RouVideoSettingsRoute> {
            RouVideoSettingsScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<RouVideoHostSettingsRoute> {
            RouVideoHostSettingsScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<RouVideoSearchRoute> {
            val route = it.toRoute<RouVideoSearchRoute>()
            RouVideoSearchScreen(
                initialQuery = route.query,
                initialTag = route.tag,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { id -> navController.navigateSafely(RouVideoVideoRoute(id)) },
            )
        }

        composable<RouVideoVideoRoute> {
            val route = it.toRoute<RouVideoVideoRoute>()
            RouVideoVideoScreen(
                videoId = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { id -> navController.navigateSafely(RouVideoVideoRoute(id)) },
                onNavigateToTag = { tag -> navController.navigateSafely(RouVideoTagRoute(tag)) },
                onOpenAccount = { navController.navigateSafely(RouVideoAccountRoute) },
            )
        }

        composable<RouVideoTagRoute> {
            val route = it.toRoute<RouVideoTagRoute>()
            RouVideoTagScreen(
                tag = route.tag,
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { id -> navController.navigateSafely(RouVideoVideoRoute(id)) },
                onNavigateToTag = { tag -> navController.navigateSafely(RouVideoTagRoute(tag)) },
            )
        }

        composable<RouVideoCategoriesRoute> {
            RouVideoCategoriesScreen(
                onBack = { navController.popBackStackSafely() },
                onOpenTag = { tag -> navController.navigateSafely(RouVideoTagRoute(tag)) },
                onSearch = { text -> navController.navigateSafely(RouVideoSearchRoute(text)) },
            )
        }

        composable<RouVideoHistoryRoute> {
            RouVideoHistoryScreen(
                onNavigateToVideo = { id -> navController.navigateSafely(RouVideoVideoRoute(id)) },
                onBack = { navController.popBackStackSafely() },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.ROUVIDEO,
            config = { rouVideoHubConfig(navController, activity) },
        )

        composable<RouVideoAccountRoute> {
            RouVideoAccountScreen(
                onBack = { navController.popBackStackSafely() },
                onOpenLibrary = { tab -> navController.navigateSafely(RouVideoLibraryRoute(tab.name)) },
                onEditProfile = { navController.navigateSafely(RouVideoProfileEditRoute) },
            )
        }

        composable<RouVideoProfileEditRoute> {
            RouVideoProfileEditScreen(onBack = { navController.popBackStackSafely() })
        }

        composable<RouVideoLibraryRoute> {
            val route = it.toRoute<RouVideoLibraryRoute>()
            RouVideoLibraryScreen(
                initialTab = runCatching { RouVideoLibraryTab.valueOf(route.tab) }
                    .getOrDefault(RouVideoLibraryTab.FOLLOWS),
                onBack = { navController.popBackStackSafely() },
                onNavigateToVideo = { id -> navController.navigateSafely(RouVideoVideoRoute(id)) },
            )
        }

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
}
