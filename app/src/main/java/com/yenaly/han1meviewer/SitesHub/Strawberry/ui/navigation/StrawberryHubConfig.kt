package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
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
fun strawberryHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig {
    val loggedIn = siteLoginStatus(LocalContext.current, SiteType.STRAWBERRY).loggedIn
    return standardSiteHubConfig(
        site = SiteType.STRAWBERRY,
        navigator = navigator,
        activity = activity,
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "search",
                title = stringResource(R.string.search),
                icon = Icons.Filled.Search,
                onClick = { navigator.navigateSafely(StrawberrySearchRoute()) },
            ),
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.strawberry_history_title),
                subtitle = stringResource(R.string.strawberry_history_subtitle),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(StrawberryHistoryRoute) },
            ),
            SiteHubEntry(
                key = "announcements",
                title = stringResource(R.string.strawberry_announcements_title),
                subtitle = stringResource(R.string.strawberry_announcements_subtitle),
                icon = Icons.Filled.Campaign,
                onClick = { navigator.navigateSafely(StrawberryAnnouncementsRoute) },
            ),
            SiteHubEntry(
                key = "chat",
                title = stringResource(R.string.drawer_chat),
                subtitle = stringResource(R.string.strawberry_chat_subtitle),
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = { navigator.navigateSafely(StrawberryChatRoute) },
            ),
        ),
        profileExtraQuickActions = listOf(
            SiteHubEntry(
                key = "account",
                title = stringResource(R.string.drawer_account_session),
                subtitle = stringResource(R.string.strawberry_account_subtitle),
                icon = Icons.Filled.AccountCircle,
                onClick = { navigator.navigateSafely(StrawberryAccountSettingsRoute) },
            ).gated(loggedIn) { navigator.navigateSafely(StrawberryCookieRoute) },
            SiteHubEntry(
                key = "cookies",
                title = stringResource(R.string.drawer_cookies_sign_in),
                subtitle = stringResource(R.string.strawberry_cookies_subtitle),
                icon = Icons.Filled.Lock,
                onClick = { navigator.navigateSafely(StrawberryCookieRoute) },
            ),
        ),
    )
}
