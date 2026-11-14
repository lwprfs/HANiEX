package com.yenaly.han1meviewer.FapFolder.ui.navigation

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
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.FapFolder.data.remote.FapFolderNetwork
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

/**
 * FapFolder: Library holds history, saved posts, followed communities and search history;
 * Subscription surfaces the followed communities. Account, privacy, cookies and the existing
 * sign-in / register screens stay reachable from My Profile.
 */
@Composable
fun fapFolderHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig {
    val loggedIn = FapFolderNetwork.hasAuthCookies()
    val followed = SiteHubEntry(
        key = "followed",
        title = stringResource(R.string.drawer_followed_communities),
        subtitle = stringResource(R.string.fap_followed_subtitle),
        icon = Icons.Filled.Groups,
        onClick = { navController.navigateSafely(FapFolderFollowedCommunitiesRoute) },
    )
    val account = SiteHubEntry(
        key = "account",
        title = stringResource(R.string.site_my_account),
        icon = Icons.Filled.AccountCircle,
        onClick = { navController.navigateSafely(FapFolderAccountRoute) },
    )
    val cookies = SiteHubEntry(
        key = "cookies",
        title = stringResource(R.string.site_cookies),
        subtitle = stringResource(R.string.fap_cookies_subtitle),
        icon = Icons.Filled.Cookie,
        onClick = { navController.navigateSafely(FapFolderCookiesRoute) },
    )
    val authEntries = if (loggedIn) {
        listOf(
            account,
            SiteHubEntry(
                key = "privacy",
                title = stringResource(R.string.site_privacy),
                subtitle = stringResource(R.string.fap_privacy_subtitle),
                icon = Icons.Filled.Lock,
                onClick = { navController.navigateSafely(FapFolderPrivacyRoute) },
            ),
            cookies,
        )
    } else {
        listOf(
            SiteHubEntry(
                key = "login",
                title = stringResource(R.string.site_sign_in),
                subtitle = stringResource(R.string.fap_signin_subtitle),
                icon = Icons.AutoMirrored.Filled.Login,
                onClick = { navController.navigateSafely(FapFolderLoginRoute) },
            ),
            SiteHubEntry(
                key = "register",
                title = stringResource(R.string.site_create_account),
                subtitle = stringResource(R.string.fap_create_account_subtitle),
                icon = Icons.Filled.PersonAdd,
                onClick = { navController.navigateSafely(FapFolderRegisterRoute) },
            ),
            cookies,
        )
    }
    return standardSiteHubConfig(
        site = SiteType.FAPFOLDER,
        navController = navController,
        activity = activity,
        siteSettingsRoute = FapFolderSettingsRoute,
        subscriptionSections = listOf(SiteHubSection(null, listOf(followed))),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.fap_history_title),
                subtitle = stringResource(R.string.fap_history_subtitle),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(FapFolderHistoryRoute) },
            ),
            SiteHubEntry(
                key = "saved",
                title = stringResource(R.string.fap_saved_posts_title),
                subtitle = stringResource(R.string.fap_saved_posts_subtitle),
                icon = Icons.Filled.Bookmark,
                onClick = { navController.navigateSafely(FapFolderSavedRoute) },
            ),
            followed,
            SiteHubEntry(
                key = "search_history",
                title = stringResource(R.string.drawer_search_history),
                subtitle = stringResource(R.string.fap_search_history_subtitle),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(FapFolderSearchHistoryRoute) },
            ),
            SiteHubEntry(
                key = "search",
                title = stringResource(R.string.site_search),
                subtitle = stringResource(R.string.fap_search_subtitle),
                icon = Icons.Filled.Search,
                onClick = { navController.navigateSafely(FapFolderSearchRoute()) },
            ),
        ),
        extraLibrarySections = emptyList(),
        profileExtraQuickActions = authEntries,
        customizeEntries = listOf(rememberPlayerSettingsEntry(navController, SiteType.FAPFOLDER)),
    )
}
