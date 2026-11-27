package com.yenaly.han1meviewer.SitesHub.Coser.ui.navigation

import com.yenaly.han1meviewer.ui.navigation.settings.SettingsRedirect
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.SitesHub.Coser.ui.history.CoserHistoryScreen
import com.yenaly.han1meviewer.SitesHub.Coser.ui.home.CoserHomeScreen
import com.yenaly.han1meviewer.SitesHub.Coser.ui.list.CoserListScreen
import com.yenaly.han1meviewer.SitesHub.Coser.ui.post.CoserPhotosScreen
import com.yenaly.han1meviewer.SitesHub.Coser.ui.post.CoserPostScreen
import com.yenaly.han1meviewer.SitesHub.Coser.ui.search.CoserSearchScreen
import com.yenaly.han1meviewer.SitesHub.Coser.ui.settings.CoserSettingsScreen
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object CoserHomeRoute : NavKey

@Serializable
object CoserSettingsHubRoute : NavKey

@Serializable
data class CoserSearchRoute(val query: String? = null) : NavKey

@Serializable
data class CoserPostRoute(val slug: String, val resumePosition: Long = 0L) : NavKey

@Serializable
data class CoserPhotosRoute(val slug: String) : NavKey

@Serializable
data class CoserListRoute(
    val path: String,
    val title: String,
    val collectTerms: Boolean = false,
) : NavKey

@Serializable
object CoserHistoryRoute : NavKey

@Serializable
object CoserSettingsRoute : NavKey

fun CoserTerm.toListRoute() = CoserListRoute(path = path, title = name)

@Composable
fun CoserNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val openPost: (String) -> Unit = { navigator.navigateSafely(CoserPostRoute(it)) }
    val openTerm: (CoserTerm) -> Unit = { navigator.navigateSafely(it.toListRoute()) }

    val entryProvider = entryProvider<NavKey> {
        screen<CoserHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                CoserHomeScreen(
                    onNavigateToPost = openPost,
                    onNavigateToSearch = { navigator.navigateSafely(CoserSearchRoute(it)) },
                    onNavigateToHistory = { navigator.navigateSafely(CoserHistoryRoute) },
                    onNavigateToTerm = openTerm,
                )
            }
        }

        screen<CoserSettingsHubRoute> {
            SettingsRedirect(navigator)
        }

        screen<CoserSettingsRoute> {
            CoserSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<CoserSearchRoute> {
            val route = it
            CoserSearchScreen(
                initialQuery = route.query,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<CoserListRoute> {
            val route = it
            CoserListScreen(
                path = route.path,
                title = route.title,
                collectTerms = route.collectTerms,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<CoserPostRoute> {
            val route = it
            CoserPostScreen(
                slug = route.slug,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
                onOpenPhotos = { navigator.navigateSafely(CoserPhotosRoute(route.slug)) },
            )
        }

        screen<CoserPhotosRoute> {
            val route = it
            CoserPhotosScreen(
                slug = route.slug,
                onBack = { navigator.goBack() },
            )
        }

        screen<CoserHistoryRoute> {
            CoserHistoryScreen(
                onNavigateToPost = openPost,
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.COSER,
            config = { coserHubConfig(navigator, activity) },
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
