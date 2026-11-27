package com.yenaly.han1meviewer.ui.screen.site

import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsRedirect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.ui.navigation.main.DailyCheckInRoute
import com.yenaly.han1meviewer.ui.navigation.main.DownloadRoute
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.GifDownloadsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.ImageDownloadsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.LocalPlaylistsRoute
import com.yenaly.han1meviewer.ui.screen.library.IMAGE_DOWNLOAD_SITES
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.saveInt
import com.yenaly.han1meviewer.ui.navigation.settings.saveString
import com.yenaly.han1meviewer.ui.navigation.site.SiteCustomizeRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteLibraryRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteSubscriptionRoute
import com.yenaly.han1meviewer.ui.screen.main.siteLoginStatus

fun SiteType.supportsSubscriptions(): Boolean = when (this) {
    SiteType.MISSAV, SiteType.FAPFOLDER, SiteType.ROUVIDEO, SiteType.XHAMSTER,
    SiteType.COSPLAYTELE, SiteType.COSER, SiteType.HENTAIMAMA, SiteType.FULLVIDEOS,
    SiteType.FUCKINGSESSION, SiteType.REDGIFS, SiteType.WUMAOBI, SiteType.XXXTUBE, SiteType.COOMER, SiteType.LEAKGALLERY -> true
    else -> false
}

fun SiteType.usesPagesTab(): Boolean = when (this) {
    SiteType.COSPLAYTELE, SiteType.COSER, SiteType.HENTAIMAMA, SiteType.FULLVIDEOS, SiteType.WUMAOBI,
    SiteType.COOMER, SiteType.LEAKGALLERY -> true
    else -> false
}

@Composable
fun SiteType.subscriptionTabTitle(): String =
    if (usesPagesTab()) stringResource(R.string.hub_pages_title)
    else stringResource(R.string.my_subscribe)

fun SiteType.subscriptionTabIcon(): ImageVector =
    if (usesPagesTab()) Icons.Filled.Dashboard else Icons.Filled.Subscriptions

data class SiteHubConfig(
    val site: SiteType,
    val subscriptionSections: List<SiteHubSection>,
    val librarySections: List<SiteHubSection>,
    val profileQuickActions: List<SiteHubEntry>,
    val profileSections: List<SiteHubSection>,
    val subscriptionContent: (@Composable () -> Unit)? = null,
)

class CommonProfileEntries(
    val settings: SiteHubEntry,
    val checkIn: SiteHubEntry,
    val switchSite: SiteHubEntry,
    val allHistory: SiteHubEntry,
    val offlinePlaylist: SiteHubEntry,
    val downloads: SiteHubEntry,
    val imageDownloads: SiteHubEntry,
    val gifDownloads: SiteHubEntry,
)

@Composable
fun rememberCommonProfileEntries(
    navigator: Navigator,
    activity: MainActivity,
): CommonProfileEntries {
    val settingsTitle = stringResource(R.string.settings)
    val settingsDesc = stringResource(R.string.profile_settings_desc)
    val checkInTitle = stringResource(R.string.has_mastur)
    val checkInDesc = stringResource(R.string.hub_check_in_desc)
    val switchTitle = stringResource(R.string.switch_site)
    val switchDesc = stringResource(R.string.hub_switch_site_desc)
    val historyTitle = stringResource(R.string.drawer_watch_history_all)
    val historyDesc = stringResource(R.string.hub_all_history_desc)
    val playlistTitle = stringResource(R.string.hub_offline_playlist)
    val playlistDesc = stringResource(R.string.hub_offline_playlist_desc)
    val downloadsTitle = stringResource(R.string.hub_downloads)
    val downloadsDesc = stringResource(R.string.hub_downloads_desc)
    val imageDownloadsTitle = stringResource(R.string.hub_image_downloads)
    val imageDownloadsDesc = stringResource(R.string.hub_image_downloads_desc)
    val gifDownloadsTitle = stringResource(R.string.hub_gif_downloads)
    val gifDownloadsDesc = stringResource(R.string.hub_gif_downloads_desc)
    return CommonProfileEntries(
        settings = SiteHubEntry(
            key = "settings",
            title = settingsTitle,
            subtitle = settingsDesc,
            icon = Icons.Filled.Settings,
            onClick = { navigator.navigateSafely(HomeSettingsRoute) },
        ),
        checkIn = SiteHubEntry(
            key = "check_in",
            title = checkInTitle,
            subtitle = checkInDesc,
            icon = Icons.Filled.CheckCircle,
            onClick = { navigator.navigateSafely(DailyCheckInRoute) },
        ),
        switchSite = SiteHubEntry(
            key = "switch_site",
            title = switchTitle,
            subtitle = switchDesc,
            icon = Icons.Filled.SwapHoriz,
            onClick = { activity.requestSiteSwitch() },
        ),
        allHistory = SiteHubEntry(
            key = "all_history",
            title = historyTitle,
            subtitle = historyDesc,
            icon = Icons.Filled.History,
            onClick = { navigator.navigateSafely(UnifiedHistoryRoute) },
        ),
        offlinePlaylist = SiteHubEntry(
            key = "offline_playlist",
            title = playlistTitle,
            subtitle = playlistDesc,
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            onClick = { navigator.navigateSafely(LocalPlaylistsRoute) },
        ),
        downloads = SiteHubEntry(
            key = "downloads",
            title = downloadsTitle,
            subtitle = downloadsDesc,
            icon = Icons.Filled.Download,
            onClick = { navigator.navigateSafely(DownloadRoute) },
        ),
        imageDownloads = SiteHubEntry(
            key = "image_downloads",
            title = imageDownloadsTitle,
            subtitle = imageDownloadsDesc,
            icon = Icons.Filled.PhotoLibrary,
            onClick = { navigator.navigateSafely(ImageDownloadsRoute) },
        ),
        gifDownloads = SiteHubEntry(
            key = "gif_downloads",
            title = gifDownloadsTitle,
            subtitle = gifDownloadsDesc,
            icon = Icons.Filled.PhotoLibrary,
            onClick = { navigator.navigateSafely(GifDownloadsRoute) },
        ),
    )
}

