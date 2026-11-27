package com.yenaly.han1meviewer.ui.screen.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.util.translation.AppTranslation
import com.yenaly.han1meviewer.util.translation.AppTranslation.Kind
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator

private fun kindIcon(kind: Kind): Int = when (kind) {
    Kind.TAG -> R.drawable.ic_settings_label_24
    Kind.TITLE -> R.drawable.ic_settings_movie_24
    Kind.DESC -> R.drawable.ic_settings_notes_24
    Kind.COMMENT -> R.drawable.ic_baseline_comment_24
}

@Composable
private fun SettingsSectionTitle(title: String) {
    com.yenaly.han1meviewer.ui.component.SettingSectionHeader(title)
}

@Composable
private fun PausedNotice() {
    val paused = AppTranslation.pausedForSeconds()
    if (paused > 0) {
        Text(
            text = stringResource(
                R.string.rouvideo_tr_paused,
                ((paused + 59) / 60).toInt().coerceAtLeast(1),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun TranslationSettingsScreen(
    onBack: () -> Unit,
    onOpenSite: (SiteType) -> Unit,
) {
    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.rouvideo_tr_title)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.rouvideo_tr_privacy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item { SettingsSectionTitle(stringResource(R.string.rouvideo_tr_sites)) }
            items(AppTranslation.SUPPORTED_SITES, key = { "site_" + it.name }) { site ->
                val on = AppTranslation.siteEnabled(site)
                val total = AppTranslation.stats(site).total
                SettingNavigationItem(
                    title = CrossSiteNavigator.siteLabel(site),
                    summary = if (on) {
                        stringResource(R.string.rouvideo_tr_site_summary_on, total)
                    } else {
                        stringResource(R.string.rouvideo_tr_site_summary_off)
                    },
                    iconRes = R.drawable.ic_settings_public_24,
                    onClick = { onOpenSite(site) },
                )
            }
            item { PausedNotice() }
        }
    }
}

private fun kindLabelRes(kind: Kind): Int = when (kind) {
    Kind.TAG -> R.string.rouvideo_tr_tags
    Kind.TITLE -> R.string.rouvideo_tr_titles
    Kind.DESC -> R.string.rouvideo_tr_descriptions
    Kind.COMMENT -> R.string.rouvideo_tr_comments
}

private fun kindDescRes(kind: Kind): Int = when (kind) {
    Kind.TAG -> R.string.rouvideo_tr_tags_desc
    Kind.TITLE -> R.string.rouvideo_tr_titles_desc
    Kind.DESC -> R.string.rouvideo_tr_descriptions_desc
    Kind.COMMENT -> R.string.rouvideo_tr_comments_desc
}

@Composable
fun TranslationSiteSettingsScreen(
    site: SiteType,
    onBack: () -> Unit,
    onOpenCache: () -> Unit,
) {
    val context = LocalContext.current
    val config = AppTranslation.config(site)
    val stats = AppTranslation.stats(site)
    val enabled = config.enabled
    val kinds = AppTranslation.kindsFor(site)
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.rouvideo_tr_site_title,
                            CrossSiteNavigator.siteLabel(site),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.rouvideo_tr_enable),
                    summary = stringResource(R.string.rouvideo_tr_enable_desc),
                    iconRes = R.drawable.ic_translate_24,
                    checked = enabled,
                    onCheckedChange = { config.enabled = it },
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.rouvideo_tr_what)) }
            items(kinds, key = { "kind_" + it.name }) { kind ->
                SettingSwitchItem(
                    title = stringResource(kindLabelRes(kind)),
                    summary = stringResource(kindDescRes(kind)),
                    iconRes = kindIcon(kind),
                    checked = config.isOn(kind),
                    enabled = enabled,
                    onCheckedChange = { config.set(kind, it) },
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.rouvideo_tr_cache)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.rouvideo_tr_view),
                    summary = stringResource(
                        R.string.rouvideo_tr_cache_breakdown,
                        stats.tags, stats.titles, stats.descriptions, stats.comments,
                    ),
                    valueText = stringResource(
                        R.string.rouvideo_tr_cache_usage,
                        Formatter.formatShortFileSize(context, stats.bytes),
                        stats.total,
                    ),
                    iconRes = R.drawable.ic_settings_list_24,
                    onClick = onOpenCache,
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.rouvideo_tr_clear),
                    iconRes = R.drawable.ic_baseline_delete_24,
                    enabled = stats.total > 0,
                    onClick = { confirmClear = true },
                )
            }
            item { PausedNotice() }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text(stringResource(R.string.rouvideo_tr_clear_confirm, stats.total)) },
            confirmButton = {
                TextButton(onClick = {
                    AppTranslation.clear(site)
                    confirmClear = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun TranslationCacheScreen(site: SiteType, onBack: () -> Unit) {
    var filterName by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<AppTranslation.Entry?>(null) }
    val kinds = AppTranslation.kindsFor(site)
    val filter = kinds.firstOrNull { it.name == filterName }
    val items = AppTranslation.snapshot(site, filter)
    val options = listOf<Kind?>(null) + kinds
    val labels = mapOf<Kind?, String>(
        null to stringResource(R.string.rouvideo_tr_filter_all),
        Kind.TAG to stringResource(R.string.rouvideo_tr_tags),
        Kind.TITLE to stringResource(R.string.rouvideo_tr_titles),
        Kind.DESC to stringResource(R.string.rouvideo_tr_descriptions),
        Kind.COMMENT to stringResource(R.string.rouvideo_tr_comments),
    )

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.rouvideo_tr_view_site,
                            CrossSiteNavigator.siteLabel(site),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (kinds.size > 1) {
                SiteChipRow(
                    options = options,
                    selected = filter,
                    label = { labels.getValue(it) },
                    onSelect = { filterName = it?.name.orEmpty() },
                )
            }
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.rouvideo_tr_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.kind.key + "\u0001" + it.source }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editing = entry }
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(kindIcon(entry.kind)),
                                contentDescription = labels.getValue(entry.kind),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.text,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = entry.source,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (entry.edited) {
                                    Text(
                                        text = stringResource(R.string.rouvideo_tr_edited),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            IconButton(onClick = { AppTranslation.delete(entry) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    editing?.let { entry ->
        EditTranslationDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { text ->
                AppTranslation.edit(entry, text)
                editing = null
            },
        )
    }
}

@Composable
private fun EditTranslationDialog(
    entry: AppTranslation.Entry,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(entry.kind.key, entry.source) { mutableStateOf(entry.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rouvideo_tr_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.rouvideo_tr_edit_original),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = entry.source,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.rouvideo_tr_edit_translation)) },
                    minLines = if (entry.kind.long) 4 else 2,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
