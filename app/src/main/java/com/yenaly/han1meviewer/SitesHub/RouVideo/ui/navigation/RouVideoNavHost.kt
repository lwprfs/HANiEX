package com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
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
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.util.translation.AppTranslation
import com.yenaly.han1meviewer.ui.navigation.settings.TranslationSettingsRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.account.RouVideoAccountScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.account.RouVideoProfileEditScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.library.RouVideoLibraryScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.library.RouVideoLibraryTab
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.categories.RouVideoCategoriesScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.history.RouVideoHistoryScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.home.RouVideoHomeScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.home.RouVideoTagScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.search.RouVideoSearchScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.settings.RouVideoHostSettingsScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.settings.RouVideoSettingsScreen
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.video.RouVideoVideoScreen
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object RouVideoHomeRoute : NavKey

@Serializable
object RouVideoSettingsHubRoute : NavKey

@Serializable
data class RouVideoSearchRoute(val query: String? = null, val tag: String? = null) : NavKey

@Serializable
data class RouVideoVideoRoute(val videoCode: String, val resumePosition: Long = 0L) : NavKey

@Serializable
data class RouVideoTagRoute(val tag: String) : NavKey

@Serializable
object RouVideoCategoriesRoute : NavKey

@Serializable
object RouVideoHistoryRoute : NavKey

@Serializable
object RouVideoSettingsRoute : NavKey

@Serializable
object RouVideoAccountRoute : NavKey

@Serializable
object RouVideoProfileEditRoute : NavKey

@Serializable
data class RouVideoLibraryRoute(val tab: String = RouVideoLibraryTab.FOLLOWS.name) : NavKey

@Serializable
object RouVideoHostSettingsRoute : NavKey

@Composable
fun RouVideoNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val entryProvider = entryProvider<NavKey> {
        screen<RouVideoHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                RouVideoHomeScreen(
                    onNavigateToVideo = { id -> navigator.navigateSafely(RouVideoVideoRoute(id)) },
                    onNavigateToSearch = { query ->
                        navigator.navigateSafely(RouVideoSearchRoute(query))
                    },
                    onNavigateToCategories = { navigator.navigateSafely(RouVideoCategoriesRoute) },
                )
            }
        }

        screen<RouVideoSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.ROUVIDEO,
                onResetSite = { com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoSettings.resetToDefaults() },
                siteEntries = listOfNotNull(
                    SiteSettingsEntry(
                        key = "account",
                        title = stringResource(R.string.rouvideo_account_title),
                        subtitle = stringResource(R.string.rouvideo_account_summary),
                        icon = Icons.Default.AccountCircle,
                        onClick = { navigator.navigateSafely(RouVideoAccountRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "library",
                        title = stringResource(R.string.rouvideo_my_library),
                        subtitle = stringResource(R.string.rouvideo_library_summary),
                        icon = Icons.Default.VideoLibrary,
                        onClick = { navigator.navigateSafely(RouVideoLibraryRoute()) },
                    ),
                    SiteSettingsEntry(
                        key = "host",
                        title = stringResource(R.string.drawer_host_mirror),
                        subtitle = stringResource(R.string.rouvideo_host_subtitle),
                        icon = Icons.Default.Dns,
                        onClick = { navigator.navigateSafely(RouVideoHostSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "site_settings",
                        title = stringResource(R.string.rouvideo_settings_title),
                        subtitle = stringResource(R.string.rouvideo_settings_summary),
                        icon = Icons.Default.Tune,
                        onClick = { navigator.navigateSafely(RouVideoSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "translation",
                        title = stringResource(R.string.rouvideo_tr_title),
                        subtitle = stringResource(R.string.rouvideo_tr_privacy),
                        icon = Icons.Default.Translate,
                        onClick = { navigator.navigateSafely(TranslationSettingsRoute) },
                    ).takeIf { AppTranslation.available },
                    SiteSettingsEntry(
                        key = "categories",
                        title = stringResource(R.string.rouvideo_categories),
                        subtitle = stringResource(R.string.rouvideo_categories_summary),
                        icon = Icons.Default.Category,
                        onClick = { navigator.navigateSafely(RouVideoCategoriesRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.rouvideo_history_title),
                        subtitle = stringResource(R.string.rouvideo_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(RouVideoHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = {
                    navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.ROUVIDEO.name))
                },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<RouVideoSettingsRoute> {
            RouVideoSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<RouVideoHostSettingsRoute> {
            RouVideoHostSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<RouVideoSearchRoute> {
            val route = it
            RouVideoSearchScreen(
                initialQuery = route.query,
                initialTag = route.tag,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { id -> navigator.navigateSafely(RouVideoVideoRoute(id)) },
            )
        }

        screen<RouVideoVideoRoute> {
            val route = it
            RouVideoVideoScreen(
                videoId = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { id -> navigator.navigateSafely(RouVideoVideoRoute(id)) },
                onNavigateToTag = { tag -> navigator.navigateSafely(RouVideoTagRoute(tag)) },
                onOpenAccount = { navigator.navigateSafely(RouVideoAccountRoute) },
            )
        }

        screen<RouVideoTagRoute> {
            val route = it
            RouVideoTagScreen(
                tag = route.tag,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { id -> navigator.navigateSafely(RouVideoVideoRoute(id)) },
                onNavigateToTag = { tag -> navigator.navigateSafely(RouVideoTagRoute(tag)) },
            )
        }

        screen<RouVideoCategoriesRoute> {
            RouVideoCategoriesScreen(
                onBack = { navigator.goBack() },
                onOpenTag = { tag -> navigator.navigateSafely(RouVideoTagRoute(tag)) },
                onSearch = { text -> navigator.navigateSafely(RouVideoSearchRoute(text)) },
            )
        }

        screen<RouVideoHistoryRoute> {
            RouVideoHistoryScreen(
                onNavigateToVideo = { id -> navigator.navigateSafely(RouVideoVideoRoute(id)) },
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.ROUVIDEO,
            config = { rouVideoHubConfig(navigator, activity) },
        )

        screen<RouVideoAccountRoute> {
            RouVideoAccountScreen(
                onBack = { navigator.goBack() },
                onOpenLibrary = { tab -> navigator.navigateSafely(RouVideoLibraryRoute(tab.name)) },
                onEditProfile = { navigator.navigateSafely(RouVideoProfileEditRoute) },
            )
        }

        screen<RouVideoProfileEditRoute> {
            RouVideoProfileEditScreen(onBack = { navigator.goBack() })
        }

        screen<RouVideoLibraryRoute> {
            val route = it
            RouVideoLibraryScreen(
                initialTab = runCatching { RouVideoLibraryTab.valueOf(route.tab) }
                    .getOrDefault(RouVideoLibraryTab.FOLLOWS),
                onBack = { navigator.goBack() },
                onNavigateToVideo = { id -> navigator.navigateSafely(RouVideoVideoRoute(id)) },
            )
        }

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
