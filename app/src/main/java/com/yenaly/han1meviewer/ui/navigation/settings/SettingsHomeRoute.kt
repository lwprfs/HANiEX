package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.screen.settings.SettingsCategoryItem
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchEmpty
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchField
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchResultItem
import com.yenaly.han1meviewer.ui.screen.settings.rememberSettingsSearchResults

@Composable
fun SettingsHomeRouteScreen(
    onOpenCategory: (SettingsCategory) -> Unit,
) {
    val useTwoPane = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    if (useTwoPane) {
        SettingsEmptyDetailPane()
    } else {
        var query by rememberSaveable { mutableStateOf("") }
        val results = rememberSettingsSearchResults(query)
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
                items(
                    items = SettingsCategory.entries,
                    key = { it.key },
                ) { category ->
                    SettingsCategoryItem(
                        category = category,
                        onClick = { onOpenCategory(category) },
                        showChevron = true,
                    )
                }
            } else if (results.isEmpty()) {
                item(key = "search_empty") { SettingsSearchEmpty() }
            } else {
                items(results, key = { "${it.category.key}/${it.title}" }) { result ->
                    SettingsSearchResultItem(
                        result = result,
                        onClick = { onOpenCategory(result.category) },
                    )
                }
            }
        }
    }
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
