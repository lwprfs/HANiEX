package com.yenaly.han1meviewer.SitesHub.RouVideo.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.util.translation.AppTranslation
import com.yenaly.han1meviewer.ui.navigation.settings.TranslationSiteRoute
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.library.RouVideoLibraryTab
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.gated
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

@Composable
fun rouVideoHubConfig(navigator: Navigator, activity: MainActivity): SiteHubConfig {
    val translation = if (AppTranslation.available) {
        SiteHubEntry(
            key = "translation",
            title = stringResource(R.string.rouvideo_tr_title),
            subtitle = stringResource(R.string.rouvideo_tr_privacy),
            icon = Icons.Filled.Translate,
            onClick = { navigator.navigateSafely(TranslationSiteRoute(com.yenaly.han1meviewer.SiteType.ROUVIDEO.name)) },
        )
    } else null
    val loggedIn = RouVideoAuth.isLoggedIn
    val onLogin = { navigator.navigateSafely(RouVideoAccountRoute) }
    fun library(tab: RouVideoLibraryTab) = { navigator.navigateSafely(RouVideoLibraryRoute(tab.name)) }
    val follows = SiteHubEntry(
        key = "follows",
        title = stringResource(R.string.rouvideo_my_follows),
        icon = Icons.Filled.Subscriptions,
        onClick = library(RouVideoLibraryTab.FOLLOWS),
    ).gated(loggedIn, onLogin)
    val accountEntries = buildList {
        add(
            SiteHubEntry(
                key = "account",
                title = stringResource(R.string.rouvideo_account_title),
                subtitle = stringResource(R.string.login_status_tap_account),
                icon = Icons.Filled.AccountCircle,
                onClick = { navigator.navigateSafely(RouVideoAccountRoute) },
            ),
        )
        add(
            SiteHubEntry(
                key = "edit_profile",
                title = stringResource(R.string.rouvideo_edit_profile),
                icon = Icons.Filled.Edit,
                onClick = { navigator.navigateSafely(RouVideoProfileEditRoute) },
            ).gated(loggedIn, onLogin),
        )
    }
    return standardSiteHubConfig(
        site = SiteType.ROUVIDEO,
        navigator = navigator,
        activity = activity,
        siteSettingsRoute = RouVideoSettingsHubRoute,
        subscriptionSections = listOf(
            SiteHubSection(
                null,
                listOf(
                    follows,
                    SiteHubEntry(
                        key = "favorites",
                        title = stringResource(R.string.rouvideo_my_favorites),
                        icon = Icons.Filled.Favorite,
                        onClick = library(RouVideoLibraryTab.FAVORITES),
                    ).gated(loggedIn, onLogin),
                    SiteHubEntry(
                        key = "cloud_history",
                        title = stringResource(R.string.rouvideo_online_watch_history),
                        icon = Icons.Filled.Cloud,
                        onClick = library(RouVideoLibraryTab.HISTORY),
                    ).gated(loggedIn, onLogin),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "categories",
                title = stringResource(R.string.rouvideo_categories),
                subtitle = stringResource(R.string.rouvideo_categories_summary),
                icon = Icons.Filled.Category,
                onClick = { navigator.navigateSafely(RouVideoCategoriesRoute) },
            ),
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.rouvideo_history_title),
                subtitle = stringResource(R.string.rouvideo_history_summary),
                icon = Icons.Filled.History,
                onClick = { navigator.navigateSafely(RouVideoHistoryRoute) },
            ),
        ),
        profileExtraQuickActions = listOfNotNull(translation) + accountEntries,
        customizeEntries = listOfNotNull(
            SiteHubEntry(
                key = "host",
                title = stringResource(R.string.drawer_host_mirror),
                subtitle = stringResource(R.string.rouvideo_host_subtitle),
                icon = Icons.Filled.Dns,
                onClick = { navigator.navigateSafely(RouVideoHostSettingsRoute) },
            ),
            SiteHubEntry(
                key = "site_settings",
                title = stringResource(R.string.rouvideo_settings_title),
                subtitle = stringResource(R.string.rouvideo_settings_summary),
                icon = Icons.Filled.Tune,
                onClick = { navigator.navigateSafely(RouVideoSettingsRoute) },
            ),
            translation,
            rememberPlayerSettingsEntry(navigator, SiteType.ROUVIDEO),
        ),
    )
}
