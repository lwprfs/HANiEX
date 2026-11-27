package com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.main.siteLoginStatus
import com.yenaly.han1meviewer.ui.screen.site.gated
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun onAirHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig {
    val loggedIn = siteLoginStatus(LocalContext.current, SiteType.ONAIR).loggedIn
    return standardSiteHubConfig(
        site = SiteType.ONAIR,
        navigator = navigator,
        activity = activity,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.onair_history_title),
                subtitle = stringResource(R.string.onair_history_subtitle),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(OnAirHistoryRoute) },
            ),
            SiteHubEntry(
                key = "tags",
                title = stringResource(R.string.drawer_tags),
                subtitle = stringResource(R.string.onair_tags_subtitle),
                icon = Icons.AutoMirrored.Filled.Label,
                onClick = { navigator.navigateSafely(OnAirTagsIndexRoute) },
            ),
            SiteHubEntry(
                key = "pictures",
                title = stringResource(R.string.drawer_pictures),
                icon = Icons.Filled.Image,
                onClick = { navigator.navigateSafely(OnAirPicturesRoute) },
            ),
            SiteHubEntry(
                key = "announcements",
                title = stringResource(R.string.onair_announcements_title),
                subtitle = stringResource(R.string.onair_announcements_subtitle),
                icon = Icons.Filled.Campaign,
                onClick = { navigator.navigateSafely(OnAirAnnouncementsRoute) },
            ),
            SiteHubEntry(
                key = "chat",
                title = stringResource(R.string.drawer_chat),
                subtitle = stringResource(R.string.onair_chat_subtitle),
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = { navigator.navigateSafely(OnAirChatRoute) },
            ),
        ),
        profileExtraQuickActions = listOf(
            SiteHubEntry(
                key = "account",
                title = stringResource(R.string.drawer_account_session),
                subtitle = stringResource(R.string.onair_account_subtitle),
                icon = Icons.Filled.AccountCircle,
                onClick = { navigator.navigateSafely(OnAirAccountSettingsRoute) },
            ).gated(loggedIn) { navigator.navigateSafely(OnAirCookieRoute) },
            SiteHubEntry(
                key = "cookies",
                title = stringResource(R.string.drawer_cookies_sign_in),
                subtitle = stringResource(R.string.onair_cookies_subtitle),
                icon = Icons.Filled.Lock,
                onClick = { navigator.navigateSafely(OnAirCookieRoute) },
            ),
            SiteHubEntry(
                key = "github_downloader",
                title = stringResource(R.string.onair_github_downloader_title),
                subtitle = stringResource(R.string.onair_github_downloader_subtitle),
                icon = Icons.Filled.CloudDownload,
                onClick = { navigator.navigateSafely(OnAirGithubDownloaderRoute) },
            ),
        ),
    )
}
