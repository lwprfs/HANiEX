package com.yenaly.han1meviewer.ui.screen.site

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.ui.navigation.main.DailyCheckInRoute
import com.yenaly.han1meviewer.ui.navigation.main.DownloadRoute
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
import com.yenaly.han1meviewer.ui.navigation.settings.LocalPlaylistsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.saveInt
import com.yenaly.han1meviewer.ui.navigation.settings.saveString
import com.yenaly.han1meviewer.ui.navigation.site.SiteCustomizeRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteLibraryRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteSubscriptionRoute
import com.yenaly.han1meviewer.ui.screen.main.siteLoginStatus
import com.yenaly.han1meviewer.ui.screen.settings.dialog.HorizontalCardCountDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.SearchGridColumnsDialog

/**
 * Everything a site contributes to the four shared primary pages.
 *
 * Each site builds one of these in its own package (see e.g. `MissAvHubConfig`), which keeps all
 * site-specific wiring isolated: adding a library page, a profile utility or a customization
 * entry to a single site never touches the shared screens or any other site.
 *
 * Extension points:
 *  * [subscriptionSections] – leave empty when the site has no subscription concept; the page then
 *    shows an honest "unavailable" state. Fill it when real support exists.
 *  * [librarySections] – add entries (e.g. an online playlist) once the site has a data source.
 *  * [customizeExtras] – extra customization rows rendered under the shared layout controls.
 */
data class SiteHubConfig(
    val site: SiteType,
    val subscriptionSections: List<SiteHubSection>,
    val librarySections: List<SiteHubSection>,
    val profileQuickActions: List<SiteHubEntry>,
    val profileSections: List<SiteHubSection>,
    val customizeSections: List<SiteHubSection>,
    val customizeExtras: (LazyListScope.() -> Unit)? = null,
)

/** Entries every site's profile shares; each site arranges them in its own order. */
class CommonProfileEntries(
    val siteSettings: SiteHubEntry,
    val checkIn: SiteHubEntry,
    val switchSite: SiteHubEntry,
    val customize: SiteHubEntry,
    val allHistory: SiteHubEntry,
    val offlinePlaylist: SiteHubEntry,
    val downloads: SiteHubEntry,
)

@Composable
fun rememberCommonProfileEntries(
    navController: NavHostController,
    activity: MainActivity,
    site: SiteType,
    siteSettingsRoute: Any,
): CommonProfileEntries {
    val label = CrossSiteNavigator.siteLabel(site)
    val settingsTitle = stringResource(R.string.hub_site_settings_title, label)
    val settingsDesc = stringResource(R.string.hub_site_settings_desc)
    val checkInTitle = stringResource(R.string.has_cum)
    val checkInDesc = stringResource(R.string.hub_check_in_desc)
    val switchTitle = stringResource(R.string.switch_site)
    val switchDesc = stringResource(R.string.hub_switch_site_desc)
    val customizeTitle = stringResource(R.string.hub_customize_title)
    val customizeDesc = stringResource(R.string.hub_customize_desc)
    val historyTitle = stringResource(R.string.drawer_watch_history_all)
    val historyDesc = stringResource(R.string.hub_all_history_desc)
    val playlistTitle = stringResource(R.string.hub_offline_playlist)
    val playlistDesc = stringResource(R.string.hub_offline_playlist_desc)
    val downloadsTitle = stringResource(R.string.hub_downloads)
    val downloadsDesc = stringResource(R.string.hub_downloads_desc)
    return CommonProfileEntries(
        siteSettings = SiteHubEntry(
            key = "site_settings",
            title = settingsTitle,
            subtitle = settingsDesc,
            icon = Icons.Filled.Settings,
            onClick = { navController.navigateSafely(siteSettingsRoute) },
        ),
        checkIn = SiteHubEntry(
            key = "check_in",
            title = checkInTitle,
            subtitle = checkInDesc,
            icon = Icons.Filled.CheckCircle,
            onClick = { navController.navigateSafely(DailyCheckInRoute) },
        ),
        switchSite = SiteHubEntry(
            key = "switch_site",
            title = switchTitle,
            subtitle = switchDesc,
            icon = Icons.Filled.SwapHoriz,
            onClick = { activity.requestSiteSwitch() },
        ),
        customize = SiteHubEntry(
            key = "customize",
            title = customizeTitle,
            subtitle = customizeDesc,
            icon = Icons.Filled.Palette,
            onClick = { navController.navigateSafely(SiteCustomizeRoute) },
        ),
        allHistory = SiteHubEntry(
            key = "all_history",
            title = historyTitle,
            subtitle = historyDesc,
            icon = Icons.Filled.History,
            onClick = { navController.navigateSafely(UnifiedHistoryRoute) },
        ),
        offlinePlaylist = SiteHubEntry(
            key = "offline_playlist",
            title = playlistTitle,
            subtitle = playlistDesc,
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            onClick = { navController.navigateSafely(LocalPlaylistsRoute) },
        ),
        downloads = SiteHubEntry(
            key = "downloads",
            title = downloadsTitle,
            subtitle = downloadsDesc,
            icon = Icons.Filled.Download,
            onClick = { navController.navigateSafely(DownloadRoute) },
        ),
    )
}

