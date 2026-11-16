package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.ChoiceDialog
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.SettingIconBadge
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.SettingTile
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.ui.screen.SiteCardSettings
import com.yenaly.han1meviewer.ui.screen.SiteHomeSettings
import com.yenaly.han1meviewer.ui.screen.SiteImageSettings
import com.yenaly.han1meviewer.ui.screen.SiteLayoutSettings
import com.yenaly.han1meviewer.ui.screen.SiteSearchSettings
import com.yenaly.han1meviewer.ui.screen.rememberSiteGridColumns
import kotlinx.coroutines.launch

data class SiteSettingsEntry(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

object SiteSettingsRegistry {
    var site by mutableStateOf<SiteType?>(null)
        private set
    var entries by mutableStateOf<List<SiteSettingsEntry>>(emptyList())
        private set

    fun publish(site: SiteType, entries: List<SiteSettingsEntry>) {
        if (this.site != site) this.site = site
        this.entries = entries
    }

    val activeEntries: List<SiteSettingsEntry>
        get() = if (site == Preferences.siteType) entries else emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSettingsHubScreen(
    site: SiteType,
    siteEntries: List<SiteSettingsEntry>,
    onBack: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
    onOpenUnifiedHistory: (() -> Unit)? = null,
    onResetSite: (suspend () -> Unit)? = null,
) {
    SideEffect { SiteSettingsRegistry.publish(site, siteEntries) }
    val siteLabel = CrossSiteNavigator.siteLabel(site)
    val scope = rememberCoroutineScope()
    var showResetConfirm by remember { mutableStateOf(false) }
    val resetDoneText = stringResource(R.string.site_reset_done, siteLabel)
    val resetFailedText = stringResource(R.string.site_reset_failed)
    val hasGridColumns = site in SiteLayoutSettings.gridColumnSites
    val gridColumns = rememberSiteGridColumns(site)
    var showColumnsDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showRetriesDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    val hasCache = site in SiteHomeSettings.cacheSites
    var cacheMinutes by remember(site) { mutableStateOf(SiteHomeSettings.cacheMinutes(site)) }
    val imageOptions = SiteImageSettings.options(site)
    val sortChoices = remember(site) { SiteSearchSettings.sortChoices(site) }
    var defaultSort by remember(site) { mutableStateOf(SiteSearchSettings.defaultSort(site).orEmpty()) }
    val cardAspect = if (site in SiteCardSettings.sites) SiteCardSettings.aspect(site) else SiteCardSettings.Aspect.Default
    val hasCardOptions = site in SiteCardSettings.sites
    val homeSections = remember(site) { SiteHomeSettings.sectionsOf(site) }
    val canReset = onResetSite != null || hasGridColumns || hasCardOptions ||
        homeSections.isNotEmpty() || sortChoices != null

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

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text("$siteLabel · ${stringResource(R.string.settings)}") },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
        ) {
            item(key = "intro") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = siteLabel, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = stringResource(R.string.site_settings_intro, siteLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Site-specific options come first: they are what the person opened this hub for.
            if (siteEntries.isNotEmpty()) {
                item { HubSectionTitle(stringResource(R.string.site_settings_2, siteLabel)) }
                items(siteEntries.size, key = { siteEntries[it].key }) { index ->
                    HubRow(siteEntries[index])
                }
            }

            if (hasGridColumns || hasCardOptions) {
                item { HubSectionTitle(stringResource(R.string.site_layout_section)) }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.site_preset_title),
                        summary = stringResource(R.string.site_preset_summary),
                        onClick = { showPresetDialog = true },
                    )
                }
            }
            if (hasGridColumns) {
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.site_grid_columns_title),
                        summary = stringResource(R.string.site_grid_columns_summary),
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
                        valueText = stringResource(cardAspect.labelRes()),
                        onClick = { showAspectDialog = true },
                    )
                }
            }

            if (sortChoices != null) {
                item { HubSectionTitle(stringResource(R.string.site_search_section)) }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.site_default_sort_title),
                        summary = stringResource(R.string.site_default_sort_summary),
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
                        checked = SiteHomeSettings.isShown(section),
                        onCheckedChange = { SiteHomeSettings.setShown(section, it) },
                    )
                }
            }

            if (imageOptions != null) {
                item { HubSectionTitle(stringResource(R.string.site_image_section)) }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.site_image_retries_title),
                        summary = stringResource(R.string.site_image_retries_summary),
                        valueText = stringResource(imageOptions.retries.retriesLabelRes()),
                        onClick = { showRetriesDialog = true },
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.site_image_fade_title),
                        summary = stringResource(R.string.site_image_fade_summary),
                        checked = imageOptions.fade,
                        onCheckedChange = { SiteImageSettings.setFade(site, it) },
                    )
                }
                item {
                    SettingSwitchItem(
                        title = stringResource(R.string.site_image_reduced_title),
                        summary = stringResource(R.string.site_image_reduced_summary),
                        checked = imageOptions.reduced,
                        onCheckedChange = { SiteImageSettings.setReduced(site, it) },
                    )
                }
            }

            // Global settings (appearance, language, ...) live in the app settings only.
            item { HubSectionTitle(stringResource(R.string.site_all_app_settings)) }
            item {
                HubRow(
                    SiteSettingsEntry(
                        key = "app_settings",
                        title = stringResource(R.string.settings),
                        subtitle = stringResource(R.string.site_playback_language_backup_cache_update),
                        icon = Icons.Default.Apps,
                        onClick = onOpenAppSettings,
                    )
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.player_settings),
                    iconRes = R.drawable.ic_settings_player_24,
                    onClick = onOpenPlayerSettings,
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.network_settings),
                    iconRes = R.drawable.ic_settings_network_24,
                    onClick = onOpenNetworkSettings,
                )
            }

            if (canReset) {
                item { HubSectionTitle(stringResource(R.string.site_reset_section)) }
                item {
                    SettingNavigationItem(
                        title = stringResource(R.string.site_reset_title, siteLabel),
                        summary = stringResource(R.string.site_reset_summary),
                        onClick = { showResetConfirm = true },
                    )
                }
            }

            if (onOpenUnifiedHistory != null) {
                item { HubSectionTitle(stringResource(R.string.site_history)) }
                item {
                    HubRow(
                        SiteSettingsEntry(
                            key = "unified_history",
                            title = stringResource(R.string.site_watch_history_all_sites),
                            subtitle = stringResource(R.string.site_touch_an_item_of_another_site),
                            icon = Icons.Default.History,
                            onClick = onOpenUnifiedHistory,
                        )
                    )
                }
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

    if (canReset) {
        ConfirmDialog(
            visible = showResetConfirm,
            title = stringResource(R.string.site_reset_title, siteLabel),
            message = stringResource(R.string.site_reset_message, siteLabel),
            confirmText = stringResource(R.string.site_reset_confirm),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                showResetConfirm = false
                scope.launch {
                    runCatching {
                        SiteLayoutSettings.reset(site)
                        defaultSort = ""
                        cacheMinutes = SiteHomeSettings.DEFAULT_CACHE_MINUTES
                        onResetSite?.invoke()
                    }
                        .onSuccess {
                            GlobalToasts.show(resetDoneText, level = GlobalToasts.ToastLevel.SUCCESS)
                        }
                        .onFailure {
                            GlobalToasts.show(resetFailedText, level = GlobalToasts.ToastLevel.ERROR)
                        }
                }
            },
            onDismiss = { showResetConfirm = false },
        )
    }
}

@Composable
private fun HubSectionTitle(text: String) {
    SettingSectionHeader(text)
}

@Composable
private fun HubRow(entry: SiteSettingsEntry) {
    SettingTile(onClick = entry.onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingIconBadge(imageVector = entry.icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
