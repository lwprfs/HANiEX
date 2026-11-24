package com.yenaly.han1meviewer.RedGifs.ui.navigation

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
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.common.RedGifsSession
import com.yenaly.han1meviewer.RedGifs.common.RedGifsSettings
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsFeedSource
import com.yenaly.han1meviewer.RedGifs.ui.account.RedGifsAccountScreen
import com.yenaly.han1meviewer.RedGifs.ui.account.RedGifsCollectionsScreen
import com.yenaly.han1meviewer.RedGifs.ui.account.RedGifsDashboardScreen
import com.yenaly.han1meviewer.RedGifs.ui.account.RedGifsEditProfileScreen
import com.yenaly.han1meviewer.RedGifs.ui.account.RedGifsTagsScreen
import com.yenaly.han1meviewer.RedGifs.ui.account.RedGifsLoginScreen
import com.yenaly.han1meviewer.RedGifs.ui.browse.RedGifsNichesScreen
import com.yenaly.han1meviewer.RedGifs.ui.browse.RedGifsUsersMode
import com.yenaly.han1meviewer.RedGifs.ui.browse.RedGifsUsersScreen
import com.yenaly.han1meviewer.RedGifs.ui.creator.RedGifsCreatorScreen
import com.yenaly.han1meviewer.RedGifs.ui.feed.RedGifsFeedScreen
import com.yenaly.han1meviewer.RedGifs.ui.feed.RedGifsGifScreen
import com.yenaly.han1meviewer.RedGifs.ui.home.RedGifsHomeScreen
import com.yenaly.han1meviewer.RedGifs.ui.search.RedGifsSearchScreen
import com.yenaly.han1meviewer.RedGifs.ui.settings.RedGifsSettingsScreen
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.content.LoginGate
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
object RedGifsHomeRoute : NavKey

@Serializable
object RedGifsSettingsHubRoute : NavKey

@Serializable
object RedGifsSettingsRoute : NavKey

@Serializable
data class RedGifsSearchRoute(val query: String? = null) : NavKey

@Serializable
data class RedGifsGifRoute(val id: String) : NavKey

@Serializable
data class RedGifsFeedRoute(val kind: String, val arg: String = "", val title: String = "") : NavKey

@Serializable
data class RedGifsCreatorRoute(val name: String) : NavKey

@Serializable
data class RedGifsNichesRoute(val following: Boolean = false) : NavKey

@Serializable
data class RedGifsUsersRoute(val mode: String) : NavKey

@Serializable
object RedGifsAccountRoute : NavKey

@Serializable
object RedGifsLoginRoute : NavKey

@Serializable
object RedGifsEditProfileRoute : NavKey

@Serializable
object RedGifsCollectionsRoute : NavKey

@Serializable
object RedGifsTagsRoute : NavKey

@Serializable
object RedGifsDashboardRoute : NavKey

