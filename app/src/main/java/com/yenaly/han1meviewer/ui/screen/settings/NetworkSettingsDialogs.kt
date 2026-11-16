package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.DohConfig
import com.yenaly.han1meviewer.ui.component.ChoiceDialog
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn

@Composable
internal fun NetworkChoiceDialog(
    title: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    ChoiceDialog(
        title = title,
        options = options,
        selectedValue = selectedValue,
        onDismiss = onDismiss,
        onSelect = onSelect,
    )
}

@Composable
internal fun ProxyDialog(
    initialType: Int,
    initialIp: String,
    initialPort: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Int) -> Unit,
) {
    var selectedType by rememberSaveable(initialType) {
        mutableStateOf(
            ProxyTypeOption.entries.firstOrNull { it.value == initialType }
                ?: ProxyTypeOption.System
        )
    }
    var ip by rememberSaveable(initialIp) { mutableStateOf(initialIp) }
    var portText by rememberSaveable(initialPort) {
        mutableStateOf(initialPort.takeIf { it >= 0 }?.toString().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.proxy)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProxyTypeOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedType == option,
                                onClick = { selectedType = option },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Checkbox(
                            checked = selectedType == option,
                            onCheckedChange = null,
                        )
                        Text(
                            when (option) {
                                ProxyTypeOption.Direct -> stringResource(R.string.direct)
                                ProxyTypeOption.System -> stringResource(R.string.system_proxy)
                                ProxyTypeOption.Http -> stringResource(R.string.http)
                                ProxyTypeOption.Socks -> stringResource(R.string.socks)
                            }
                        )
                    }
                }
                val editable =
                    selectedType == ProxyTypeOption.Http || selectedType == ProxyTypeOption.Socks
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    enabled = editable,
                    label = { Text(stringResource(R.string.host_or_ipv4)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    enabled = editable,
                    label = { Text(stringResource(R.string.port)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedType.value, ip, portText.toIntOrNull() ?: -1)
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
internal fun DelayTestDialog(
    currentHost: String,
    results: List<DelayResultUi>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.current_node_latency)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = currentHost,
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.ip }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item.ip)
                            Text(
                                text = if (item.delay >= 0) "${item.delay} ms" else stringResource(R.string.network_timeout_text),
                                color = when (item.delay) {
                                    in 0 until 100 -> Color(0xFF4CAF50)
                                    in 100..500 -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {},
    )
}

@Composable
internal fun DohTestDialog(
    currentHost: String,
    results: List<DohTestResultUi>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.doh_test_result)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = currentHost,
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.host }) { item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(item.host)
                            Text(
                                text = item.message.ifBlank {
                                    if (item.delay >= 0) {
                                        "${item.delay} ms"
                                    } else {
                                        stringResource(R.string.network_timeout_text)
                                    }
                                },
                                color = when (item.delay) {
                                    in 0 until 100 -> Color(0xFF4CAF50)
                                    in 100..500 -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                },
                            )
                            if (item.ips.isNotEmpty()) {
                                Text(
                                    text = item.ips.joinToString(),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {},
    )
}

@Composable
internal fun DohDialog(
    enabled: Boolean,
    preset: String,
    customUrl: String,
    bootstrapIps: String,
    timeoutSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String, String, String, Int) -> Unit,
) {
    var dohEnabled by rememberSaveable(enabled) { mutableStateOf(enabled) }
    var presetValue by rememberSaveable(preset) {
        mutableStateOf(preset.ifBlank { DohConfig.presets.first().key })
    }
    var customValue by rememberSaveable(customUrl) { mutableStateOf(customUrl) }
    var bootstrapValue by rememberSaveable(bootstrapIps) { mutableStateOf(bootstrapIps) }
    var timeoutValue by rememberSaveable(timeoutSeconds) { mutableStateOf(timeoutSeconds.toString()) }
    var showPresetDialog by rememberSaveable { mutableStateOf(false) }

    if (showPresetDialog) {
        NetworkChoiceDialog(
            title = stringResource(R.string.doh_preset),
            selectedValue = presetValue,
            options = DohConfig.presets.map { it.title to it.key } + (stringResource(R.string.custom) to "custom"),
            onDismiss = { showPresetDialog = false },
            onSelect = {
                presetValue = it
                showPresetDialog = false
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.doh_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = dohEnabled, onClick = { dohEnabled = !dohEnabled })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Checkbox(checked = dohEnabled, onCheckedChange = null)
                    Text(stringResource(R.string.use_doh))
                }

                SettingNavigationItem(
                    title = stringResource(R.string.doh_preset),
                    valueText = DohConfig.presets.firstOrNull { it.key == presetValue }?.title
                        ?: stringResource(R.string.custom),
                    iconRes = R.drawable.baseline_domain_24,
                    onClick = { showPresetDialog = true },
                )

                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it },
                    label = { Text(stringResource(R.string.doh_custom_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = bootstrapValue,
                    onValueChange = { bootstrapValue = it },
                    label = { Text(stringResource(R.string.doh_bootstrap_ips)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.doh_bootstrap_ips_summary)) },
                )

                OutlinedTextField(
                    value = timeoutValue,
                    onValueChange = { timeoutValue = it.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(R.string.doh_timeout_seconds)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.doh_timeout_seconds_summary)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    dohEnabled,
                    presetValue,
                    customValue.trim(),
                    bootstrapValue.trim(),
                    timeoutValue.toIntOrNull()?.coerceIn(1, 60) ?: 10,
                )
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
internal fun CustomHostsDialog(
    currentData: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable(currentData) { mutableStateOf(currentData) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_hosts)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.host_or_ipv4)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 6,
                    supportingText = { Text(stringResource(R.string.custom_hosts_format_ext)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
internal fun CustomMirrorSiteDialog(
    enabled: Boolean,
    currentUrl: String,
    appendPath: Boolean,
    testResult: String?,
    isTesting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String, Boolean) -> Unit,
    onTest: (String, Boolean) -> Unit,
) {
    var customEnabled by rememberSaveable(enabled) { mutableStateOf(enabled) }
    var text by rememberSaveable(currentUrl) { mutableStateOf(currentUrl) }
    var appendPathToApi by rememberSaveable(appendPath) { mutableStateOf(appendPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_mirror_site)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = customEnabled, onClick = { customEnabled = !customEnabled })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Checkbox(checked = customEnabled, onCheckedChange = null)
                    Text(stringResource(R.string.enable_custom_mirror_site))
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.custom_mirror_site)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.custom_mirror_site_hint)) },
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.custom_mirror_api_path_mode),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    CustomMirrorPathModeOption(
                        selected = appendPathToApi,
                        title = stringResource(R.string.custom_mirror_api_path_follow_home),
                        summary = stringResource(R.string.custom_mirror_api_path_follow_home_summary),
                        onClick = { appendPathToApi = true },
                    )
                    CustomMirrorPathModeOption(
                        selected = !appendPathToApi,
                        title = stringResource(R.string.custom_mirror_api_path_root),
                        summary = stringResource(R.string.custom_mirror_api_path_root_summary),
                        onClick = { appendPathToApi = false },
                    )
                }

                Button(
                    enabled = text.isNotBlank() && !isTesting,
                    onClick = { onTest(text.trim(), appendPathToApi) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.test_connection))
                }

                if (isTesting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                testResult?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(customEnabled, text.trim(), appendPathToApi) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
internal fun CustomMirrorPathModeOption(
    selected: Boolean,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