fun EntryProviderScope<NavKey>.siteHubEntries(
    navigator: Navigator,
    site: SiteType,
    config: @Composable () -> SiteHubConfig,
) {
    if (site.supportsSubscriptions()) {
        screen<SiteSubscriptionRoute> {
            val c = config()
            val custom = c.subscriptionContent
            if (custom != null) {
                custom()
            } else {
                SiteHubListScreen(
                    title = site.subscriptionTabTitle(),
                    sections = c.subscriptionSections,
                    onBack = null,
                )
            }
        }
    }
    screen<SiteLibraryRoute> {
        val c = config()
        SiteHubListScreen(
            title = stringResource(R.string.content_library),
            sections = c.librarySections,
            onBack = null,
        )
    }
    screen<SiteProfileRoute> {
        val c = config()
        val context = LocalContext.current
        val status = siteLoginStatus(context, site)
        val account = SiteAccountHeader(
            siteLabel = CrossSiteNavigator.siteLabel(site),
            title = status.title,
            subtitle = status.subtitle,
            avatarUrl = status.avatarUrl,
            loggedIn = status.loggedIn,
            onClick = status.onClick?.let { handler -> { handler(navigator) } },
        )
        SiteProfileScreen(
            account = account,
            quickActions = c.profileQuickActions,
            sections = c.profileSections,
        )
    }
    screen<SiteCustomizeRoute> {
        SettingsRedirect(navigator)
    }
}

@Composable
fun standardSiteHubConfig(
    site: SiteType,
    navigator: Navigator,
    activity: MainActivity,
    siteLibraryEntries: List<SiteHubEntry>,
    subscriptionSections: List<SiteHubSection> = emptyList(),
    extraLibrarySections: List<SiteHubSection> = emptyList(),
    profileExtraQuickActions: List<SiteHubEntry> = emptyList(),
    includeDownloads: Boolean = true,
    includeImageDownloads: Boolean = true,
    includeGifDownloads: Boolean = true,
    subscriptionContent: (@Composable () -> Unit)? = null,
): SiteHubConfig {
    val common = rememberCommonProfileEntries(navigator, activity)
    val label = CrossSiteNavigator.siteLabel(site)
    val onDevice = buildList {
        add(common.offlinePlaylist)
        if (includeDownloads) add(common.downloads)
        if (includeImageDownloads) add(common.imageDownloads)
        if (includeGifDownloads) add(common.gifDownloads)
    }
    return SiteHubConfig(
        site = site,
        subscriptionSections = subscriptionSections,
        librarySections = buildList {
            add(SiteHubSection(stringResource(R.string.hub_section_on_device), onDevice))
            if (siteLibraryEntries.isNotEmpty()) {
                add(SiteHubSection(label, siteLibraryEntries))
            }
            addAll(extraLibrarySections)
        },
        profileQuickActions = buildList {
            add(common.settings)
            add(common.checkIn)
            add(common.switchSite)
            addAll(profileExtraQuickActions)
            add(common.allHistory)
        },
        profileSections = emptyList(),
        subscriptionContent = subscriptionContent,
    )
}
