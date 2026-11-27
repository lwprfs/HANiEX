package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.navigation

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryTerm
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account.LeakGalleryAccountScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account.LeakGalleryCollectionScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account.LeakGalleryLoginScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account.LeakGalleryNotificationsScreen
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.list.LeakGallerySource
import com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.shorts.LeakGalleryShortsScreen
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
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
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
data class LeakGalleryLoginRoute(val register: Boolean = false) : NavKey

@Serializable
object LeakGalleryAccountRoute : NavKey

@Serializable
data class LeakGalleryShortsRoute(val mode: String = "", val startId: String? = null) : NavKey

@Serializable
object LeakGalleryLikedRoute : NavKey

@Serializable
object LeakGalleryFollowingRoute : NavKey

@Serializable
object LeakGalleryNotificationsRoute : NavKey

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
    val goLogin = { navigator.navigateSafely(LeakGalleryLoginRoute()) }
    val openShorts: (String, String?) -> Unit =
        { mode, startId -> navigator.navigateSafely(LeakGalleryShortsRoute(mode, startId)) }

    val entryProvider = entryProvider<NavKey> {
        screen<LeakGalleryHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                LeakGalleryHomeScreen(
                    onNavigateToPost = openPost,
                    onNavigateToSearch = { navigator.navigateSafely(LeakGallerySearchRoute(it)) },
                    onNavigateToHistory = { navigator.navigateSafely(LeakGalleryHistoryRoute) },
                    onNavigateToTerm = openTerm,
                    onNavigateToShort = openShorts,
                )
            }
        }

        screen<LeakGallerySettingsHubRoute> {
            SettingsRedirect(navigator)
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
                onNavigateToShort = openShorts,
                onLogin = { goLogin() },
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
                onOpenShorts = { mode, startId -> openShorts(mode, startId) },
                onLogin = { goLogin() },
            )
        }

        screen<LeakGalleryShortsRoute> {
            val route = it
            LeakGalleryShortsScreen(
                mode = route.mode,
                startId = route.startId,
                onBack = { navigator.goBack() },
                onLogin = { goLogin() },
                onOpenCreator = { name ->
                    navigator.navigateSafely(
                        LeakGalleryListRoute(
                            path = LeakGalleryConstants.profilePath(name),
                            title = name,
                        ),
                    )
                },
            )
        }

        screen<LeakGalleryLoginRoute> {
            val route = it
            LeakGalleryLoginScreen(
                onBack = { navigator.goBack() },
                onLoggedIn = { navigator.goBack() },
                startOnRegister = route.register,
            )
        }

        screen<LeakGalleryAccountRoute> {
            LeakGalleryAccountScreen(
                onBack = { navigator.goBack() },
                onLogin = { goLogin() },
                onOpenLiked = { navigator.navigateSafely(LeakGalleryLikedRoute) },
                onOpenFollowing = { navigator.navigateSafely(LeakGalleryFollowingRoute) },
                onOpenFollowedShorts = { openShorts(LeakGalleryConstants.SHORTS_FOLLOWED, null) },
                onOpenNotifications = { navigator.navigateSafely(LeakGalleryNotificationsRoute) },
                onOpenSettings = { navigator.navigateSafely(LeakGallerySettingsRoute) },
            )
        }

        screen<LeakGalleryLikedRoute> {
            LeakGalleryCollectionScreen(
                source = LeakGallerySource.Liked,
                title = stringResource(R.string.leakgallery_liked_title),
                onBack = { navigator.goBack() },
                onLogin = { goLogin() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<LeakGalleryFollowingRoute> {
            LeakGalleryCollectionScreen(
                source = LeakGallerySource.Following,
                title = stringResource(R.string.leakgallery_following_title),
                onBack = { navigator.goBack() },
                onLogin = { goLogin() },
                onNavigateToPost = openPost,
                onNavigateToTerm = openTerm,
            )
        }

        screen<LeakGalleryNotificationsRoute> {
            LeakGalleryNotificationsScreen(
                onBack = { navigator.goBack() },
                onLogin = { goLogin() },
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