/**
 * Registers Subscription / Library / Profile / Customize for [site] in the current NavHost.
 *
 * [config] is a composable lambda because entries need `stringResource`; it is re-evaluated
 * whenever one of these pages is composed, so account/login state is always fresh.
 */
fun NavGraphBuilder.registerSiteHubRoutes(
    navController: NavHostController,
    site: SiteType,
    config: @Composable () -> SiteHubConfig,
) {
    composable<SiteSubscriptionRoute> {
        val c = config()
        SiteHubListScreen(
            title = stringResource(R.string.my_subscribe),
            sections = c.subscriptionSections,
            onBack = null,
            emptyState = {
                SiteHubUnavailableState(
                    title = stringResource(
                        R.string.hub_subscription_unavailable_title,
                        CrossSiteNavigator.siteLabel(site),
                    ),
                    message = stringResource(R.string.hub_subscription_unavailable_message),
                    icon = Icons.Filled.Subscriptions,
                )
            },
        )
    }
    composable<SiteLibraryRoute> {
        val c = config()
        SiteHubListScreen(
            title = stringResource(R.string.content_library),
            sections = c.librarySections,
            onBack = null,
        )
    }
    composable<SiteProfileRoute> {
        val c = config()
        val context = LocalContext.current
        val status = siteLoginStatus(context, site)
        val account = SiteAccountHeader(
            siteLabel = CrossSiteNavigator.siteLabel(site),
            title = status.title,
            subtitle = status.subtitle,
            avatarUrl = status.avatarUrl,
            loggedIn = status.loggedIn,
            onClick = status.onClick?.let { handler -> { handler(navController) } },
        )
        SiteProfileScreen(
            account = account,
            quickActions = c.profileQuickActions,
            sections = c.profileSections,
        )
    }
    composable<SiteCustomizeRoute> {
        val c = config()
        SiteCustomizeScreen(
            config = c,
            onBack = { navController.popBackStackSafely() },
        )
    }
}

private enum class LayoutDialog { VideoGrid, SearchGrid, HorizontalCards }

/**
 * Customize page. Site-specific entries come from [SiteHubConfig.customizeSections]; the three
 * card-layout controls below are shared and fully independent of each other:
 *  - horizontal cards per row (horizontal category sections),
 *  - video grid columns (vertical home / list grids),
 *  - search grid columns (search results).
 */
