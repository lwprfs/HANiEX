package com.yenaly.han1meviewer.SupJav.ui.settings

import com.yenaly.han1meviewer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import com.yenaly.han1meviewer.MissAV.ui.home.MissAvSubtitleSettingsRepo
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SupJav.common.SupJavConstants
import com.yenaly.han1meviewer.SupJav.common.SupJavSettings
import com.yenaly.han1meviewer.SupJav.data.remote.SupJavNetwork
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Restore
import com.yenaly.han1meviewer.ui.component.SettingActionItem
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingRadioItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupJavSettingsScreen(onBack: () -> Unit) {
    var quality by remember { mutableStateOf(SupJavSettings.preferredQuality) }
    var order by remember { mutableStateOf(SupJavSettings.serverOrder) }
    var disabled by remember { mutableStateOf(SupJavSettings.disabledServers) }
    var fallback by remember { mutableStateOf(SupJavSettings.autoFallback) }
    var language by remember { mutableStateOf(SupJavSettings.language) }
    var cookiesCleared by remember { mutableStateOf(false) }
    var subSource by remember { mutableStateOf(SupJavSettings.subtitleSource) }
    var subLanguage by remember { mutableStateOf(SupJavSettings.subtitleLanguage) }
    var subTranslate by remember { mutableStateOf(SupJavSettings.subtitleTranslate) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.supjav_subtitle_language)) },
            text = {
                LazyColumn {
                    items(MissAvSubtitleSettingsRepo.subtitleCatLanguages, key = { it.code }) { lang ->
                        RadioRow(
                            title = lang.label,
                            selected = subLanguage == lang.code,
                            onClick = {
                                subLanguage = lang.code
                                SupJavSettings.subtitleLanguage = lang.code
                                showLanguageDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.site_settings_2, "SupJav")) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item { SectionHeader(stringResource(R.string.supjav_pref_quality)) }
            item {
                HintText(
                    stringResource(R.string.supjav_pref_quality_hint)
                )
            }
            SupJavConstants.QUALITIES.forEach { option ->
                item(key = "quality_$option") {
                    RadioRow(
                        title = option,
                        selected = quality == option,
                        onClick = {
                            quality = option
                            SupJavSettings.preferredQuality = option
                        },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.supjav_servers)) }
            item {
                HintText(
                    stringResource(R.string.supjav_servers_hint)
                )
            }
            order.forEachIndexed { index, name ->
                val info = SupJavConstants.SERVERS.firstOrNull { it.name == name }
                item(key = "server_$name") {
                    ServerRow(
                        title = info?.title ?: name,
                        description = info?.description.orEmpty(),
                        enabled = name !in disabled,
                        canMoveUp = index > 0,
                        canMoveDown = index < order.lastIndex,
                        onToggle = { on ->
                            disabled = if (on) disabled - name else disabled + name
                            SupJavSettings.disabledServers = disabled
                        },
                        onMoveUp = {
                            order = order.toMutableList().apply {
                                add(index - 1, removeAt(index))
                            }
                            SupJavSettings.serverOrder = order
                        },
                        onMoveDown = {
                            order = order.toMutableList().apply {
                                add(index + 1, removeAt(index))
                            }
                            SupJavSettings.serverOrder = order
                        },
                    )
                }
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.supjav_auto_fallback),
                    subtitle = stringResource(R.string.supjav_auto_fallback_summary),
                    checked = fallback,
                    onChange = {
                        fallback = it
                        SupJavSettings.autoFallback = it
                    },
                )
            }

            item { SectionHeader(stringResource(R.string.supjav_site_language)) }
            item {
                HintText(stringResource(R.string.supjav_site_language_hint))
            }
            SupJavConstants.LANGUAGES.forEach { (code, title) ->
                item(key = "lang_$code") {
                    RadioRow(
                        title = title,
                        selected = language == code,
                        onClick = {
                            language = code
                            SupJavSettings.language = code
                        },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.supjav_subtitles)) }
            item {
                HintText(
                    stringResource(R.string.supjav_subtitles_hint)
                )
            }
            MissAvSubtitleSettingsRepo.sources.forEach { source ->
                item(key = "subsource_${source.value}") {
                    Column {
                        RadioRow(
                            title = source.label,
                            selected = subSource == source.value,
                            onClick = {
                                subSource = source.value
                                SupJavSettings.subtitleSource = source.value
                            },
                        )
                        HintText(source.description)
                    }
                }
            }
            if (subSource == MissAvSubtitleSettingsRepo.SOURCE_SUBTITLECAT) {
                item(key = "sublang") {
                    SettingNavigationItem(
                        title = stringResource(R.string.supjav_subtitle_language),
                        valueText = MissAvSubtitleSettingsRepo.languageName(subLanguage),
                        onClick = { showLanguageDialog = true },
                    )
                }
                item(key = "subtranslate") {
                    SwitchRow(
                        title = stringResource(R.string.supjav_subtitle_translate),
                        subtitle = stringResource(R.string.supjav_subtitle_translate_summary),
                        checked = subTranslate,
                        onChange = {
                            subTranslate = it
                            SupJavSettings.subtitleTranslate = it
                        },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.supjav_troubleshooting)) }
            item {
                SettingNavigationItem(
                    title = if (cookiesCleared) stringResource(R.string.supjav_cf_cleared)
                    else stringResource(R.string.supjav_cf_clear),
                    icon = Icons.Filled.CleaningServices,
                    onClick = {
                        SupJavNetwork.clearCookies()
                        cookiesCleared = true
                    },
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.supjav_reset_defaults),
                    icon = Icons.Filled.Restore,
                    onClick = {
                        SupJavSettings.resetToDefaults()
                        quality = SupJavSettings.preferredQuality
                        order = SupJavSettings.serverOrder
                        disabled = SupJavSettings.disabledServers
                        fallback = SupJavSettings.autoFallback
                        language = SupJavSettings.language
                        subSource = SupJavSettings.subtitleSource
                        subLanguage = SupJavSettings.subtitleLanguage
                        subTranslate = SupJavSettings.subtitleTranslate
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    SettingSectionHeader(text)
}

@Composable
private fun HintText(text: String) {
    SettingHintText(text)
}

@Composable
private fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    SettingRadioItem(title = title, selected = selected, onClick = onClick)
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SettingSwitchItem(
        title = title,
        summary = subtitle,
        checked = checked,
        onCheckedChange = onChange,
    )
}

@Composable
private fun ServerRow(
    title: String,
    description: String,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    SettingActionItem(
        title = title,
        summary = description.takeIf { it.isNotBlank() },
    ) {
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = null)
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = null)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
        )
    }
}
