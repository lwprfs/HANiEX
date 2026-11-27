package com.yenaly.han1meviewer.SitesHub.FullVideos.ui.navigation

import com.yenaly.han1meviewer.ui.navigation.settings.SettingsRedirect
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.history.FullVideosHistoryScreen
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.home.FullVideosBrowseScreen
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.home.FullVideosHomeScreen
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.home.FullVideosListScreen
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.search.FullVideosSearchScreen
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.settings.FullVideosSettingsScreen
import com.yenaly.han1meviewer.SitesHub.FullVideos.ui.video.FullVideosVideoScreen
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object FullVideosHomeRoute : NavKey

@Serializable
object FullVideosSettingsHubRoute : NavKey

@Serializable
data class FullVideosSearchRoute(val query: String? = null) : NavKey

@Serializable
data class FullVideosVideoRoute(val videoCode: String, val resumePosition: Long = 0L) : NavKey

@Serializable
object FullVideosHistoryRoute : NavKey

@Serializable
data class FullVideosBrowseRoute(val kind: String) : NavKey

@Serializable
data class FullVideosListRoute(val title: String, val path: String) : NavKey

@Serializable
object FullVideosSettingsRoute : NavKey

@Composable
fun FullVideosNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val entryProvider = entryProvider<NavKey> {
        screen<FullVideosHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                FullVideosHomeScreen(
                    onNavigateToVideo = { code ->
                        navigator.navigateSafely(FullVideosVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navigator.navigateSafely(FullVideosSearchRoute(query))
                    },
                    onNavigateToHistory = { navigator.navigateSafely(FullVideosHistoryRoute) },
                )
            }
        }

        screen<FullVideosSettingsHubRoute> {
            SettingsRedirect(navigator)
        }

        screen<FullVideosSettingsRoute> {
            FullVideosSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<FullVideosSearchRoute> {
            val route = it
            FullVideosSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(FullVideosVideoRoute(code))
                },
            )
        }

        screen<FullVideosVideoRoute> {
            val route = it
            FullVideosVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(FullVideosVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navigator.navigateSafely(FullVideosSearchRoute(query))
                },
                onNavigateToList = { title, path ->
                    navigator.navigateSafely(FullVideosListRoute(title, path))
                },
            )
        }

        screen<FullVideosBrowseRoute> {
            val route = it
            FullVideosBrowseScreen(
                kindKey = route.kind,
                onBack = { navigator.goBack() },
                onOpenEntity = { title, path ->
                    navigator.navigateSafely(FullVideosListRoute(title, path))
                },
            )
        }

        screen<FullVideosListRoute> {
            val route = it
            FullVideosListScreen(
                title = route.title,
                path = route.path,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(FullVideosVideoRoute(code))
                },
            )
        }

        screen<FullVideosHistoryRoute> {
            FullVideosHistoryScreen(
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(FullVideosVideoRoute(code))
                },
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.FULLVIDEOS,
            config = { fullVideosHubConfig(navigator, activity) },
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