@Composable
fun SiteCustomizeScreen(
    config: SiteHubConfig,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<LayoutDialog?>(null) }

    // Re-read on every refresh so the value texts reflect what was just saved.
    @Suppress("UNUSED_VARIABLE")
    val tick = refreshKey
    val videoConfig = Preferences.videoGridColumnsConfig
    val searchConfig = Preferences.searchGridColumnsConfig
    val cardConfig = Preferences.horizontalCardCountConfig

    when (dialog) {
        LayoutDialog.VideoGrid -> SearchGridColumnsDialog(
            initialConfig = videoConfig,
            title = stringResource(R.string.site_video_grid_columns),
            onDismiss = { dialog = null },
            onConfirm = { cfg ->
                dialog = null
                saveInt(SettingsPreferenceKeys.VIDEO_GRID_COLUMNS_COMPACT, cfg.compactColumns)
                saveInt(SettingsPreferenceKeys.VIDEO_GRID_COLUMNS_MEDIUM, cfg.mediumColumns)
                saveInt(SettingsPreferenceKeys.VIDEO_GRID_COLUMNS_EXPANDED, cfg.expandedColumns)
                saveInt(SettingsPreferenceKeys.VIDEO_GRID_COLUMNS_LARGE, cfg.largeColumns)
                refreshKey++
            },
        )

        LayoutDialog.SearchGrid -> SearchGridColumnsDialog(
            initialConfig = searchConfig,
            onDismiss = { dialog = null },
            onConfirm = { cfg ->
                dialog = null
                saveInt(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_COMPACT, cfg.compactColumns)
                saveInt(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_MEDIUM, cfg.mediumColumns)
                saveInt(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_EXPANDED, cfg.expandedColumns)
                saveInt(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_LARGE, cfg.largeColumns)
                refreshKey++
            },
        )

        LayoutDialog.HorizontalCards -> HorizontalCardCountDialog(
            initialConfig = cardConfig,
            onDismiss = { dialog = null },
            onConfirm = { cfg ->
                dialog = null
                saveString(SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_NARROW, cfg.narrowCount.toString())
                saveString(SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_COMPACT, cfg.compactCount.toString())
                saveString(SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_MEDIUM, cfg.mediumCount.toString())
                saveString(SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_EXPANDED, cfg.expandedCount.toString())
                refreshKey++
            },
        )

        null -> Unit
    }

    HanimeScaffold(
        title = stringResource(R.string.hub_customize_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            siteHubSections(config.customizeSections)
            config.customizeExtras?.invoke(this)

            item(key = "layout_header") {
                SettingSectionHeader(title = stringResource(R.string.site_layout_column_count))
            }
            item(key = "layout_horizontal_cards") {
                SettingNavigationItem(
                    title = stringResource(R.string.hub_horizontal_cards_title),
                    iconRes = R.drawable.ic_settings_view_carousel_24,
                    summary = stringResource(R.string.site_number_of_cards_visible_in_horizontal),
                    valueText = listOf(
                        cardConfig.narrowCount,
                        cardConfig.compactCount,
                        cardConfig.mediumCount,
                        cardConfig.expandedCount,
                    ).joinToString(" / "),
                    onClick = { dialog = LayoutDialog.HorizontalCards },
                )
            }
            item(key = "layout_video_grid") {
                SettingNavigationItem(
                    title = stringResource(R.string.site_video_grid_columns),
                    iconRes = R.drawable.ic_settings_grid_view_24,
                    summary = stringResource(R.string.hub_video_grid_columns_desc),
                    valueText = listOf(
                        videoConfig.compactColumns,
                        videoConfig.mediumColumns,
                        videoConfig.expandedColumns,
                        videoConfig.largeColumns,
                    ).joinToString(" / "),
                    onClick = { dialog = LayoutDialog.VideoGrid },
                )
            }
            item(key = "layout_search_grid") {
                SettingNavigationItem(
                    title = stringResource(R.string.hub_search_grid_columns_title),
                    iconRes = R.drawable.ic_settings_grid_view_24,
                    summary = stringResource(R.string.hub_search_grid_columns_desc),
                    valueText = listOf(
                        searchConfig.compactColumns,
                        searchConfig.mediumColumns,
                        searchConfig.expandedColumns,
                        searchConfig.largeColumns,
                    ).joinToString(" / "),
                    onClick = { dialog = LayoutDialog.SearchGrid },
                )
            }
        }
    }
}

/** "Player settings" entry shared by every site's Customize page. */
@Composable
fun rememberPlayerSettingsEntry(navController: NavHostController, site: SiteType): SiteHubEntry =
    SiteHubEntry(
        key = "player_settings",
        title = stringResource(R.string.player_settings),
        icon = androidx.compose.material.icons.Icons.Filled.PlayCircleOutline,
        onClick = {
            navController.navigateSafely(
                com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute(site.name),
            )
        },
    )

/**
 * Builds the standard [SiteHubConfig] used by sites without special needs. A site only passes
 * the pages that really exist for it; nothing is listed for features it does not have.
 */
@Composable
fun standardSiteHubConfig(
    site: SiteType,
    navController: NavHostController,
    activity: MainActivity,
    siteSettingsRoute: Any,
    siteLibraryEntries: List<SiteHubEntry>,
    customizeEntries: List<SiteHubEntry>,
    subscriptionSections: List<SiteHubSection> = emptyList(),
    extraLibrarySections: List<SiteHubSection> = emptyList(),
    profileExtraQuickActions: List<SiteHubEntry> = emptyList(),
    includeDownloads: Boolean = false,
    customizeExtras: (LazyListScope.() -> Unit)? = null,
): SiteHubConfig {
    val common = rememberCommonProfileEntries(navController, activity, site, siteSettingsRoute)
    val label = CrossSiteNavigator.siteLabel(site)
    val onDevice = buildList {
        add(common.offlinePlaylist)
        if (includeDownloads) add(common.downloads)
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
            add(common.siteSettings)
            add(common.checkIn)
            add(common.switchSite)
            addAll(profileExtraQuickActions)
            add(common.customize)
            add(common.allHistory)
        },
        profileSections = emptyList(),
        customizeSections = listOf(SiteHubSection(null, customizeEntries)),
        customizeExtras = customizeExtras,
    )
}
