package com.yenaly.han1meviewer.Cosplaytele.ui.settings

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
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleSettings
import com.yenaly.han1meviewer.Cosplaytele.data.remote.CosplayteleNetwork
import com.yenaly.han1meviewer.R
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
            item { Header(stringResource(R.string.cosplaytele_pref_quality)) }
            item { Hint(stringResource(R.string.cosplaytele_pref_quality_hint)) }
            CosplayteleConstants.QUALITIES.forEach { option ->
                item(key = "q_$option") {
                    RadioRow(option, quality == option) {
                        quality = option
                        CosplayteleSettings.preferredQuality = option
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { Header(stringResource(R.string.cosplaytele_photo_columns)) }
            item { Hint(stringResource(R.string.cosplaytele_photo_columns_hint)) }
            CosplayteleConstants.PHOTO_COLUMN_OPTIONS.forEach { n ->
                item(key = "c_$n") {
                    RadioRow(n.toString(), columns == n) {
                        columns = n
                        CosplayteleSettings.photoColumns = n
                    }
                }
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

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { Header(stringResource(R.string.cosplaytele_troubleshooting)) }
            item {
                OutlinedButton(
                    onClick = {
                        CosplayteleNetwork.clearCookies()
                        cookiesCleared = true
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(
                        stringResource(
                            if (cookiesCleared) R.string.cosplaytele_cf_cleared else R.string.cosplaytele_cf_clear
                        )
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        CosplayteleSettings.resetToDefaults()
                        quality = CosplayteleSettings.preferredQuality
                        columns = CosplayteleSettings.photoColumns
                        tapFlip = CosplayteleSettings.viewerTapToFlip
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text(stringResource(R.string.cosplaytele_reset_defaults)) }
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
