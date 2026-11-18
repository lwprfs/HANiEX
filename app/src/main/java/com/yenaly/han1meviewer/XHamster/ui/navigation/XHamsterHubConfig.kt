package com.yenaly.han1meviewer.XHamster.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.XHamster.common.XHamsterSession
import com.yenaly.han1meviewer.XHamster.common.XHamsterSettings
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterMomentsRepo
import com.yenaly.han1meviewer.XHamster.common.rememberXHamsterFlag
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.gated
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterAuth

@Composable
fun xHamsterHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig {
    // Reading the epoch makes the hub recompose after login / logout / settings changes.
    @Suppress("UNUSED_VARIABLE")
    val sessionEpoch by XHamsterSession.epoch.collectAsState()
    val loggedIn = XHamsterAuth.isLoggedIn
    val onLogin = { navController.navigateSafely(XHamsterAccountRoute) }
    fun library(tab: String) = { navController.navigateSafely(XHamsterLibraryRoute(tab)) }

    val subscription = SiteHubSection(
        title = null,
        entries = listOf(
            SiteHubEntry(
                key = "feed",
                title = stringResource(R.string.xhamster_subscriptions_feed),
                icon = Icons.Filled.Subscriptions,
                onClick = library("SUBSCRIPTIONS"),
            ).gated(loggedIn, onLogin),
            SiteHubEntry(
                key = "channels",
                title = stringResource(R.string.xhamster_channels),
                icon = Icons.Filled.Tv,
                onClick = { navController.navigateSafely(XHamsterChannelsRoute) },
            ),
            SiteHubEntry(
                key = "creators",
                title = stringResource(R.string.xhamster_top_creators),
                icon = Icons.Filled.Star,
                onClick = { navController.navigateSafely(XHamsterStarsRoute) },
            ),
            SiteHubEntry(
                key = "categories",
                title = stringResource(R.string.xhamster_categories),
                icon = Icons.Filled.Category,
                onClick = { navController.navigateSafely(XHamsterCategoriesRoute) },
            ),
        ),
    )
    val more = SiteHubSection(
        title = "More to explore",
        entries = listOf(
            SiteHubEntry(
                key = "photos",
                title = "Photos",
                icon = Icons.Filled.PhotoLibrary,
                onClick = { navController.navigateSafely(XHamsterPhotosRoute) },
            ),
            SiteHubEntry(
                key = "gifs",
                title = "GIFs",
                icon = Icons.Filled.Gif,
                onClick = {
                    navController.navigateSafely(XHamsterMomentsRoute(XHamsterMomentsRepo.Feed.GIFS.name, "GIFs"))
                },
            ),
            SiteHubEntry(
                key = "shorts",
                title = "Short videos",
                icon = Icons.Filled.SmartDisplay,
                onClick = {
                    navController.navigateSafely(XHamsterMomentsRoute(XHamsterMomentsRepo.Feed.SHORTS.name, "Short videos"))
                },
            ),
            SiteHubEntry(
                key = "top_creator_videos",
                title = "Top creator videos",
                icon = Icons.Filled.EmojiEvents,
                onClick = { navController.navigateSafely(XHamsterTopCreatorVideosRoute) },
            ),
        ),
    )
    val libraryEntries = listOf(
        SiteHubEntry("history", stringResource(R.string.xhamster_watch_history), Icons.Filled.History, library("HISTORY")).gated(loggedIn, onLogin),
        SiteHubEntry("favorites", stringResource(R.string.xhamster_favorites), Icons.Filled.Favorite, library("FAVORITES")).gated(loggedIn, onLogin),
        SiteHubEntry("watch_later", stringResource(R.string.watch_later), Icons.Filled.Schedule, library("WATCH_LATER")).gated(loggedIn, onLogin),
        SiteHubEntry("liked", stringResource(R.string.xhamster_liked_videos), Icons.Filled.ThumbUp, library("LIKED")).gated(loggedIn, onLogin),
        SiteHubEntry("collections", stringResource(R.string.xhamster_playlists), Icons.AutoMirrored.Filled.PlaylistAdd, library("PLAYLISTS")).gated(loggedIn, onLogin),
    )
    val accountSettings = SiteHubEntry(
        key = "account_settings",
        title = stringResource(R.string.xhamster_account_settings),
        subtitle = stringResource(R.string.xhamster_account_settings_summary),
        icon = Icons.Filled.AccountCircle,
        onClick = { navController.navigateSafely(XHamsterAccountSettingsRoute) },
    ).gated(loggedIn, onLogin)
    val playback = SiteHubEntry(
        key = "playback",
        title = stringResource(R.string.xhamster_settings_title),
        subtitle = stringResource(R.string.xhamster_settings_summary),
        icon = Icons.Filled.HighQuality,
        onClick = { navController.navigateSafely(XHamsterSettingsRoute) },
    )
    val showChannels = rememberXHamsterFlag(XHamsterSettings.KEY_SHOW_CHANNELS)
    val showCreators = rememberXHamsterFlag(XHamsterSettings.KEY_SHOW_TOP_CREATORS)
    val sectionTitle = stringResource(R.string.hub_xhamster_home_sections)
    val channelsTitle = stringResource(R.string.hub_xhamster_show_channels)
    val channelsDesc = stringResource(R.string.hub_xhamster_show_channels_desc)
    val creatorsTitle = stringResource(R.string.hub_xhamster_show_top_creators)
    val creatorsDesc = stringResource(R.string.hub_xhamster_show_top_creators_desc)

    return standardSiteHubConfig(
        site = SiteType.XHAMSTER,
        navController = navController,
        activity = activity,
        siteSettingsRoute = XHamsterSettingsHubRoute,
        siteLibraryEntries = emptyList(),
        extraLibrarySections = listOf(
            SiteHubSection(stringResource(R.string.xhamster_my_library), libraryEntries),
        ),
        subscriptionSections = listOf(subscription, more),
        profileExtraQuickActions = listOf(accountSettings),
        customizeEntries = listOf(playback, rememberPlayerSettingsEntry(navController, SiteType.XHAMSTER)),
        customizeExtras = {
            item(key = "xh_home_header") { SettingSectionHeader(title = sectionTitle) }
            item(key = "xh_show_channels") {
                SettingSwitchItem(
                    title = channelsTitle,
                    summary = channelsDesc,
                    checked = showChannels,
                    onCheckedChange = { XHamsterSettings.showChannels = it },
                )
            }
            item(key = "xh_show_creators") {
                SettingSwitchItem(
                    title = creatorsTitle,
                    summary = creatorsDesc,
                    checked = showCreators,
                    onCheckedChange = { XHamsterSettings.showTopCreators = it },
                )
            }
        },
    )
}
