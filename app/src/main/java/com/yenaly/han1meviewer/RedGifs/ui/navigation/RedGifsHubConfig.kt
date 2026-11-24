package com.yenaly.han1meviewer.RedGifs.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RedGifs.common.RedGifsSession
import com.yenaly.han1meviewer.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.RedGifs.ui.browse.RedGifsUsersMode
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.gated
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.rememberSocialStyleEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun redGifsHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig {

    @Suppress("UNUSED_VARIABLE")
    val sessionEpoch by RedGifsSession.epoch.collectAsState()
    val loggedIn = RedGifsAuth.isLoggedIn
    val onLogin = { navigator.navigateSafely(RedGifsLoginRoute) }

    val likedTitle = stringResource(R.string.redgifs_liked)
    val mineTitle = stringResource(R.string.redgifs_my_uploads)

    val subscription = SiteHubSection(
        title = null,
        entries = listOf(
            SiteHubEntry(
                key = "following",
                title = stringResource(R.string.redgifs_following),
                icon = Icons.Filled.Groups,
                onClick = { navigator.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.FOLLOWING.name)) },
            ).gated(loggedIn, onLogin),
            SiteHubEntry(
                key = "followed_niches",
                title = stringResource(R.string.redgifs_followed_niches),
                icon = Icons.Filled.Subscriptions,
                onClick = { navigator.navigateSafely(RedGifsNichesRoute(following = true)) },
            ).gated(loggedIn, onLogin),
            SiteHubEntry(
                key = "niches",
                title = stringResource(R.string.redgifs_niches),
                icon = Icons.Filled.Category,
                onClick = { navigator.navigateSafely(RedGifsNichesRoute()) },
            ),
            SiteHubEntry(
                key = "tags",
                title = stringResource(R.string.redgifs_tags),
                icon = Icons.Filled.Category,
                onClick = { navigator.navigateSafely(RedGifsTagsRoute) },
            ),
            SiteHubEntry(
                key = "creators",
                title = stringResource(R.string.redgifs_creators),
                icon = Icons.Filled.Star,
                onClick = { navigator.navigateSafely(RedGifsUsersRoute(RedGifsUsersMode.CREATORS.name)) },
            ),
        ),
    )

    val libraryEntries = listOf(
        SiteHubEntry(
            key = "liked",
            title = likedTitle,
            icon = Icons.Filled.ThumbUp,
            onClick = { navigator.navigateSafely(RedGifsFeedRoute("liked", "", likedTitle)) },
        ).gated(loggedIn, onLogin),
        SiteHubEntry(
            key = "mine",
            title = mineTitle,
            icon = Icons.Filled.PhotoLibrary,
            onClick = { navigator.navigateSafely(RedGifsFeedRoute("mine", "", mineTitle)) },
        ).gated(loggedIn, onLogin),
        SiteHubEntry(
            key = "collections",
            title = stringResource(R.string.redgifs_collections),
            icon = Icons.Filled.PhotoLibrary,
            onClick = { navigator.navigateSafely(RedGifsCollectionsRoute) },
        ).gated(loggedIn, onLogin),
        SiteHubEntry(
            key = "dashboard",
            title = stringResource(R.string.redgifs_dashboard),
            icon = Icons.Filled.AccountCircle,
            onClick = { navigator.navigateSafely(RedGifsDashboardRoute) },
        ).gated(loggedIn, onLogin),
    )

    val account = SiteHubEntry(
        key = "account",
        title = stringResource(R.string.redgifs_account),
        subtitle = stringResource(R.string.redgifs_account_summary),
        icon = Icons.Filled.AccountCircle,
        onClick = { navigator.navigateSafely(RedGifsAccountRoute) },
    )
    val playback = SiteHubEntry(
        key = "playback",
        title = stringResource(R.string.redgifs_settings_title),
        subtitle = stringResource(R.string.redgifs_settings_summary),
        icon = Icons.Filled.HighQuality,
        onClick = { navigator.navigateSafely(RedGifsSettingsRoute) },
    )

    return standardSiteHubConfig(
        site = SiteType.REDGIFS,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = RedGifsSettingsHubRoute,
        siteLibraryEntries = emptyList(),
        extraLibrarySections = listOf(
            SiteHubSection(stringResource(R.string.redgifs_my_library), libraryEntries),
        ),
        subscriptionSections = listOf(subscription),
        profileExtraQuickActions = listOf(account),
        customizeEntries = listOf(playback, rememberPlayerSettingsEntry(navigator, SiteType.REDGIFS),
            rememberSocialStyleEntry(navigator, SiteType.REDGIFS)),
    )
}
