package com.yenaly.han1meviewer.SitesHub.MissAV.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.SitesHub.MissAV.data.remote.MissAvAuth
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.gated
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberCommonProfileEntries
import androidx.compose.ui.res.stringResource

@Composable
fun missAvHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig {
    val common = rememberCommonProfileEntries(
        navigator = navigator,
        activity = activity,
    )
    val loggedIn = MissAvAuth.currentUser() != null
    val onLogin = { navigator.navigateSafely(MissAvLoginRoute) }
    val account = SiteHubEntry(
        key = "account",
        title = stringResource(R.string.missav_acc_account_title),
        subtitle = stringResource(R.string.login_status_tap_account),
        icon = Icons.Filled.AccountCircle,
        onClick = { navigator.navigateSafely(MissAvAccountRoute) },
    ).gated(loggedIn, onLogin)
    val savedActresses = SiteHubEntry(
        key = "saved_actresses",
        title = stringResource(R.string.missav_acc_saved_actresses),
        icon = Icons.Filled.Favorite,
        onClick = { navigator.navigateSafely(MissAvSavedRoute(1)) },
    ).gated(loggedIn, onLogin)
    val savedVideos = SiteHubEntry(
        key = "saved_videos",
        title = stringResource(R.string.missav_acc_saved),
        icon = Icons.Filled.Bookmark,
        onClick = { navigator.navigateSafely(MissAvSavedRoute(0)) },
    ).gated(loggedIn, onLogin)
    val actresses = SiteHubEntry(
        key = "actresses",
        title = stringResource(R.string.missav_acc_actresses),
        icon = Icons.Filled.Face,
        onClick = { navigator.navigateSafely(MissAvActressesRoute) },
    )
    return SiteHubConfig(
        site = SiteType.MISSAV,
        subscriptionSections = listOf(
            SiteHubSection(null, listOf(savedVideos, savedActresses, actresses)),
        ),
        librarySections = listOf(
            SiteHubSection(
                title = stringResource(R.string.site_drawer_section_account),
                entries = listOf(
                    SiteHubEntry(
                        key = "playlists",
                        title = stringResource(R.string.missav_acc_playlists),
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        onClick = { navigator.navigateSafely(MissAvPlaylistsRoute) },
                    ).gated(loggedIn, onLogin),
                    SiteHubEntry(
                        key = "site_history",
                        title = stringResource(R.string.missav_acc_site_history),
                        icon = Icons.Filled.History,
                        onClick = { navigator.navigateSafely(MissAvSiteHistoryRoute) },
                    ).gated(loggedIn, onLogin),
                ),
            ),
            SiteHubSection(
                title = stringResource(R.string.hub_section_on_device),
                entries = listOf(
                    common.offlinePlaylist,
                    common.downloads,
                    SiteHubEntry(
                        key = "local_history",
                        title = stringResource(R.string.missav_acc_local_history),
                        subtitle = stringResource(R.string.missav_history_subtitle),
                        icon = Icons.Filled.History,
                        onClick = { navigator.navigateSafely(MissAvHistoryRoute) },
                    ),
                ),
            ),
        ),
        profileQuickActions = listOf(
            common.settings,
            common.checkIn,
            common.switchSite,
            common.allHistory,
        ),
        profileSections = listOf(SiteHubSection(stringResource(R.string.site_drawer_section_account), listOf(account))),
    )
}
