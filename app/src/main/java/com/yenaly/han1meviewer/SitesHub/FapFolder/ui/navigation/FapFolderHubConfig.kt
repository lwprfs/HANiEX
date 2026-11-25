package com.yenaly.han1meviewer.SitesHub.FapFolder.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.remote.FapFolderNetwork
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.rememberSocialStyleEntry
import com.yenaly.han1meviewer.ui.screen.site.gated
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun fapFolderHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig {
    val loggedIn = FapFolderNetwork.hasAuthCookies()
    val onLogin = { navigator.navigateSafely(FapFolderLoginRoute) }
    val followed = SiteHubEntry(
        key = "followed",
        title = stringResource(R.string.drawer_followed_communities),
        subtitle = stringResource(R.string.fap_followed_subtitle),
        icon = Icons.Filled.Groups,
        onClick = { navigator.navigateSafely(FapFolderFollowedCommunitiesRoute) },
    ).gated(loggedIn, onLogin)
    val account = SiteHubEntry(
        key = "account",
        title = stringResource(R.string.site_my_account),
        icon = Icons.Filled.AccountCircle,
        onClick = { navigator.navigateSafely(FapFolderAccountRoute) },
    ).gated(loggedIn, onLogin)
    val cookies = SiteHubEntry(
        key = "cookies",
        title = stringResource(R.string.site_cookies),
        subtitle = stringResource(R.string.fap_cookies_subtitle),
        icon = Icons.Filled.Cookie,
        onClick = { navigator.navigateSafely(FapFolderCookiesRoute) },
    )
    val privacy = SiteHubEntry(
        key = "privacy",
        title = stringResource(R.string.site_privacy),
        subtitle = stringResource(R.string.fap_privacy_subtitle),
        icon = Icons.Filled.Lock,
        onClick = { navigator.navigateSafely(FapFolderPrivacyRoute) },
    ).gated(loggedIn, onLogin)
    val authEntries = if (loggedIn) {
        listOf(account, privacy, cookies)
    } else {
        listOf(
            account,
            privacy,
            SiteHubEntry(
                key = "login",
                title = stringResource(R.string.site_sign_in),
                subtitle = stringResource(R.string.fap_signin_subtitle),
                icon = Icons.AutoMirrored.Filled.Login,
                onClick = { navigator.navigateSafely(FapFolderLoginRoute) },
            ),
            SiteHubEntry(
                key = "register",
                title = stringResource(R.string.site_create_account),
                subtitle = stringResource(R.string.fap_create_account_subtitle),
                icon = Icons.Filled.PersonAdd,
                onClick = { navigator.navigateSafely(FapFolderRegisterRoute) },
            ),
            cookies,
        )
    }
    return standardSiteHubConfig(
        site = SiteType.FAPFOLDER,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = FapFolderSettingsRoute,
        subscriptionSections = listOf(SiteHubSection(null, listOf(followed))),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.fap_history_title),
                subtitle = stringResource(R.string.fap_history_subtitle),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(FapFolderHistoryRoute) },
            ),
            SiteHubEntry(
                key = "saved",
                title = stringResource(R.string.fap_saved_posts_title),
                subtitle = stringResource(R.string.fap_saved_posts_subtitle),
                icon = Icons.Filled.Bookmark,
                onClick = { navigator.navigateSafely(FapFolderSavedRoute) },
            ).gated(loggedIn, onLogin),
        ),
        extraLibrarySections = emptyList(),
        profileExtraQuickActions = authEntries,
        customizeEntries = listOf(rememberPlayerSettingsEntry(navigator, SiteType.FAPFOLDER),
            rememberSocialStyleEntry(navigator, SiteType.FAPFOLDER)),
    )
}
