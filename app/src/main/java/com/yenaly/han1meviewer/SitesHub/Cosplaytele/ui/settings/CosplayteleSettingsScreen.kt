package com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleSettings
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.remote.CosplayteleNetwork
import com.yenaly.han1meviewer.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Restore
import com.yenaly.han1meviewer.ui.component.SettingChoiceDialogItem
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingRadioItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosplayteleSettingsScreen(onBack: () -> Unit) {
    var quality by remember { mutableStateOf(CosplayteleSettings.preferredQuality) }
    var columns by remember { mutableIntStateOf(CosplayteleSettings.photoColumns) }
    var tapFlip by remember { mutableStateOf(CosplayteleSettings.viewerTapToFlip) }
    var cookiesCleared by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.site_settings_2, "Cosplaytele")) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                SettingChoiceDialogItem(
                    title = stringResource(R.string.cosplaytele_pref_quality),
                    dialogMessage = stringResource(R.string.cosplaytele_pref_quality_hint),
                    options = CosplayteleConstants.QUALITIES.map { it to it },
                    selectedValue = quality,
                    onSelect = {
                        quality = it
                        CosplayteleSettings.preferredQuality = it
                    },
                )
            }

            item {
                SettingChoiceDialogItem(
                    title = stringResource(R.string.cosplaytele_photo_columns),
                    dialogMessage = stringResource(R.string.cosplaytele_photo_columns_hint),
                    options = CosplayteleConstants.PHOTO_COLUMN_OPTIONS.map { it.toString() to it.toString() },
                    selectedValue = columns.toString(),
                    onSelect = { picked ->
                        picked.toIntOrNull()?.let {
                            columns = it
                            CosplayteleSettings.photoColumns = it
                        }
                    },
                )
            }
            item {
                SwitchRow(
                    stringResource(R.string.cosplaytele_viewer_tap),
                    stringResource(R.string.cosplaytele_viewer_tap_summary),
                    tapFlip,
                ) {
                    tapFlip = it
                    CosplayteleSettings.viewerTapToFlip = it
                }
            }

            item { Header(stringResource(R.string.cosplaytele_troubleshooting)) }
            item {
                SettingNavigationItem(
                    title = stringResource(
                        if (cookiesCleared) R.string.cosplaytele_cf_cleared else R.string.cosplaytele_cf_clear
                    ),
                    icon = Icons.Filled.CleaningServices,
                    onClick = {
                        CosplayteleNetwork.clearCookies()
                        cookiesCleared = true
                    },
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.cosplaytele_reset_defaults),
                    icon = Icons.Filled.Restore,
                    onClick = {
                        CosplayteleSettings.resetToDefaults()
                        quality = CosplayteleSettings.preferredQuality
                        columns = CosplayteleSettings.photoColumns
                        tapFlip = CosplayteleSettings.viewerTapToFlip
                    },
                )
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    SettingSectionHeader(text)
}

@Composable
private fun Hint(text: String) {
    SettingHintText(text)
}

@Composable
private fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    SettingRadioItem(title = title, selected = selected, onClick = onClick)
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingSwitchItem(
        title = title,
        summary = subtitle,
        checked = checked,
        onCheckedChange = onChange,
    )
}
