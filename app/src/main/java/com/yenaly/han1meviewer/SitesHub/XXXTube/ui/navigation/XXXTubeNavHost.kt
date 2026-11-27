package com.yenaly.han1meviewer.SitesHub.XXXTube.ui.navigation

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
import com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.entities.XXXTubeEntityScreen
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.history.XXXTubeHistoryScreen
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.home.XXXTubeHomeScreen
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.home.XXXTubeListScreen
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.search.XXXTubeSearchScreen
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.settings.XXXTubeSettingsScreen
import com.yenaly.han1meviewer.SitesHub.XXXTube.ui.video.XXXTubeVideoScreen
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object XXXTubeHomeRoute : NavKey

@Serializable
object XXXTubeSettingsHubRoute : NavKey

@Serializable
data class XXXTubeSearchRoute(val query: String? = null) : NavKey

@Serializable
data class XXXTubeVideoRoute(val videoCode: String, val resumePosition: Long = 0L) : NavKey

@Serializable
object XXXTubeHistoryRoute : NavKey

@Serializable
data class XXXTubeListRoute(val title: String, val path: String) : NavKey

@Serializable
data class XXXTubeEntityRoute(val kind: String) : NavKey

@Serializable
object XXXTubeSettingsRoute : NavKey

@Composable
fun XXXTubeNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val entryProvider = entryProvider<NavKey> {
        screen<XXXTubeHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                XXXTubeHomeScreen(
                    onNavigateToVideo = { code ->
                        navigator.navigateSafely(XXXTubeVideoRoute(code))
                    },
                    onNavigateToSearch = { query ->
                        navigator.navigateSafely(XXXTubeSearchRoute(query))
                    },
                    onNavigateToHistory = { navigator.navigateSafely(XXXTubeHistoryRoute) },
                    onNavigateToEntities = { kind ->
                        navigator.navigateSafely(XXXTubeEntityRoute(kind.key))
                    },
                )
            }
        }

        screen<XXXTubeSettingsHubRoute> {
            SettingsRedirect(navigator)
        }

        screen<XXXTubeSettingsRoute> {
            XXXTubeSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<XXXTubeSearchRoute> {
            val route = it
            XXXTubeSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XXXTubeVideoRoute(code))
                },
                onOpenEntity = { entity ->
                    navigator.navigateSafely(XXXTubeListRoute(entity.title, entity.listingPath))
                },
            )
        }

        screen<XXXTubeEntityRoute> {
            val route = it
            XXXTubeEntityScreen(
                kind = XXXTubeConstants.EntityKind.fromKey(route.kind),
                onBack = { navigator.goBack() },
                onOpen = { entity ->
                    navigator.navigateSafely(XXXTubeListRoute(entity.title, entity.listingPath))
                },
            )
        }

        screen<XXXTubeVideoRoute> {
            val route = it
            XXXTubeVideoScreen(
                videoCode = route.videoCode,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XXXTubeVideoRoute(code))
                },
                onNavigateToSearch = { query ->
                    navigator.navigateSafely(XXXTubeSearchRoute(query))
                },
                onNavigateToList = { title, path ->
                    navigator.navigateSafely(XXXTubeListRoute(title, path))
                },
            )
        }

        screen<XXXTubeListRoute> {
            val route = it
            XXXTubeListScreen(
                title = route.title,
                path = route.path,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XXXTubeVideoRoute(code))
                },
            )
        }

        screen<XXXTubeHistoryRoute> {
            XXXTubeHistoryScreen(
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(XXXTubeVideoRoute(code))
                },
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.XXXTUBE,
            config = { xxxTubeHubConfig(navigator, activity) },
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
