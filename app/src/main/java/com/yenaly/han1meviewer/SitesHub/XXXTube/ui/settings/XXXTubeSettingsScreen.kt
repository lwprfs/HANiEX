package com.yenaly.han1meviewer.SitesHub.XXXTube.ui.settings

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
import com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeSettings
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.remote.XXXTubeNetwork
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
fun XXXTubeSettingsScreen(onBack: () -> Unit) {
    var cookiesCleared by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(XXXTubeSettings.sectionOrder) }
    var hidden by remember { mutableStateOf(XXXTubeSettings.hiddenSections) }
    val sectionsByKey = remember { XXXTubeConstants.SECTIONS.associateBy { it.key } }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.site_settings_2, "X-X-X.tube")) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item { SettingSectionHeader(stringResource(R.string.xxxtube_home_sections)) }
            item { SettingHintText(stringResource(R.string.xxxtube_home_sections_hint)) }
            order.forEachIndexed { index, key ->
                val section = sectionsByKey[key] ?: return@forEachIndexed
                item(key = "section_$key") {
                    SettingActionItem(title = section.title) {
                        IconButton(
                            onClick = {
                                order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                                XXXTubeSettings.sectionOrder = order
                            },
                            enabled = index > 0,
                        ) { Icon(Icons.Filled.ArrowUpward, contentDescription = null) }
                        IconButton(
                            onClick = {
                                order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
                                XXXTubeSettings.sectionOrder = order
                            },
                            enabled = index < order.lastIndex,
                        ) { Icon(Icons.Filled.ArrowDownward, contentDescription = null) }
                        PawSwitch(
                            checked = key !in hidden,
                            onCheckedChange = { on ->
                                hidden = if (on) hidden - key else hidden + key
                                XXXTubeSettings.hiddenSections = hidden
                            },
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        )
                    }
                }
            }

            item { SettingSectionHeader(stringResource(R.string.xxxtube_troubleshooting)) }
            item {
                SettingNavigationItem(
                    title = if (cookiesCleared) stringResource(R.string.xxxtube_cf_cleared)
                    else stringResource(R.string.xxxtube_cf_clear),
                    icon = Icons.Filled.CleaningServices,
                    onClick = {
                        XXXTubeNetwork.clearCookies()
                        cookiesCleared = true
                    },
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.xxxtube_reset_defaults),
                    icon = Icons.Filled.Restore,
                    onClick = {
                        XXXTubeSettings.resetToDefaults()
                        order = XXXTubeSettings.sectionOrder
                        hidden = XXXTubeSettings.hiddenSections
                    },
                )
            }
        }
    }
}
