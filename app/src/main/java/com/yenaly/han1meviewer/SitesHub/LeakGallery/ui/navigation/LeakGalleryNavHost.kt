package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

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
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.history.LeakGalleryHistoryScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.home.LeakGalleryHomeScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGalleryListScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.post.LeakGalleryPhotosScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.post.LeakGalleryPostScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.search.LeakGallerySearchScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.settings.LeakGallerySettingsScreen
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object LeakGalleryHomeRoute : NavKey

@Serializable
object LeakGallerySettingsHubRoute : NavKey

@Serializable
data class LeakGallerySearchRoute(val query: String? = null) : NavKey

@Serializable
data class LeakGalleryPostRoute(val slug: String, val resumePosition: Long = 0L) : NavKey

@Serializable
data class LeakGalleryPhotosRoute(val slug: String) : NavKey

@Serializable
data class LeakGalleryListRoute(
    val path: String,
    val title: String,
) : NavKey

@Serializable
object LeakGalleryHistoryRoute : NavKey

@Serializable
object LeakGallerySettingsRoute : NavKey

fun LeakGalleryTerm.toListRoute() = LeakGalleryListRoute(path = path, title = name)

@Composable
fun LeakGalleryNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val openPost: (String) -> Unit = { navigator.navigateSafely(LeakGalleryPostRoute(it)) }
    val openTerm: (LeakGalleryTerm) -> Unit = { navigator.navigateSafely(it.toListRoute()) }

    val entryProvider = entryProvider<NavKey> {
        screen<LeakGalleryHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                LeakGalleryHomeScreen(
                    onNavigateToPost = openPost,
                    onNavigateToSearch = { navigator.navigateSafely(LeakGallerySearchRoute(it)) },
                    onNavigateToHistory = { navigator.navigateSafely(LeakGalleryHistoryRoute) },
                    onNavigateToTerm = openTerm,
                )
            }
        }

        screen<LeakGallerySettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.LEAKGALLERY,
                onResetSite = { com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.leakgallery_settings_title),
                        subtitle = stringResource(R.string.leakgallery_settings_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(LeakGallerySettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.leakgallery_history_title),
                        subtitle = stringResource(R.string.leakgallery_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(LeakGalleryHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.LEAKGALLERY.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<LeakGallerySettingsRoute> {
            LeakGallerySettingsScreen(onBack = { navigator.goBack() })
        }

        screen<LeakGallerySearchRoute> {
            val route = it
            LeakGallerySearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<LeakGalleryListRoute> {
            val route = it
            LeakGalleryListScreen(
                path = route.path,
                title = route.title,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<LeakGalleryPostRoute> {
            val route = it
            LeakGalleryPostScreen(
                slug = route.slug,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
                onOpenPhotos = { navigator.navigateSafely(LeakGalleryPhotosRoute(route.slug)) },
            )
        }

        screen<LeakGalleryPhotosRoute> {
            val route = it
            LeakGalleryPhotosScreen(
                slug = route.slug,
                onBack = { navigator.goBack() },
            )
        }

        screen<LeakGalleryHistoryRoute> {
            LeakGalleryHistoryScreen(
                onNavigateToPost = openPost,
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.LEAKGALLERY,
            config = { leakgalleryHubConfig(navigator, activity) },
        )

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