@Composable
fun RedGifsNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val goLogin = { navigator.navigateSafely(RedGifsLoginRoute) }
    val back = { navigator.goBack() }

    val sessionEpoch by RedGifsSession.epoch.collectAsState()
    val loggedIn = remember(sessionEpoch) { RedGifsAuth.isLoggedIn }

    val actions = remember(navigator) {
        RedGifsActions(
            openCreator = { name -> navigator.navigateSafely(RedGifsCreatorRoute(name)) },
            openTag = { tag -> navigator.navigateSafely(RedGifsFeedRoute("search", tag, "#$tag")) },
        )
    }

    CompositionLocalProvider(LocalRedGifsActions provides actions) {
    val entryProvider = entryProvider<NavKey> {
        screen<RedGifsHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                RedGifsHomeScreen(
                    onSearch = { navigator.navigateSafely(RedGifsSearchRoute()) },
                    onLogin = { goLogin() },
                    onOpenSettings = { navigator.navigateSafely(RedGifsSettingsRoute) },
                )
            }
        }

        screen<RedGifsSearchRoute> {
            val route = it
            RedGifsSearchScreen(
                initialQuery = route.query,
                onBack = { back() },
                onLogin = { goLogin() },
                onOpenCreator = { user -> navigator.navigateSafely(RedGifsCreatorRoute(user.username)) },
                onOpenNiche = { niche ->
                    navigator.navigateSafely(RedGifsFeedRoute("niche", niche.id, niche.name))
                },
            )
        }

        screen<RedGifsGifRoute> {
            val route = it
            RedGifsGifScreen(id = route.id, onBack = { back() })
        }

        screen<RedGifsFeedRoute> {
            val route = it
            RedGifsFeedScreen(
                source = RedGifsFeedSource.from(route.kind, route.arg),
                title = route.title.ifBlank { route.arg },
                onBack = { back() },
                onLogin = { goLogin() },
            )
        }

        screen<RedGifsCreatorRoute> {
            val route = it
            RedGifsCreatorScreen(name = route.name, onBack = { back() }, onLogin = { goLogin() })
        }

        screen<RedGifsNichesRoute> {
            val route = it
            LoginGate(
                loggedIn = loggedIn || !route.following,
                title = stringResource(R.string.redgifs_followed_niches),
                onBack = { back() },
                onSignIn = { goLogin() },
            ) {
                RedGifsNichesScreen(
                    following = route.following,
                    onBack = { back() },
                    onOpenNiche = { niche ->
                        navigator.navigateSafely(RedGifsFeedRoute("niche", niche.id, niche.name))
                    },
                )
            }
        }

        screen<RedGifsUsersRoute> {
            val route = it
            val mode = RedGifsUsersMode.entries.firstOrNull { m -> m.name == route.mode } ?: RedGifsUsersMode.CREATORS
            LoginGate(
                loggedIn = loggedIn || mode == RedGifsUsersMode.CREATORS,
                title = stringResource(
                    if (mode == RedGifsUsersMode.FOLLOWERS) R.string.redgifs_followers else R.string.redgifs_following,
                ),
                onBack = { back() },
                onSignIn = { goLogin() },
            ) {
                RedGifsUsersScreen(
                    mode = mode,
                    onBack = { back() },
                    onOpenUser = { user -> navigator.navigateSafely(RedGifsCreatorRoute(user.username)) },
                )
            }
        }

        screen<RedGifsAccountRoute> {
            RedGifsAccountScreen(
                onBack = { back() },
                onLogin = { goLogin() },
                onOpenFollowing = { navigator.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWING.name)) },
                onOpenFollowers = { navigator.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWERS.name)) },
                onOpenFollowedNiches = { navigator.navigateSafely(RedGifsNichesRoute(following = true)) },
                onOpenLiked = {
                    navigator.navigateSafely(
                        RedGifsFeedRoute("liked", "", activity.getString(R.string.redgifs_liked)),
                    )
                },
                onOpenMine = {
                    navigator.navigateSafely(
                        RedGifsFeedRoute("mine", "", activity.getString(R.string.redgifs_my_uploads)),
                    )
                },
                onOpenSettings = { navigator.navigateSafely(RedGifsSettingsRoute) },
                onOpenDashboard = { navigator.navigateSafely(RedGifsDashboardRoute) },
                onOpenEditProfile = { navigator.navigateSafely(RedGifsEditProfileRoute) },
                onOpenCollections = { navigator.navigateSafely(RedGifsCollectionsRoute) },
            )
        }

        screen<RedGifsEditProfileRoute> {
            LoginGate(
                loggedIn = loggedIn,
                title = stringResource(R.string.redgifs_edit_profile),
                onBack = { back() },
                onSignIn = { goLogin() },
            ) { RedGifsEditProfileScreen(onBack = { back() }) }
        }

        screen<RedGifsCollectionsRoute> {
            LoginGate(
                loggedIn = loggedIn,
                title = stringResource(R.string.redgifs_collections),
                onBack = { back() },
                onSignIn = { goLogin() },
            ) { RedGifsCollectionsScreen(onBack = { back() }) }
        }

        screen<RedGifsTagsRoute> {
            RedGifsTagsScreen(
                onBack = { back() },
                onOpenTag = { tag -> navigator.navigateSafely(RedGifsFeedRoute("search", tag, "#$tag")) },
            )
        }

        screen<RedGifsDashboardRoute> {
            LoginGate(
                loggedIn = loggedIn,
                title = stringResource(R.string.redgifs_dashboard),
                onBack = { back() },
                onSignIn = { goLogin() },
            ) {
                RedGifsDashboardScreen(
                    onBack = { back() },
                    onEditProfile = { navigator.navigateSafely(RedGifsEditProfileRoute) },
                    onCollections = { navigator.navigateSafely(RedGifsCollectionsRoute) },
                    onMine = {
                        navigator.navigateSafely(
                            RedGifsFeedRoute("mine", "", activity.getString(R.string.redgifs_my_uploads)),
                        )
                    },
                    onLiked = {
                        navigator.navigateSafely(
                            RedGifsFeedRoute("liked", "", activity.getString(R.string.redgifs_liked)),
                        )
                    },
                    onFollowers = { navigator.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWERS.name)) },
                    onFollowing = { navigator.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWING.name)) },
                )
            }
        }

        screen<RedGifsLoginRoute> {
            RedGifsLoginScreen(
                onBack = { back() },
                onLoggedIn = { back() },
            )
        }

        screen<RedGifsSettingsRoute> {
            RedGifsSettingsScreen(onBack = { back() })
        }

        screen<RedGifsSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.REDGIFS,
                onResetSite = { RedGifsSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.redgifs_settings_title),
                        subtitle = stringResource(R.string.redgifs_settings_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navigator.navigateSafely(RedGifsSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "account",
                        title = stringResource(R.string.redgifs_account),
                        subtitle = stringResource(R.string.redgifs_account_summary),
                        icon = Icons.Default.Person,
                        onClick = { navigator.navigateSafely(RedGifsAccountRoute) },
                    ),
                ),
                onBack = { back() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.REDGIFS.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.REDGIFS,
            config = { redGifsHubConfig(navigator, activity) },
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
        socialSite = com.yenaly.han1meviewer.SiteType.REDGIFS,
    )
    }
}
