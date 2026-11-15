package com.yenaly.han1meviewer.MissAV.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberCommonProfileEntries
import androidx.compose.ui.res.stringResource

/**
 * MissAV's contribution to the shared Subscription / Library / Profile / Customize pages.
 *
 *  - The account card on My Profile reflects the real MissAV login (see `siteLoginStatus`).
 *  - Subscription lists saved actresses, MissAV's follow-style list. Pages that need an
 *    account show their own "log in" prompt, so entries are always listed.
 *  - Library separates account (online) data from data stored on this device.
 */
@Composable
fun missAvHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig {
    val common = rememberCommonProfileEntries(
        navController = navController,
        activity = activity,
        site = SiteType.MISSAV,
        siteSettingsRoute = MissAvSettingsHubRoute,
    )
    val subtitles = SiteHubEntry(
        key = "video_subtitles",
        title = stringResource(R.string.missav_subtitle_title),
        subtitle = stringResource(R.string.missav_subtitle_entry_summary),
        icon = Icons.Filled.Subtitles,
        onClick = { navController.navigateSafely(MissAvSubtitleSettingsRoute) },
    )
    val playerSettings = SiteHubEntry(
        key = "player",
        title = stringResource(R.string.player_settings),
        icon = Icons.Filled.PlayCircleOutline,
        onClick = {
            navController.navigateSafely(SitePlayerSettingsRoute(SiteType.MISSAV.name))
        },
    )
    val account = SiteHubEntry(
        key = "account",
        title = stringResource(R.string.missav_acc_account_title),
        subtitle = stringResource(R.string.login_status_tap_account),
        icon = Icons.Filled.AccountCircle,
        onClick = { navController.navigateSafely(MissAvAccountRoute) },
    )
    val savedActresses = SiteHubEntry(
        key = "saved_actresses",
        title = stringResource(R.string.missav_acc_saved_actresses),
        icon = Icons.Filled.Favorite,
        onClick = { navController.navigateSafely(MissAvSavedRoute(1)) },
    )
    return SiteHubConfig(
        site = SiteType.MISSAV,
        subscriptionSections = listOf(SiteHubSection(null, listOf(savedActresses))),
        librarySections = listOf(
            SiteHubSection(
                title = stringResource(R.string.site_drawer_section_account),
                entries = listOf(
                    SiteHubEntry(
                        key = "saved_videos",
                        title = stringResource(R.string.missav_acc_saved),
                        icon = Icons.Filled.Bookmark,
                        onClick = { navController.navigateSafely(MissAvSavedRoute(0)) },
                    ),
                    savedActresses,
                    SiteHubEntry(
                        key = "actresses",
                        title = stringResource(R.string.missav_acc_actresses),
                        icon = Icons.Filled.Face,
                        onClick = { navController.navigateSafely(MissAvActressesRoute) },
                    ),
                    SiteHubEntry(
                        key = "playlists",
                        title = stringResource(R.string.missav_acc_playlists),
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        onClick = { navController.navigateSafely(MissAvPlaylistsRoute) },
                    ),
                    SiteHubEntry(
                        key = "site_history",
                        title = stringResource(R.string.missav_acc_site_history),
                        icon = Icons.Filled.History,
                        onClick = { navController.navigateSafely(MissAvSiteHistoryRoute) },
                    ),
                ),
            ),
            SiteHubSection(
                title = stringResource(R.string.hub_section_on_device),
                entries = listOf(
                    common.offlinePlaylist,
                    SiteHubEntry(
                        key = "local_history",
                        title = stringResource(R.string.missav_acc_local_history),
                        subtitle = stringResource(R.string.missav_history_subtitle),
                        icon = Icons.Filled.History,
                        onClick = { navController.navigateSafely(MissAvHistoryRoute) },
                    ),
                ),
            ),
        ),
        profileQuickActions = listOf(
            common.siteSettings,
            common.checkIn,
            common.switchSite,
            subtitles,
            common.customize,
            common.allHistory,
        ),
        profileSections = listOf(SiteHubSection(stringResource(R.string.site_drawer_section_account), listOf(account))),
        customizeSections = listOf(
            SiteHubSection(
                title = null,
                entries = listOf(
                    SiteHubEntry(
                        key = "home_categories",
                        title = stringResource(R.string.drawer_home_categories),
                        subtitle = stringResource(R.string.site_home_categories_subtitle),
                        icon = Icons.Filled.Category,
                        onClick = { navController.navigateSafely(MissAvHomeCategoriesRoute) },
                    ),
                    SiteHubEntry(
                        key = "host",
                        title = stringResource(R.string.drawer_host_mirror),
                        subtitle = stringResource(R.string.missav_host_subtitle),
                        icon = Icons.Filled.Dns,
                        onClick = { navController.navigateSafely(MissAvHostSettingsRoute) },
                    ),
                    SiteHubEntry(
                        key = "video_quality",
                        title = stringResource(R.string.missav_video_resolution),
                        subtitle = stringResource(R.string.missav_choose_auto_or_a_fixed_starting),
                        icon = Icons.Filled.HighQuality,
                        onClick = { navController.navigateSafely(MissAvQualitySettingsRoute) },
                    ),
                    playerSettings,
                ),
            ),
        ),
    )
}
