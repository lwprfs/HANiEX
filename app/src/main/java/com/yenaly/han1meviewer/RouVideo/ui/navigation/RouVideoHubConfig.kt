package com.yenaly.han1meviewer.RouVideo.ui.navigation

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
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RouVideo.common.RouVideoTranslation
import com.yenaly.han1meviewer.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.RouVideo.ui.library.RouVideoLibraryTab
import com.yenaly.han1meviewer.ui.screen.site.SiteHubSection
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.site.SiteHubConfig
import com.yenaly.han1meviewer.ui.screen.site.SiteHubEntry
import com.yenaly.han1meviewer.ui.screen.site.rememberPlayerSettingsEntry
import com.yenaly.han1meviewer.ui.screen.site.standardSiteHubConfig

/** RouVideo: categories and history in Library; host, site settings, translation in Customize. */
@Composable
fun rouVideoHubConfig(navController: NavHostController, activity: MainActivity): SiteHubConfig {
    val translation = if (RouVideoTranslation.available) {
        SiteHubEntry(
            key = "translation",
            title = stringResource(R.string.rouvideo_tr_title),
            subtitle = stringResource(R.string.rouvideo_tr_privacy),
            icon = Icons.Filled.Translate,
            onClick = { navController.navigateSafely(RouVideoTranslationSettingsRoute) },
        )
    } else null
    fun library(tab: RouVideoLibraryTab) = { navController.navigateSafely(RouVideoLibraryRoute(tab.name)) }
    val follows = SiteHubEntry(
        key = "follows",
        title = stringResource(R.string.rouvideo_my_follows),
        icon = Icons.Filled.Subscriptions,
        onClick = library(RouVideoLibraryTab.FOLLOWS),
    )
    val loggedIn = RouVideoAuth.isLoggedIn
    val accountEntries = buildList {
        add(
            SiteHubEntry(
                key = "account",
                title = stringResource(R.string.rouvideo_account_title),
                subtitle = stringResource(R.string.login_status_tap_account),
                icon = Icons.Filled.AccountCircle,
                onClick = { navController.navigateSafely(RouVideoAccountRoute) },
            ),
        )
        if (loggedIn) {
            add(
                SiteHubEntry(
                    key = "edit_profile",
                    title = stringResource(R.string.rouvideo_edit_profile),
                    icon = Icons.Filled.Edit,
                    onClick = { navController.navigateSafely(RouVideoProfileEditRoute) },
                ),
            )
        }
    }
    return standardSiteHubConfig(
        site = SiteType.ROUVIDEO,
        navController = navController,
        activity = activity,
        siteSettingsRoute = RouVideoSettingsHubRoute,
        subscriptionSections = listOf(SiteHubSection(null, listOf(follows))),
        extraLibrarySections = listOf(
            SiteHubSection(
                title = stringResource(R.string.site_drawer_section_account),
                entries = listOf(
                    follows,
                    SiteHubEntry(
                        key = "favorites",
                        title = stringResource(R.string.rouvideo_my_favorites),
                        icon = Icons.Filled.Favorite,
                        onClick = library(RouVideoLibraryTab.FAVORITES),
                    ),
                    SiteHubEntry(
                        key = "cloud_history",
                        title = stringResource(R.string.rouvideo_cloud_history),
                        icon = Icons.Filled.Cloud,
                        onClick = library(RouVideoLibraryTab.HISTORY),
                    ),
                ),
            ),
        ),
        siteLibraryEntries = listOf(
            SiteHubEntry(
                key = "categories",
                title = stringResource(R.string.rouvideo_categories),
                subtitle = stringResource(R.string.rouvideo_categories_summary),
                icon = Icons.Filled.Category,
                onClick = { navController.navigateSafely(RouVideoCategoriesRoute) },
            ),
            SiteHubEntry(
                key = "history",
                title = stringResource(R.string.rouvideo_history_title),
                subtitle = stringResource(R.string.rouvideo_history_summary),
                icon = Icons.Filled.History,
                onClick = { navController.navigateSafely(RouVideoHistoryRoute) },
            ),
        ),
        profileExtraQuickActions = listOfNotNull(translation) + accountEntries,
        customizeEntries = listOfNotNull(
            SiteHubEntry(
                key = "host",
                title = stringResource(R.string.drawer_host_mirror),
                subtitle = stringResource(R.string.rouvideo_host_subtitle),
                icon = Icons.Filled.Dns,
                onClick = { navController.navigateSafely(RouVideoHostSettingsRoute) },
            ),
            SiteHubEntry(
                key = "site_settings",
                title = stringResource(R.string.rouvideo_settings_title),
                subtitle = stringResource(R.string.rouvideo_settings_summary),
                icon = Icons.Filled.Tune,
                onClick = { navController.navigateSafely(RouVideoSettingsRoute) },
            ),
            translation,
            rememberPlayerSettingsEntry(navController, SiteType.ROUVIDEO),
        ),
    )
}
