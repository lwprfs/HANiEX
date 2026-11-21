package com.yenaly.han1meviewer.FuckingSession.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.dp
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
import com.yenaly.han1meviewer.FuckingSession.common.FuckingSessionConstants
import com.yenaly.han1meviewer.FuckingSession.common.FuckingSessionSettings
import com.yenaly.han1meviewer.FuckingSession.data.remote.FuckingSessionNetwork
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
fun FuckingSessionSettingsScreen(onBack: () -> Unit) {
    var cookiesCleared by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(FuckingSessionSettings.sectionOrder) }
    var hidden by remember { mutableStateOf(FuckingSessionSettings.hiddenSections) }
    val sectionsByKey = remember { FuckingSessionConstants.SECTIONS.associateBy { it.key } }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.site_settings_2, "FuckingSession")) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item { SettingSectionHeader(stringResource(R.string.fuckingsession_home_sections)) }
            item { SettingHintText(stringResource(R.string.fuckingsession_home_sections_hint)) }
            order.forEachIndexed { index, key ->
                val section = sectionsByKey[key] ?: return@forEachIndexed
                item(key = "section_$key") {
                    SettingActionItem(title = section.title) {
                        IconButton(
                            onClick = {
                                order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                                FuckingSessionSettings.sectionOrder = order
                            },
                            enabled = index > 0,
                        ) { Icon(Icons.Filled.ArrowUpward, contentDescription = null) }
                        IconButton(
                            onClick = {
                                order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
                                FuckingSessionSettings.sectionOrder = order
                            },
                            enabled = index < order.lastIndex,
                        ) { Icon(Icons.Filled.ArrowDownward, contentDescription = null) }
                        PawSwitch(
                            checked = key !in hidden,
                            onCheckedChange = { on ->
                                hidden = if (on) hidden - key else hidden + key
                                FuckingSessionSettings.hiddenSections = hidden
                            },
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        )
                    }
                }
            }

            item { SettingSectionHeader(stringResource(R.string.fuckingsession_troubleshooting)) }
            item {
                SettingNavigationItem(
                    title = if (cookiesCleared) stringResource(R.string.fuckingsession_cf_cleared)
                    else stringResource(R.string.fuckingsession_cf_clear),
                    icon = Icons.Filled.CleaningServices,
                    onClick = {
                        FuckingSessionNetwork.clearCookies()
                        cookiesCleared = true
                    },
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.fuckingsession_reset_defaults),
                    icon = Icons.Filled.Restore,
                    onClick = {
                        FuckingSessionSettings.resetToDefaults()
                        order = FuckingSessionSettings.sectionOrder
                        hidden = FuckingSessionSettings.hiddenSections
                    },
                )
            }
        }
    }
}
