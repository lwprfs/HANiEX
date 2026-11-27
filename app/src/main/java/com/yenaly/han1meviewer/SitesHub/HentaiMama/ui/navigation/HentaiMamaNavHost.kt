package com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.navigation

import com.yenaly.han1meviewer.ui.navigation.settings.SettingsRedirect
import androidx.navigation3.runtime.NavKey
import androidx.compose.ui.Modifier
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.HentaiMama.data.remote.HentaiMamaNetwork
import com.yenaly.han1meviewer.SitesHub.HentaiMama.settings.HentaiMamaCardSettingsTarget
import com.yenaly.han1meviewer.SitesHub.HentaiMama.settings.HentaiMamaVideoSettingsScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.history.HentaiMamaHistoryScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.home.HentaiMamaHomeScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.home.HentaiMamaHomeSettingsScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.home.HentaiMamaViewModel
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.playlist.HentaiMamaPlaylistDetailScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.playlist.HentaiMamaPlaylistsScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.recent.HentaiMamaRecentEpisodesScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.search.HentaiMamaSearchScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.series.HentaiMamaSeriesScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.upcoming.HentaiMamaUpcomingScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.video.HentaiMamaVideoScreen
import com.yenaly.han1meviewer.SitesHub.HentaiMama.ui.video.HentaiMamaVideoViewModel
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object HentaiMamaHomeRoute : NavKey

@Serializable
data class HentaiMamaSearchRoute(
    val query: String? = null,
    val categoryKey: String? = null,
    val genre: String? = null,
    val order: String? = null,
    val genreSlug: String? = null,
) : NavKey

@Serializable
data class HentaiMamaVideoRoute(
    val videoCode: String,
    val path: String,
    val resumePosition: Long = 0L,
) : NavKey

@Serializable
data class HentaiMamaSeriesRoute(
    val slug: String,
) : NavKey

@Serializable
object HentaiMamaHistoryRoute : NavKey

@Serializable
object HentaiMamaUpcomingRoute : NavKey

@Serializable
object HentaiMamaRecentEpisodesRoute : NavKey

@Serializable
object HentaiMamaPlaylistsRoute : NavKey

@Serializable
data class HentaiMamaPlaylistDetailRoute(
    val playlistId: String,
) : NavKey

@Serializable
object HentaiMamaSettingsHubRoute : NavKey

@Serializable
data class HentaiMamaVideoSettingsRoute(
    val target: String = "all",
) : NavKey

@Serializable
object HentaiMamaHomeSettingsRoute : NavKey

