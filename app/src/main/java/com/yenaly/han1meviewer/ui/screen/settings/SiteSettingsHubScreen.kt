package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterSettings
import com.yenaly.han1meviewer.SitesHub.XHamster.common.rememberXHamsterFlag
import com.yenaly.han1meviewer.ui.component.ChoiceDialog
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.ui.screen.SiteCardSettings
import com.yenaly.han1meviewer.ui.screen.SiteHomeSettings
import com.yenaly.han1meviewer.ui.screen.SiteImageCardSettings
import com.yenaly.han1meviewer.ui.screen.SiteImageSettings
import com.yenaly.han1meviewer.ui.screen.SiteLayoutSettings
import com.yenaly.han1meviewer.ui.screen.SiteSearchSettings
import com.yenaly.han1meviewer.ui.screen.SiteShortsCardSettings
import com.yenaly.han1meviewer.ui.screen.rememberSiteGridColumns

internal var siteSettingsResetTick by mutableIntStateOf(0)

@Composable
fun SiteLayoutCardsScreen(site: SiteType) {
    val resetTick = siteSettingsResetTick
    val showChannels = rememberXHamsterFlag(XHamsterSettings.KEY_SHOW_CHANNELS)
    val showCreators = rememberXHamsterFlag(XHamsterSettings.KEY_SHOW_TOP_CREATORS)
    val siteLabel = CrossSiteNavigator.siteLabel(site)
    val hasGridColumns = site in SiteLayoutSettings.gridColumnSites
    val gridColumns = rememberSiteGridColumns(site)
    var showColumnsDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showRetriesDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    val hasCache = site in SiteHomeSettings.cacheSites
    var cacheMinutes by remember(site, resetTick) { mutableStateOf(SiteHomeSettings.cacheMinutes(site)) }
    val imageOptions = SiteImageSettings.options(site)
    val sortChoices = remember(site) { SiteSearchSettings.sortChoices(site) }
    var defaultSort by remember(site, resetTick) { mutableStateOf(SiteSearchSettings.defaultSort(site).orEmpty()) }
    val cardAspect = if (site in SiteCardSettings.sites) SiteCardSettings.aspect(site) else SiteCardSettings.Aspect.Default
    val hasCardOptions = site in SiteCardSettings.sites
    val hasShortCardOptions = site in SiteShortsCardSettings.sites
    val hasImageCardOptions = site in SiteImageCardSettings.sites
    val homeSections = remember(site) { SiteHomeSettings.sectionsOf(site) }
    val hiddenDetails = if (hasCardOptions) {
        SiteCardSettings.Detail.entries.count { !SiteCardSettings.isShown(site, it) }
    } else 0
    val hiddenSections = homeSections.count { !SiteHomeSettings.isShown(it) }
    val summaryParts = buildList {
        if (hasGridColumns && gridColumns != SiteLayoutSettings.FOLLOW_APP) {
            add(stringResource(R.string.site_summary_columns, gridColumns))
        }
        if (hasCardOptions && cardAspect != SiteCardSettings.Aspect.Default) {
            add(stringResource(R.string.site_summary_shape, stringResource(cardAspect.labelRes())))
        }
        if (hiddenDetails > 0) add(stringResource(R.string.site_summary_hidden_details, hiddenDetails))
        if (hiddenSections > 0) add(stringResource(R.string.site_summary_hidden_sections, hiddenSections))
        if (defaultSort.isNotEmpty()) add(stringResource(R.string.site_summary_sort))
        if (hasCache && cacheMinutes != SiteHomeSettings.DEFAULT_CACHE_MINUTES) {
            add(stringResource(R.string.site_summary_refresh, cacheMinutes))
        }
        if (imageOptions?.reduced == true) add(stringResource(R.string.site_summary_small_images))
        if (imageOptions != null && imageOptions.retries != SiteImageSettings.DEFAULT_RETRIES) {
            add(stringResource(R.string.site_summary_retries, imageOptions.retries))
        }
    }
    val summaryText = if (summaryParts.isEmpty()) {
        stringResource(R.string.site_summary_default)
    } else {
        summaryParts.joinToString(" · ")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
    ) {
        item(key = "intro") {
            SettingSectionHeader(siteLabel)
            SettingHintText(stringResource(R.string.site_settings_intro, siteLabel))
            SettingHintText(summaryText)
        }

        if (hasGridColumns || hasCardOptions) {
            item { HubSectionTitle(stringResource(R.string.site_layout_section)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.site_preset_title),
                    summary = stringResource(R.string.site_preset_summary),
                    icon = Icons.Default.Tune,
                    onClick = { showPresetDialog = true },
                )
            }
        }
        if (hasGridColumns) {
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.site_grid_columns_title),
                    summary = stringResource(R.string.site_grid_columns_summary),
                    icon = Icons.Default.GridView,
                    valueText = if (gridColumns == SiteLayoutSettings.FOLLOW_APP) {
                        stringResource(R.string.site_grid_columns_follow_app)
                    } else {
                        gridColumns.toString()
                    },
                    onClick = { showColumnsDialog = true },
                )
            }
        }

        if (hasCardOptions) {
            item { HubSectionTitle(stringResource(R.string.site_cards_section)) }
            items(SiteCardSettings.Detail.entries.size) { index ->
                val detail = SiteCardSettings.Detail.entries[index]
                SettingSwitchItem(
                    title = stringResource(
                        when (detail) {
                            SiteCardSettings.Detail.Views -> R.string.site_card_show_views
                            SiteCardSettings.Detail.Duration -> R.string.site_card_show_duration
                            SiteCardSettings.Detail.Subtitle -> R.string.site_card_show_subtitle
                            SiteCardSettings.Detail.Date -> R.string.site_card_show_date
                        }
                    ),
                    icon = when (detail) {
                        SiteCardSettings.Detail.Views -> Icons.Default.Visibility
                        SiteCardSettings.Detail.Duration -> Icons.Default.Timer
                        SiteCardSettings.Detail.Subtitle -> Icons.Default.Subtitles
                        SiteCardSettings.Detail.Date -> Icons.Default.CalendarMonth
                    },
                    checked = SiteCardSettings.isShown(site, detail),
                    onCheckedChange = { SiteCardSettings.setShown(site, detail, it) },
                )
            }
        }

        if (hasCardOptions) {
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.site_card_aspect_title),
                    summary = stringResource(R.string.site_card_aspect_summary),
                    icon = Icons.Default.AspectRatio,
                    valueText = stringResource(cardAspect.labelRes()),
                    onClick = { showAspectDialog = true },
                )
            }
        }

        if (hasShortCardOptions) {
            item { HubSectionTitle(stringResource(R.string.site_shorts_cards_section)) }
            items(SiteShortsCardSettings.Detail.entries.size) { index ->
                val detail = SiteShortsCardSettings.Detail.entries[index]
                SettingSwitchItem(
                    title = stringResource(
                        when (detail) {
                            SiteShortsCardSettings.Detail.Views -> R.string.site_card_show_views
                            SiteShortsCardSettings.Detail.Duration -> R.string.site_card_show_duration
                            SiteShortsCardSettings.Detail.GifBadge -> R.string.site_shorts_card_gif_badge
                            SiteShortsCardSettings.Detail.Title -> R.string.site_card_show_title
                        }
                    ),
                    icon = when (detail) {
                        SiteShortsCardSettings.Detail.Views -> Icons.Default.Visibility
                        SiteShortsCardSettings.Detail.Duration -> Icons.Default.Timer
                        SiteShortsCardSettings.Detail.GifBadge -> Icons.Default.GridView
                        SiteShortsCardSettings.Detail.Title -> Icons.Default.Subtitles
                    },
                    checked = SiteShortsCardSettings.isShown(site, detail),
                    onCheckedChange = { SiteShortsCardSettings.setShown(site, detail, it) },
                )
            }
            item {
                val shortAspect = SiteShortsCardSettings.aspect(site)
                SettingNavigationItem(
                    title = stringResource(R.string.site_card_aspect_title),
                    summary = stringResource(R.string.site_card_aspect_summary),
                    icon = Icons.Default.AspectRatio,
                    valueText = stringResource(shortAspect.labelRes()),
                    onClick = {
                        val all = SiteCardSettings.Aspect.entries
                        SiteShortsCardSettings.setAspect(site, all[(all.indexOf(shortAspect) + 1) % all.size])
                    },
                )
            }
            item {
                val shortColumns = SiteShortsCardSettings.columns(site)
                SettingNavigationItem(
                    title = stringResource(R.string.site_grid_columns_title),
                    summary = stringResource(R.string.site_grid_columns_summary),
                    icon = Icons.Default.GridView,
                    valueText = shortColumns.toString(),
                    onClick = {
                        val all = SiteShortsCardSettings.COLUMN_CHOICES
                        SiteShortsCardSettings.setColumns(site, all[(all.indexOf(shortColumns) + 1) % all.size])
                    },
                )
            }
        }

        if (hasImageCardOptions) {
            item { HubSectionTitle(stringResource(R.string.site_image_cards_section)) }
            items(SiteImageCardSettings.Detail.entries.size) { index ->
                val detail = SiteImageCardSettings.Detail.entries[index]
                SettingSwitchItem(
                    title = stringResource(
                        when (detail) {
                            SiteImageCardSettings.Detail.Title -> R.string.site_card_show_title
                            SiteImageCardSettings.Detail.Badges -> R.string.site_image_card_show_badges
                            SiteImageCardSettings.Detail.Subtitle -> R.string.site_image_card_show_subtitle
                        }
                    ),
                    icon = when (detail) {
                        SiteImageCardSettings.Detail.Title -> Icons.Default.Subtitles
                        SiteImageCardSettings.Detail.Badges -> Icons.Default.Visibility
                        SiteImageCardSettings.Detail.Subtitle -> Icons.Default.CalendarMonth
                    },
                    checked = SiteImageCardSettings.isShown(site, detail),
                    onCheckedChange = { SiteImageCardSettings.setShown(site, detail, it) },
                )
            }
            item {
                val imageAspect = SiteImageCardSettings.aspect(site)
                SettingNavigationItem(
                    title = stringResource(R.string.site_card_aspect_title),
                    summary = stringResource(R.string.site_card_aspect_summary),
                    icon = Icons.Default.AspectRatio,
                    valueText = stringResource(imageAspect.labelRes()),
                    onClick = {
                        val all = SiteCardSettings.Aspect.entries
                        SiteImageCardSettings.setAspect(site, all[(all.indexOf(imageAspect) + 1) % all.size])
                    },
                )
            }
        }

        if (sortChoices != null) {
            item { HubSectionTitle(stringResource(R.string.site_search_section)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.site_default_sort_title),
                    summary = stringResource(R.string.site_default_sort_summary),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    valueText = sortChoices.firstOrNull { it.second == defaultSort }?.first
                        ?: stringResource(R.string.site_default_sort_none),
                    onClick = { showSortDialog = true },
                )
            }
        }

        if (homeSections.isNotEmpty() || hasCache) {
            item { HubSectionTitle(stringResource(R.string.site_home_sections)) }
            if (hasCache) {
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.site_home_cache_title),
                        summary = stringResource(R.string.site_home_cache_summary),
                        icon = Icons.Default.Update,
                        valueText = stringResource(R.string.site_minutes, cacheMinutes),
                        onClick = { showCacheDialog = true },
                    )
                }
            }
            items(homeSections.size) { index ->
                val section = homeSections[index]
                SettingSwitchItem(
                    title = stringResource(
                        when (section) {
                            SiteHomeSettings.Section.FapFolderTrending -> R.string.fap_trending
                            SiteHomeSettings.Section.FapFolderSuggested -> R.string.fap_suggested_communities
                        }
                    ),
                    icon = when (section) {
                        SiteHomeSettings.Section.FapFolderTrending -> Icons.AutoMirrored.Filled.TrendingUp
                        SiteHomeSettings.Section.FapFolderSuggested -> Icons.Default.Groups
                    },
                    checked = SiteHomeSettings.isShown(section),
                    onCheckedChange = { SiteHomeSettings.setShown(section, it) },
                )
            }
        }

        if (site == SiteType.XHAMSTER) {
            item { HubSectionTitle(stringResource(R.string.hub_xhamster_home_sections)) }
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.hub_xhamster_show_channels),
                    summary = stringResource(R.string.hub_xhamster_show_channels_desc),
                    checked = showChannels,
                    onCheckedChange = { XHamsterSettings.showChannels = it },
                )
            }
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.hub_xhamster_show_top_creators),
                    summary = stringResource(R.string.hub_xhamster_show_top_creators_desc),
                    checked = showCreators,
                    onCheckedChange = { XHamsterSettings.showTopCreators = it },
                )
            }
        }

        if (imageOptions != null) {
            item { HubSectionTitle(stringResource(R.string.site_image_section)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.site_image_retries_title),
                    summary = stringResource(R.string.site_image_retries_summary),
                    icon = Icons.Default.Refresh,
                    valueText = stringResource(imageOptions.retries.retriesLabelRes()),
                    onClick = { showRetriesDialog = true },
                )
            }
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.site_image_fade_title),
                    summary = stringResource(R.string.site_image_fade_summary),
                    icon = Icons.Default.Animation,
                    checked = imageOptions.fade,
                    onCheckedChange = { SiteImageSettings.setFade(site, it) },
                )
            }
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.site_image_reduced_title),
                    summary = stringResource(R.string.site_image_reduced_summary),
                    icon = Icons.Default.PhotoSizeSelectSmall,
                    checked = imageOptions.reduced,
                    onCheckedChange = { SiteImageSettings.setReduced(site, it) },
                )
            }
        }
    }

    if (imageOptions != null) {
        ChoiceDialog(
            visible = showRetriesDialog,
            title = stringResource(R.string.site_image_retries_title),
            message = stringResource(R.string.site_image_retries_message, siteLabel),
            options = SiteImageSettings.retryChoices.map { stringResource(it.retriesLabelRes()) to it.toString() },
            selectedValue = imageOptions.retries.toString(),
            onDismiss = { showRetriesDialog = false },
            onSelect = { value ->
                SiteImageSettings.setRetries(site, value.toIntOrNull() ?: SiteImageSettings.DEFAULT_RETRIES)
                showRetriesDialog = false
            },
        )
    }

    if (hasCache) {
        ChoiceDialog(
            visible = showCacheDialog,
            title = stringResource(R.string.site_home_cache_title),
            message = stringResource(R.string.site_home_cache_message, siteLabel),
            options = SiteHomeSettings.cacheMinuteChoices.map { stringResource(R.string.site_minutes, it) to it.toString() },
            selectedValue = cacheMinutes.toString(),
            onDismiss = { showCacheDialog = false },
            onSelect = { value ->
                val minutes = value.toIntOrNull() ?: SiteHomeSettings.DEFAULT_CACHE_MINUTES
                SiteHomeSettings.setCacheMinutes(site, minutes)
                cacheMinutes = SiteHomeSettings.cacheMinutes(site)
                showCacheDialog = false
            },
        )
    }

    if (hasCardOptions) {
        ChoiceDialog(
            visible = showAspectDialog,
            title = stringResource(R.string.site_card_aspect_title),
            message = stringResource(R.string.site_card_aspect_message, siteLabel),
            options = SiteCardSettings.Aspect.entries.map { stringResource(it.labelRes()) to it.keyValue },
            selectedValue = cardAspect.keyValue,
            onDismiss = { showAspectDialog = false },
            onSelect = { value ->
                SiteCardSettings.Aspect.entries.firstOrNull { it.keyValue == value }
                    ?.let { SiteCardSettings.setAspect(site, it) }
                showAspectDialog = false
            },
        )
    }

    if (sortChoices != null) {
        ChoiceDialog(
            visible = showSortDialog,
            title = stringResource(R.string.site_default_sort_title),
            message = stringResource(R.string.site_default_sort_message, siteLabel),
            options = listOf(stringResource(R.string.site_default_sort_none) to "") + sortChoices,
            selectedValue = defaultSort,
            onDismiss = { showSortDialog = false },
            onSelect = { value ->
                SiteSearchSettings.setDefaultSort(site, value)
                defaultSort = value
                showSortDialog = false
            },
        )
    }

    if (hasGridColumns || hasCardOptions) {
        val presetNames = mapOf(
            SiteLayoutSettings.Preset.Balanced to stringResource(R.string.site_preset_balanced),
            SiteLayoutSettings.Preset.Compact to stringResource(R.string.site_preset_compact),
            SiteLayoutSettings.Preset.Spacious to stringResource(R.string.site_preset_spacious),
            SiteLayoutSettings.Preset.Minimal to stringResource(R.string.site_preset_minimal),
        )
        ChoiceDialog(
            visible = showPresetDialog,
            title = stringResource(R.string.site_preset_title),
            message = stringResource(R.string.site_preset_message, siteLabel),
            options = SiteLayoutSettings.Preset.entries.map { (presetNames[it] ?: it.name) to it.name },
            selectedValue = "",
            onDismiss = { showPresetDialog = false },
            onSelect = { name ->
                SiteLayoutSettings.Preset.entries.firstOrNull { it.name == name }
                    ?.let { SiteLayoutSettings.applyPreset(site, it) }
                showPresetDialog = false
            },
        )
    }

    if (hasGridColumns) {
        val followApp = stringResource(R.string.site_grid_columns_follow_app)
        ChoiceDialog(
            visible = showColumnsDialog,
            title = stringResource(R.string.site_grid_columns_title),
            message = stringResource(R.string.site_grid_columns_message, siteLabel),
            options = listOf(followApp to SiteLayoutSettings.FOLLOW_APP.toString()) +
                SiteLayoutSettings.COLUMN_OPTIONS.map { it.toString() to it.toString() },
            selectedValue = gridColumns.toString(),
            onDismiss = { showColumnsDialog = false },
            onSelect = {
                SiteLayoutSettings.setGridColumns(site, it.toIntOrNull() ?: SiteLayoutSettings.FOLLOW_APP)
                showColumnsDialog = false
            },
        )
    }
}

@Composable
private fun HubSectionTitle(text: String) {
    SettingSectionHeader(text)
}

private fun SiteCardSettings.Aspect.labelRes(): Int = when (this) {
    SiteCardSettings.Aspect.Default -> R.string.site_card_aspect_default
    SiteCardSettings.Aspect.Wide -> R.string.site_card_aspect_wide
    SiteCardSettings.Aspect.Classic -> R.string.site_card_aspect_classic
    SiteCardSettings.Aspect.Square -> R.string.site_card_aspect_square
    SiteCardSettings.Aspect.Portrait -> R.string.site_card_aspect_portrait
}

private fun Int.retriesLabelRes(): Int = when (this) {
    0 -> R.string.site_image_retries_off
    4 -> R.string.site_image_retries_more
    else -> R.string.site_image_retries_standard
}
