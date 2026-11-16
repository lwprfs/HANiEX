package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.DefaultSiteSwitchOptions
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem

@Composable
fun SiteVisibilitySettings() {
    var disabled by remember { mutableStateOf(Preferences.disabledSites) }
    var showDialog by remember { mutableStateOf(false) }
    val total = DefaultSiteSwitchOptions.size
    val enabledCount = DefaultSiteSwitchOptions.count { it.siteType !in disabled }

    SettingNavigationItem(
        title = stringResource(R.string.site_visibility_title),
        summary = stringResource(R.string.site_visibility_summary),
        valueText = "$enabledCount/$total",
        iconRes = R.drawable.ic_settings_apps_24,
        onClick = {
            disabled = Preferences.disabledSites
            showDialog = true
        },
    )

    if (showDialog) {
        SiteVisibilityDialog(
            disabled = disabled,
            onDisabledChange = { disabled = it },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun SiteVisibilityDialog(
    disabled: Set<com.yenaly.han1meviewer.SiteType>,
    onDisabledChange: (Set<com.yenaly.han1meviewer.SiteType>) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentSiteMessage = stringResource(R.string.site_visibility_current_site)
    val hiddenSummary = stringResource(R.string.site_visibility_hidden)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.site_visibility_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DefaultSiteSwitchOptions.forEach { option ->
                    val enabled = option.siteType !in disabled
                    SettingSwitchItem(
                        title = option.name,
                        summary = if (enabled) option.description else hiddenSummary,
                        checked = enabled,
                        onCheckedChange = { on ->
                            if (Preferences.setSiteEnabled(option.siteType, on)) {
                                onDisabledChange(Preferences.disabledSites)
                            } else {
                                GlobalToasts.show(
                                    currentSiteMessage,
                                    level = GlobalToasts.ToastLevel.WARNING,
                                )
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm)) }
        },
    )
}
