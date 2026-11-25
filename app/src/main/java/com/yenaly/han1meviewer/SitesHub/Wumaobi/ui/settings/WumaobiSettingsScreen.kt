package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.component.SettingChoiceDialogItem
import com.yenaly.han1meviewer.ui.component.SettingActionItem
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiSettings
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiNetwork
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingRadioItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.PawSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WumaobiSettingsScreen(onBack: () -> Unit) {
    var quality by remember { mutableStateOf(WumaobiSettings.preferredQuality) }
    var cookiesCleared by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(WumaobiSettings.sectionOrder) }
    var hidden by remember { mutableStateOf(WumaobiSettings.hiddenSections) }
    val sectionsByKey = remember { WumaobiConstants.SECTIONS.associateBy { it.key } }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.site_settings_2, "Wumaobi")) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item { SettingSectionHeader(stringResource(R.string.wumaobi_home_sections)) }
            item { SettingHintText(stringResource(R.string.wumaobi_home_sections_hint)) }
            order.forEachIndexed { index, key ->
                val section = sectionsByKey[key] ?: return@forEachIndexed
                item(key = "section_$key") {
                    SettingActionItem(title = section.title) {
                        IconButton(
                            onClick = {
                                order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                                WumaobiSettings.sectionOrder = order
                            },
                            enabled = index > 0,
                        ) { Icon(Icons.Filled.ArrowUpward, contentDescription = null) }
                        IconButton(
                            onClick = {
                                order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
                                WumaobiSettings.sectionOrder = order
                            },
                            enabled = index < order.lastIndex,
                        ) { Icon(Icons.Filled.ArrowDownward, contentDescription = null) }
                        PawSwitch(
                            checked = key !in hidden,
                            onCheckedChange = { on ->
                                hidden = if (on) hidden - key else hidden + key
                                WumaobiSettings.hiddenSections = hidden
                            },
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        )
                    }
                }
            }

            item {
                SettingChoiceDialogItem(
                    title = stringResource(R.string.wumaobi_pref_quality),
                    dialogMessage = stringResource(R.string.wumaobi_pref_quality_hint),
                    options = WumaobiConstants.QUALITIES.map { it to it },
                    selectedValue = quality,
                    onSelect = {
                        quality = it
                        WumaobiSettings.preferredQuality = it
                    },
                )
            }

            item { SettingSectionHeader(stringResource(R.string.wumaobi_troubleshooting)) }
            item {
                SettingNavigationItem(
                    title = if (cookiesCleared) stringResource(R.string.wumaobi_cf_cleared)
                    else stringResource(R.string.wumaobi_cf_clear),
                    icon = Icons.Filled.CleaningServices,
                    onClick = {
                        WumaobiNetwork.clearCookies()
                        cookiesCleared = true
                    },
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.wumaobi_reset_defaults),
                    icon = Icons.Filled.Restore,
                    onClick = {
                        WumaobiSettings.resetToDefaults()
                        quality = WumaobiSettings.preferredQuality
                        order = WumaobiSettings.sectionOrder
                        hidden = WumaobiSettings.hiddenSections
                    },
                )
            }
        }
    }
}