@Composable
fun HentaiMamaNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
) {

    DisposableEffect(navigator) {
        onDispose {
            val vm: HentaiMamaViewModel? = null
            vm?.resetAll()
        }
    }

    val entryProvider = entryProvider<NavKey> {

        screen<HentaiMamaHomeRoute> {
            HentaiMamaHomeScreen(
                onNavigateToVideo = { code, path ->
                    navigator.navigateSafely(
                        HentaiMamaSeriesRoute(slug = code)
                    )
                },
                onNavigateToSearch = { query ->
                    navigator.navigateSafely(HentaiMamaSearchRoute(query = query))
                },
                onNavigateToCategorySearch = { categoryKey ->
                    navigator.navigateSafely(
                        HentaiMamaSearchRoute(categoryKey = categoryKey)
                    )
                },
                onNavigateToGenre = { slug ->
                    navigator.navigateSafely(
                        HentaiMamaSearchRoute(genreSlug = slug)
                    )
                },
                onNavigateToUpcoming = {
                    navigator.navigateSafely(HentaiMamaUpcomingRoute)
                },
                onNavigateToHome = {
                    navigator.goHome()
                },
            )
        }

        screen<HentaiMamaSearchRoute> {
            val route = it
            HentaiMamaSearchScreen(
                initialQuery = route.query,
                initialCategoryKey = route.categoryKey,
                initialGenre = route.genre,
                initialOrder = route.order,
                initialGenreSlug = route.genreSlug,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code ->
                    navigator.navigateSafely(
                        HentaiMamaSeriesRoute(slug = code)
                    )
                },
            )
        }

        screen<HentaiMamaVideoRoute> { backStackEntry ->
            val route = backStackEntry
            val videoViewModel: HentaiMamaVideoViewModel = viewModel(
                key = route.videoCode,
            )
            HentaiMamaVideoScreen(
                videoCode = route.videoCode,
                path = route.path,
                resumePosition = route.resumePosition,
                onBack = { navigator.goBack() },
                onNavigateToVideo = { code, path ->
                    if (code != route.videoCode) {
                        navigator.navigateSafely(
                            HentaiMamaVideoRoute(videoCode = code, path = path)
                        )
                    }
                },
                onNavigateToSearch = { query ->
                    navigator.navigateSafely(HentaiMamaSearchRoute(query = query))
                },
                onNavigateToSeries = { slug ->
                    navigator.navigateSafely(HentaiMamaSeriesRoute(slug = slug))
                },
                viewModel = videoViewModel,
            )
        }

        screen<HentaiMamaSeriesRoute> { backStackEntry ->
            val route = backStackEntry
            HentaiMamaSeriesScreen(
                slug = route.slug,
                onBack = { navigator.goBack() },
                onOpenEpisode = { code, url ->
                    navigator.navigateSafely(
                        HentaiMamaVideoRoute(videoCode = code, path = url)
                    )
                },
                onOpenRelatedSeries = { relatedSlug ->
                    if (relatedSlug != route.slug) {
                        navigator.navigateSafely(
                            HentaiMamaSeriesRoute(slug = relatedSlug)
                        )
                    }
                },
                onGenreClick = { genre ->
                    navigator.navigateSafely(HentaiMamaSearchRoute(query = genre))
                },
            )
        }

        screen<HentaiMamaHistoryRoute> {
            HentaiMamaHistoryScreen(
                onBack = { navigator.goBack() },
                onNavigateToSeries = { videoCode, lastEpisodeUrl, resumePosition ->
                    navigator.navigateSafely(
                        HentaiMamaVideoRoute(
                            videoCode = videoCode,
                            path = lastEpisodeUrl,
                            resumePosition = resumePosition,
                        )
                    )
                },
                onBrowseHome = {
                    navigator.goHome()
                },
            )
        }

        screen<HentaiMamaUpcomingRoute> {
            HentaiMamaUpcomingScreen(
                onBack = { navigator.goBack() },
                onOpenEpisode = { slug, url ->
                    navigator.navigateSafely(
                        HentaiMamaVideoRoute(videoCode = slug, path = url)
                    )
                },
                onOpenSeries = { slug ->
                    navigator.navigateSafely(
                        HentaiMamaSeriesRoute(slug = slug)
                    )
                },
                onOpenStudio = { studioSlug ->
                    navigator.navigateSafely(
                        HentaiMamaSearchRoute(query = studioSlug)
                    )
                },
                onNavigateToCardSettings = {
                    navigator.navigateSafely(
                        HentaiMamaVideoSettingsRoute(target = "upcoming")
                    )
                },
            )
        }

        screen<HentaiMamaRecentEpisodesRoute> {
            HentaiMamaRecentEpisodesScreen(
                onBack = { navigator.goBack() },
                onOpenEpisode = { slug, url ->
                    navigator.navigateSafely(
                        HentaiMamaVideoRoute(videoCode = slug, path = url)
                    )
                },
                onNavigateToCardSettings = {
                    navigator.navigateSafely(
                        HentaiMamaVideoSettingsRoute(target = "recent")
                    )
                },
            )
        }

        screen<HentaiMamaPlaylistsRoute> {
            HentaiMamaPlaylistsScreen(
                onBack = { navigator.goBack() },
                onOpenPlaylist = { playlistId ->
                    navigator.navigateSafely(
                        HentaiMamaPlaylistDetailRoute(playlistId = playlistId)
                    )
                },
                onPlayFirstEpisode = { _: String, episodeUrl: String ->
                    val slug = episodeUrl.trimEnd('/').substringAfterLast('/')
                    navigator.navigateSafely(
                        HentaiMamaVideoRoute(
                            videoCode = slug,
                            path = episodeUrl,
                        )
                    )
                },
            )
        }

        screen<HentaiMamaPlaylistDetailRoute> { backStackEntry ->
            val route = backStackEntry
            HentaiMamaPlaylistDetailScreen(
                playlistId = route.playlistId,
                onBack = { navigator.goBack() },
                onOpenEpisode = { _: String, episodeUrl: String, _: Int ->
                    val slug = episodeUrl.trimEnd('/').substringAfterLast('/')
                    navigator.navigateSafely(
                        HentaiMamaVideoRoute(
                            videoCode = slug,
                            path = episodeUrl,
                        )
                    )
                },
                onPlayAll = { episodeUrl ->
                    val slug = episodeUrl.trimEnd('/').substringAfterLast('/')
                    navigator.navigateSafely(
                        HentaiMamaVideoRoute(
                            videoCode = slug,
                            path = episodeUrl,
                        )
                    )
                },
                onShuffle = { episodeUrl ->
                    val slug = episodeUrl.trimEnd('/').substringAfterLast('/')
                    navigator.navigateSafely(
                        HentaiMamaVideoRoute(
                            videoCode = slug,
                            path = episodeUrl,
                        )
                    )
                },
                onOpenOwner = { ownerUrl ->
                    val username = ownerUrl.trimEnd('/').substringAfterLast('/')
                    navigator.navigateSafely(
                        HentaiMamaSearchRoute(query = username)
                    )
                },
            )
        }

        screen<HentaiMamaSettingsHubRoute> {
            SettingsRedirect(navigator)
        }

        screen<HentaiMamaVideoSettingsRoute> { backStackEntry ->
            val route = backStackEntry
            val targets: Set<HentaiMamaCardSettingsTarget> = when (route.target) {
                "recent" -> setOf(HentaiMamaCardSettingsTarget.Recent)
                "upcoming" -> setOf(HentaiMamaCardSettingsTarget.Upcoming)
                "home" -> setOf(HentaiMamaCardSettingsTarget.Home)
                else -> setOf(
                    HentaiMamaCardSettingsTarget.Home,
                    HentaiMamaCardSettingsTarget.Recent,
                    HentaiMamaCardSettingsTarget.Upcoming,
                )
            }
            HentaiMamaVideoSettingsScreen(
                onBack = { navigator.goBack() },
                targets = targets,
            )
        }

        screen<HentaiMamaHomeSettingsRoute> {
            HentaiMamaHomeSettingsScreen(
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.HENTAIMAMA,
            config = { hentaiMamaHubConfig(navigator, activity) },
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
