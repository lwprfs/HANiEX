package com.yenaly.han1meviewer.OnAir.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
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
 * OnAir: all browsing pages (history, tags, pictures, announcements, chat) live in Library so
 * nothing becomes unreachable once the drawer is gone. Account/session and cookie sign-in are
 * profile quick actions; the existing login / register screens are untouched.
 */
@Composable
fun onAirHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig =
    standardSiteHubConfig(
        site = SiteType.ONAIR,
        navController = navController,
        activity = activity,
        siteSettingsRoute = OnAirSettingsRoute,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.onair_history_title),
                subtitle = stringResource(R.string.onair_history_subtitle),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(OnAirHistoryRoute) },
            ),
            SiteHubEntry(
                key = "tags",
                title = stringResource(R.string.drawer_tags),
                subtitle = stringResource(R.string.onair_tags_subtitle),
                icon = Icons.AutoMirrored.Filled.Label,
                onClick = { navController.navigateSafely(OnAirTagsIndexRoute) },
            ),
            SiteHubEntry(
                key = "pictures",
                title = stringResource(R.string.drawer_pictures),
                icon = Icons.Filled.Image,
                onClick = { navController.navigateSafely(OnAirPicturesRoute) },
            ),
            SiteHubEntry(
                key = "announcements",
                title = stringResource(R.string.onair_announcements_title),
                subtitle = stringResource(R.string.onair_announcements_subtitle),
                icon = Icons.Filled.Campaign,
                onClick = { navController.navigateSafely(OnAirAnnouncementsRoute) },
            ),
            SiteHubEntry(
                key = "chat",
                title = stringResource(R.string.drawer_chat),
                subtitle = stringResource(R.string.onair_chat_subtitle),
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = { navController.navigateSafely(OnAirChatRoute) },
            ),
        ),
        profileExtraQuickActions = listOf(
            SiteHubEntry(
                key = "account",
                title = stringResource(R.string.drawer_account_session),
                subtitle = stringResource(R.string.onair_account_subtitle),
                icon = Icons.Filled.AccountCircle,
                onClick = { navController.navigateSafely(OnAirAccountSettingsRoute) },
            ),
            SiteHubEntry(
                key = "cookies",
                title = stringResource(R.string.drawer_cookies_sign_in),
                subtitle = stringResource(R.string.onair_cookies_subtitle),
                icon = Icons.Filled.Lock,
                onClick = { navController.navigateSafely(OnAirCookieRoute) },
            ),
        ),
        customizeEntries = listOf(rememberPlayerSettingsEntry(navController, SiteType.ONAIR)),
    )
