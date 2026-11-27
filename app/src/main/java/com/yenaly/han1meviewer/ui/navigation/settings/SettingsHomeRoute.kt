package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.screen.settings.SettingsCategoryItem
import com.yenaly.han1meviewer.ui.screen.settings.SettingsListRow
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchEmpty
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchField
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchResultItem
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchTarget
import com.yenaly.han1meviewer.ui.screen.settings.rememberSettingsSearchResults
import com.yenaly.han1meviewer.ui.screen.settings.siteSettingsResetTick
import kotlinx.coroutines.launch

@Composable
fun SettingsHomeRouteScreen(
    onOpenCategory: (SettingsCategory) -> Unit,
    onOpenRoute: (NavKey) -> Unit,
) {
    val useTwoPane = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    if (useTwoPane) {
        SettingsEmptyDetailPane()
    } else {
        SettingsHomeList(
            onOpenCategory = onOpenCategory,
            onOpenRoute = onOpenRoute,
            showChevron = true,
        )
    }
}

@Composable
internal fun SettingsHomeList(
    onOpenCategory: (SettingsCategory) -> Unit,
    onOpenRoute: (NavKey) -> Unit,
    showChevron: Boolean,
    selectedCategory: SettingsCategory? = null,
    selectedSection: SiteSettingsSection? = null,
) {
    val site = Preferences.siteType
    val siteLabel = siteSettingsLabel(site)
    val siteItems = buildSiteSettingsItems(site, LocalSiteContentAvailable.current)

    val appCategories = SettingsCategory.entries.filter { it != SettingsCategory.Network }

    var query by rememberSaveable { mutableStateOf("") }
    val results = rememberSettingsSearchResults(query)
    var showResetConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val resetDoneText = stringResource(R.string.site_reset_done, siteLabel)
    val resetFailedText = stringResource(R.string.site_reset_failed)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "search") {
            SettingsSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            )
        }
        if (query.isBlank()) {
            item(key = "header_this_site") {
                SettingSectionHeader(stringResource(R.string.settings_group_this_site, siteLabel))
            }
            items(siteItems, key = { "site/${it.section.key}" }) { item ->
                SettingsListRow(
                    title = item.titleArg?.let { stringResource(item.titleRes, it) }
                        ?: stringResource(item.titleRes),
                    description = item.summaryArg?.let { stringResource(item.summaryRes, it) }
                        ?: stringResource(item.summaryRes),
                    icon = rememberVectorPainter(item.icon),
                    selected = item.section == selectedSection,
                    showChevron = showChevron && item.destination != null,
                    onClick = {
                        val destination = item.destination
                        if (destination == null) showResetConfirm = true
                        else onOpenRoute(destination)
                    },
                )
            }
            item(key = "header_all_sites") {
                SettingSectionHeader(stringResource(R.string.settings_group_all_sites))
            }
            items(appCategories, key = { it.key }) { category ->
                SettingsCategoryItem(
                    category = category,
                    selected = category == selectedCategory,
                    showChevron = showChevron,
                    onClick = { onOpenCategory(category) },
                )
            }
        } else if (results.isEmpty()) {
            item(key = "search_empty") { SettingsSearchEmpty() }
        } else {
            items(results, key = { it.id }) { result ->
                SettingsSearchResultItem(
                    result = result,
                    onClick = {

                        if (!showChevron) query = ""
                        when (val target = result.target) {
                            is SettingsSearchTarget.Category -> onOpenCategory(target.category)
                            is SettingsSearchTarget.Screen -> onOpenRoute(target.route)
                            SettingsSearchTarget.Reset -> showResetConfirm = true
                        }
                    },
                )
            }
        }
    }

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
                runCatching { resetSiteSettings(site) }
                    .onSuccess {
                        siteSettingsResetTick++
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

@Composable
private fun SettingsEmptyDetailPane() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.settings_select_category_hint),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.settings_pane_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
