package com.yenaly.han1meviewer.MissAV.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
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
 * MissAV has no authentication and no subscription feature, so:
 *  - the account card shows the "unavailable" state coming from `siteLoginStatus`;
 *    when login is added later only that function (and an `onClick`) needs to change;
 *  - `subscriptionSections` is empty so the page shows an honest unavailable state;
 *  - online playlists / actresses / saved videos are NOT listed because no data source exists.
 *    Add them to [librarySections] below once a real implementation is available.
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
    val history = SiteHubEntry(
        key = "history",
        title = stringResource(R.string.missav_history_title),
        subtitle = stringResource(R.string.missav_history_subtitle),
        icon = Icons.Filled.History,
        onClick = { navController.navigateSafely(MissAvHistoryRoute) },
    )
    val playerSettings = SiteHubEntry(
        key = "player",
        title = stringResource(R.string.player_settings),
        icon = Icons.Filled.PlayCircleOutline,
        onClick = {
            navController.navigateSafely(SitePlayerSettingsRoute(SiteType.MISSAV.name))
        },
    )
    return SiteHubConfig(
        site = SiteType.MISSAV,
        subscriptionSections = emptyList(),
        librarySections = listOf(
            SiteHubSection(
                title = stringResource(R.string.hub_section_on_device),
                entries = listOf(common.offlinePlaylist),
            ),
            SiteHubSection(
                title = stringResource(R.string.hub_section_history),
                entries = listOf(history),
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
        profileSections = emptyList(),
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
