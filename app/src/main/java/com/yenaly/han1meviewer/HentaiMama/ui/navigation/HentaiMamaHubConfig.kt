package com.yenaly.han1meviewer.HentaiMama.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

/**
 * HentaiMama: every existing page (playlists + playlist details, history, recent episodes,
 * upcoming) lives in Library. Upcoming is additionally the single home-header shortcut.
 * The site has no account system, so the profile account card shows the unavailable state.
 */
@Composable
fun hentaiMamaHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.HENTAIMAMA,
        navController = navController,
        activity = activity,
        siteSettingsRoute = HentaiMamaSettingsHubRoute,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "playlists",
                title = stringResource(R.string.hm_playlists_title),
                subtitle = stringResource(R.string.hm_playlists_subtitle),
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { navController.navigateSafely(HentaiMamaPlaylistsRoute) },
            ),
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.hm_history_title),
                subtitle = stringResource(R.string.hm_history_subtitle),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(HentaiMamaHistoryRoute) },
            ),
            SiteHubEntry(
                key = "recent_episodes",
                title = stringResource(R.string.drawer_recent_episodes),
                subtitle = stringResource(R.string.hm_recent_episodes_subtitle),
                icon = Icons.Filled.VideoLibrary,
                onClick = { navController.navigateSafely(HentaiMamaRecentEpisodesRoute) },
            ),
            SiteHubEntry(
                key = "upcoming",
                title = stringResource(R.string.drawer_upcoming),
                subtitle = stringResource(R.string.hm_upcoming_subtitle),
                icon = Icons.Filled.CalendarMonth,
                onClick = { navController.navigateSafely(HentaiMamaUpcomingRoute) },
            ),
        ),
        customizeEntries = listOf(
            SiteHubEntry(
                key = "video_card",
                title = stringResource(R.string.drawer_video_card_size),
                subtitle = stringResource(R.string.hm_video_card_subtitle),
                icon = Icons.Filled.PlayCircleOutline,
                onClick = {
                    navController.navigateSafely(HentaiMamaVideoSettingsRoute(target = "all"))
                },
            ),
            SiteHubEntry(
                key = "home_categories",
                title = stringResource(R.string.drawer_home_categories),
                subtitle = stringResource(R.string.site_home_categories_subtitle),
                icon = Icons.Filled.Category,
                onClick = { navController.navigateSafely(HentaiMamaHomeSettingsRoute) },
            ),
            rememberPlayerSettingsEntry(navController, SiteType.HENTAIMAMA),
        ),
    )
