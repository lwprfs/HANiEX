package com.yenaly.han1meviewer.RedGifs.ui.navigation

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.registerGlobalSettingsRoutes
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import com.yenaly.han1meviewer.ui.screen.site.registerSiteHubRoutes
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object RedGifsHomeRoute

@Serializable
object RedGifsSettingsHubRoute

@Serializable
object RedGifsSettingsRoute

@Serializable
data class RedGifsSearchRoute(val query: String? = null)

@Serializable
data class RedGifsGifRoute(val id: String)

@Serializable
data class RedGifsFeedRoute(val kind: String, val arg: String = "", val title: String = "")

@Serializable
data class RedGifsCreatorRoute(val name: String)

@Serializable
data class RedGifsNichesRoute(val following: Boolean = false)

@Serializable
data class RedGifsUsersRoute(val mode: String)

@Serializable
object RedGifsAccountRoute

@Serializable
object RedGifsLoginRoute

@Serializable
object RedGifsEditProfileRoute

@Serializable
object RedGifsCollectionsRoute

@Serializable
object RedGifsTagsRoute

@Serializable
object RedGifsDashboardRoute

@Composable
fun RedGifsNavHost(
    activity: MainActivity,
    navController: NavHostController,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(Unit) {
        delay(50)
        NavigationManager.initialize(navController, SiteType.REDGIFS)
    }

    val goLogin = { navController.navigateSafely(RedGifsLoginRoute) }
    val back = { navController.popBackStackSafely() }

    val sessionEpoch by RedGifsSession.epoch.collectAsState()
    val loggedIn = remember(sessionEpoch) { RedGifsAuth.isLoggedIn }

    val actions = remember(navController) {
        RedGifsActions(
            openCreator = { name -> navController.navigateSafely(RedGifsCreatorRoute(name)) },
            openTag = { tag -> navController.navigateSafely(RedGifsFeedRoute("search", tag, "#$tag")) },
        )
    }

    CompositionLocalProvider(LocalRedGifsActions provides actions) {
    NavHost(
        navController = navController,
        startDestination = RedGifsHomeRoute,
        modifier = modifier,
    ) {
        composable<RedGifsHomeRoute> {
            Box(modifier = Modifier.padding(contentPadding)) {
                RedGifsHomeScreen(
                    onSearch = { navController.navigateSafely(RedGifsSearchRoute()) },
                    onLogin = { goLogin() },
                    onOpenSettings = { navController.navigateSafely(RedGifsSettingsRoute) },
                )
            }
        }

        composable<RedGifsSearchRoute> {
            val route = it.toRoute<RedGifsSearchRoute>()
            RedGifsSearchScreen(
                initialQuery = route.query,
                onBack = { back() },
                onLogin = { goLogin() },
                onOpenCreator = { user -> navController.navigateSafely(RedGifsCreatorRoute(user.username)) },
                onOpenNiche = { niche ->
                    navController.navigateSafely(RedGifsFeedRoute("niche", niche.id, niche.name))
                },
            )
        }

        composable<RedGifsGifRoute> {
            val route = it.toRoute<RedGifsGifRoute>()
            RedGifsGifScreen(id = route.id, onBack = { back() })
        }

        composable<RedGifsFeedRoute> {
            val route = it.toRoute<RedGifsFeedRoute>()
            RedGifsFeedScreen(
                source = RedGifsFeedSource.from(route.kind, route.arg),
                title = route.title.ifBlank { route.arg },
                onBack = { back() },
                onLogin = { goLogin() },
            )
        }

        composable<RedGifsCreatorRoute> {
            val route = it.toRoute<RedGifsCreatorRoute>()
            RedGifsCreatorScreen(name = route.name, onBack = { back() }, onLogin = { goLogin() })
        }

        composable<RedGifsNichesRoute> {
            val route = it.toRoute<RedGifsNichesRoute>()
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
                        navController.navigateSafely(RedGifsFeedRoute("niche", niche.id, niche.name))
                    },
                )
            }
        }

        composable<RedGifsUsersRoute> {
            val route = it.toRoute<RedGifsUsersRoute>()
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
                    onOpenUser = { user -> navController.navigateSafely(RedGifsCreatorRoute(user.username)) },
                )
            }
        }

        composable<RedGifsAccountRoute> {
            RedGifsAccountScreen(
                onBack = { back() },
                onLogin = { goLogin() },
                onOpenFollowing = { navController.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWING.name)) },
                onOpenFollowers = { navController.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWERS.name)) },
                onOpenFollowedNiches = { navController.navigateSafely(RedGifsNichesRoute(following = true)) },
                onOpenLiked = {
                    navController.navigateSafely(
                        RedGifsFeedRoute("liked", "", activity.getString(R.string.redgifs_liked)),
                    )
                },
                onOpenMine = {
                    navController.navigateSafely(
                        RedGifsFeedRoute("mine", "", activity.getString(R.string.redgifs_my_uploads)),
                    )
                },
                onOpenSettings = { navController.navigateSafely(RedGifsSettingsRoute) },
                onOpenDashboard = { navController.navigateSafely(RedGifsDashboardRoute) },
                onOpenEditProfile = { navController.navigateSafely(RedGifsEditProfileRoute) },
                onOpenCollections = { navController.navigateSafely(RedGifsCollectionsRoute) },
            )
        }

        composable<RedGifsEditProfileRoute> {
            LoginGate(
                loggedIn = loggedIn,
                title = stringResource(R.string.redgifs_edit_profile),
                onBack = { back() },
                onSignIn = { goLogin() },
            ) { RedGifsEditProfileScreen(onBack = { back() }) }
        }

        composable<RedGifsCollectionsRoute> {
            LoginGate(
                loggedIn = loggedIn,
                title = stringResource(R.string.redgifs_collections),
                onBack = { back() },
                onSignIn = { goLogin() },
            ) { RedGifsCollectionsScreen(onBack = { back() }) }
        }

        composable<RedGifsTagsRoute> {
            RedGifsTagsScreen(
                onBack = { back() },
                onOpenTag = { tag -> navController.navigateSafely(RedGifsFeedRoute("search", tag, "#$tag")) },
            )
        }

        composable<RedGifsDashboardRoute> {
            LoginGate(
                loggedIn = loggedIn,
                title = stringResource(R.string.redgifs_dashboard),
                onBack = { back() },
                onSignIn = { goLogin() },
            ) {
                RedGifsDashboardScreen(
                    onBack = { back() },
                    onEditProfile = { navController.navigateSafely(RedGifsEditProfileRoute) },
                    onCollections = { navController.navigateSafely(RedGifsCollectionsRoute) },
                    onMine = {
                        navController.navigateSafely(
                            RedGifsFeedRoute("mine", "", activity.getString(R.string.redgifs_my_uploads)),
                        )
                    },
                    onLiked = {
                        navController.navigateSafely(
                            RedGifsFeedRoute("liked", "", activity.getString(R.string.redgifs_liked)),
                        )
                    },
                    onFollowers = { navController.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWERS.name)) },
                    onFollowing = { navController.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWING.name)) },
                )
            }
        }

        composable<RedGifsLoginRoute> {
            RedGifsLoginScreen(
                onBack = { back() },
                onLoggedIn = { back() },
            )
        }

        composable<RedGifsSettingsRoute> {
            RedGifsSettingsScreen(onBack = { back() })
        }

        composable<RedGifsSettingsHubRoute> {
            SiteSettingsHubScreen(
                site = SiteType.REDGIFS,
                onResetSite = { RedGifsSettings.resetToDefaults() },
                siteEntries = listOf(
                    SiteSettingsEntry(
                        key = "playback",
                        title = stringResource(R.string.redgifs_settings_title),
                        subtitle = stringResource(R.string.redgifs_settings_summary),
                        icon = Icons.Default.HighQuality,
                        onClick = { navController.navigateSafely(RedGifsSettingsRoute) },
                    ),
                    SiteSettingsEntry(
                        key = "account",
                        title = stringResource(R.string.redgifs_account),
                        subtitle = stringResource(R.string.redgifs_account_summary),
                        icon = Icons.Default.Person,
                        onClick = { navController.navigateSafely(RedGifsAccountRoute) },
                    ),
                ),
                onBack = { back() },
                onOpenAppSettings = { navController.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navController.navigateSafely(SitePlayerSettingsRoute(SiteType.REDGIFS.name)) },
                onOpenNetworkSettings = { navController.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navController.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        registerSiteHubRoutes(
            navController = navController,
            site = SiteType.REDGIFS,
            config = { redGifsHubConfig(navController, activity) },
        )

        registerGlobalSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = SiteProfileRoute,
        )
    }
    }
}
