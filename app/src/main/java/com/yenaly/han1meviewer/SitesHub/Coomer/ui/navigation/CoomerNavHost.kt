package com.yenaly.han1meviewer.SitesHub.Coomer.ui.navigation

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
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerTerm
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.history.CoomerHistoryScreen
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.home.CoomerHomeScreen
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.list.CoomerListScreen
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.post.CoomerPhotosScreen
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.post.CoomerPostScreen
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.search.CoomerSearchScreen
import com.yenaly.han1meviewer.SitesHub.Coomer.ui.settings.CoomerSettingsScreen
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
object CoomerHomeRoute : NavKey

@Serializable
object CoomerSettingsHubRoute : NavKey

@Serializable
data class CoomerSearchRoute(val query: String? = null) : NavKey

@Serializable
data class CoomerPostRoute(val slug: String, val resumePosition: Long = 0L) : NavKey

@Serializable
data class CoomerPhotosRoute(val slug: String) : NavKey

@Serializable
data class CoomerListRoute(
    val path: String,
    val title: String,
) : NavKey

@Serializable
object CoomerHistoryRoute : NavKey

@Serializable
object CoomerSettingsRoute : NavKey

fun CoomerTerm.toListRoute() = CoomerListRoute(path = path, title = name)

@Composable
fun CoomerNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val openPost: (String) -> Unit = { navigator.navigateSafely(CoomerPostRoute(it)) }
    val openTerm: (CoomerTerm) -> Unit = { navigator.navigateSafely(it.toListRoute()) }

    val entryProvider = entryProvider<NavKey> {
        screen<CoomerHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                CoomerHomeScreen(
                    onNavigateToPost = openPost,
                    onNavigateToSearch = { navigator.navigateSafely(CoomerSearchRoute(it)) },
                    onNavigateToHistory = { navigator.navigateSafely(CoomerHistoryRoute) },
                    onNavigateToTerm = openTerm,
                )
            }
        }

        screen<CoomerSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.COOMER,
                onResetSite = { com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.coomer_settings_title),
                        subtitle = stringResource(R.string.coomer_settings_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(CoomerSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "history",
                        title = stringResource(R.string.coomer_history_title),
                        subtitle = stringResource(R.string.coomer_history_summary),
                        icon = Icons.Default.History,
                        onClick = { navigator.navigateSafely(CoomerHistoryRoute) },
                    ),
                ),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.COOMER.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<CoomerSettingsRoute> {
            CoomerSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<CoomerSearchRoute> {
            val route = it
            CoomerSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<CoomerListRoute> {
            val route = it
            CoomerListScreen(
                path = route.path,
                title = route.title,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<CoomerPostRoute> {
            val route = it
            CoomerPostScreen(
                slug = route.slug,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
                onOpenPhotos = { navigator.navigateSafely(CoomerPhotosRoute(route.slug)) },
            )
        }

        screen<CoomerPhotosRoute> {
            val route = it
            CoomerPhotosScreen(
                slug = route.slug,
                onBack = { navigator.goBack() },
            )
        }

        screen<CoomerHistoryRoute> {
            CoomerHistoryScreen(
                onNavigateToPost = openPost,
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.COOMER,
            config = { coomerHubConfig(navigator, activity) },
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
