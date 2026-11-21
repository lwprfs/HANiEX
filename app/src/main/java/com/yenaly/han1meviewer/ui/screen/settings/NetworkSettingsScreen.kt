package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.ui.component.SettingInfoItem
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.SettingsGroupTitle
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.util.applicationContext

data class NetworkSettingsUiState(
    val domainName: String,
    val domainDisplay: String,
    val proxySummary: String,
    val useBuiltInHosts: Boolean,
    val useBackupMediaCdn: Boolean,
    val useCustomMirrorSite: Boolean,
    val customMirrorSite: String,
    val appendCustomMirrorPath: Boolean,
    val useDoH: Boolean,
    val dohSummary: String,
    val delaySummary: String,
)

data class DelayResultUi(
    val ip: String,
    val delay: Int,
)

data class DohTestResultUi(
    val host: String,
    val ips: List<String>,
    val delay: Int,
    val message: String,
)

enum class ProxyTypeOption(val value: Int) {
    Direct(HProxySelector.TYPE_DIRECT),
    System(HProxySelector.TYPE_SYSTEM),
    Http(HProxySelector.TYPE_HTTP),
    Socks(HProxySelector.TYPE_SOCKS),
}

@Composable
fun NetworkSettingsScreen(
    state: NetworkSettingsUiState,
    domainOptions: List<Pair<String, String>>,
    currentHost: String,
    delayResults: List<DelayResultUi>,
    isDelayTesting: Boolean,
    proxyType: Int,
    proxyIp: String,
    proxyPort: Int,
    dohEnabled: Boolean,
    dohPreset: String,
    dohCustomUrl: String,
    dohBootstrapIps: String,
    dohTimeoutSeconds: Int,
    dohTestResults: List<DohTestResultUi>,
    isDohTesting: Boolean,
    useCustomMirrorSite: Boolean,
    customMirrorSite: String,
    appendCustomMirrorPath: Boolean,
    customMirrorTestResult: String?,
    isCustomMirrorTesting: Boolean,
    showCustomMirror: Boolean = true,
    isJavchu: Boolean = false,
    showBackupMediaCdn: Boolean = true,
    siteName: String = "",
    siteOptions: List<Pair<String, String>> = emptyList(),
    selectedSiteKey: String = "",
    onSiteChange: (String) -> Unit = {},
    mirrorRows: List<MirrorRowUi> = emptyList(),
    isMirrorDialogOpen: Boolean = false,
    isMirrorTesting: Boolean = false,
    onOpenMirrorLatency: () -> Unit = {},
    onUseMirror: (String) -> Unit = {},
    onUseFastestMirror: () -> Unit = {},
    onRetestMirrors: () -> Unit = {},
    onDismissMirrorLatency: () -> Unit = {},
    onDomainChange: (String) -> Unit,
    onSaveCustomMirrorSite: (Boolean, String, Boolean) -> Unit,
    onTestCustomMirrorSite: (String, Boolean) -> Unit,
    useBackupMediaCdn: Boolean,
    onUseBackupMediaCdnChange: (Boolean) -> Unit,
    onUseBuiltInHostsChange: (Boolean) -> Unit,
    onSaveCustomHosts: (String) -> Unit,
    onSaveDohSettings: (Boolean, String, String, String, Int) -> Unit,
    onOpenDelayTest: () -> Unit,
    customHostsData: String,
    onOpenDohTest: () -> Unit,
    onDismissDelayTest: () -> Unit,
    onDismissDohTest: () -> Unit,
    onApplyProxy: (Int, String, Int) -> Unit,
    onOpenApplyDeepLinks: () -> Unit,
) {
    var showDomainDialog by rememberSaveable { mutableStateOf(false) }
    var showProxyDialog by rememberSaveable { mutableStateOf(false) }
    var showDohDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomHostsDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomMirrorSiteDialog by rememberSaveable { mutableStateOf(false) }
    var showSiteDialog by rememberSaveable { mutableStateOf(false) }

    if (showSiteDialog) {
        NetworkChoiceDialog(
            title = stringResource(R.string.network_settings_site),
            selectedValue = selectedSiteKey,
            options = siteOptions,
            onDismiss = { showSiteDialog = false },
            onSelect = {
                showSiteDialog = false
                onSiteChange(it)
            },
        )
    }

    if (isMirrorDialogOpen) {
        MirrorLatencyDialog(
            siteName = siteName,
            rows = mirrorRows,
            isTesting = isMirrorTesting,
            onUse = { row -> onUseMirror(row.host) },
            onUseFastest = onUseFastestMirror,
            onRetest = onRetestMirrors,
            onDismiss = onDismissMirrorLatency,
        )
    }

    if (showDomainDialog) {
        NetworkChoiceDialog(
            title = stringResource(R.string.domain_name),
            selectedValue = state.domainName,
            options = domainOptions,
            onDismiss = { showDomainDialog = false },
            onSelect = {
                showDomainDialog = false
                onDomainChange(it)
            },
        )
    }

    if (showProxyDialog) {
        ProxyDialog(
            initialType = proxyType,
            initialIp = proxyIp,
            initialPort = proxyPort,
            onDismiss = { showProxyDialog = false },
            onConfirm = { type, ip, port ->
                showProxyDialog = false
                onApplyProxy(type, ip, port)
            },
        )
    }

    if (showDohDialog) {
        DohDialog(
            enabled = dohEnabled,
            preset = dohPreset,
            customUrl = dohCustomUrl,
            bootstrapIps = dohBootstrapIps,
            timeoutSeconds = dohTimeoutSeconds,
            onDismiss = { showDohDialog = false },
            onConfirm = { enabled, preset, url, bootstrapIps, timeout ->
                showDohDialog = false
                onSaveDohSettings(enabled, preset, url, bootstrapIps, timeout)
            },
        )
    }

    if (showCustomHostsDialog) {
        CustomHostsDialog(
            currentData = customHostsData,
            onDismiss = { showCustomHostsDialog = false },
            onConfirm = { data ->
                showCustomHostsDialog = false
                onSaveCustomHosts(data)
            },
        )
    }

    if (showCustomMirrorSiteDialog) {
        CustomMirrorSiteDialog(
            enabled = useCustomMirrorSite,
            currentUrl = customMirrorSite,
            appendPath = appendCustomMirrorPath,
            testResult = customMirrorTestResult,
            isTesting = isCustomMirrorTesting,
            onDismiss = { showCustomMirrorSiteDialog = false },
            onConfirm = { enabled, url, appendPath ->
                showCustomMirrorSiteDialog = false
                onSaveCustomMirrorSite(enabled, url, appendPath)
            },
            onTest = onTestCustomMirrorSite,
        )
    }

    if (isDelayTesting) {
        DelayTestDialog(
            currentHost = currentHost,
            results = delayResults,
            onDismiss = onDismissDelayTest,
        )
    }

    if (isDohTesting) {
        DohTestDialog(
            currentHost = currentHost,
            results = dohTestResults,
            onDismiss = onDismissDohTest,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (siteOptions.isNotEmpty()) {
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.network_settings_site),
                    summary = stringResource(R.string.network_settings_site_summary),
                    valueText = siteName,
                    iconRes = R.drawable.baseline_domain_24,
                    onClick = { showSiteDialog = true },
                )
            }
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.domain_name),
                valueText = state.domainDisplay,
                iconRes = R.drawable.baseline_domain_24,
                onClick = { showDomainDialog = true },
            )
        }

        if (mirrorRows.isNotEmpty()) {
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.mirror_latency_entry),
                    summary = stringResource(R.string.mirror_latency_entry_summary, mirrorRows.size),
                    iconRes = R.drawable.baseline_delay_24,
                    onClick = onOpenMirrorLatency,
                )
            }
        }

        if (showCustomMirror) {
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.custom_mirror_site),
                    summary = if (useCustomMirrorSite && customMirrorSite.isNotBlank())
                        customMirrorSite
                    else
                        stringResource(R.string.custom_mirror_site_hint),
                    iconRes = R.drawable.baseline_domain_24,
                    onClick = { showCustomMirrorSiteDialog = true },
                )
            }
        } else if (isJavchu) {

            item {
                SettingInfoItem(
                    title = stringResource(R.string.domain_name),
                    summary = applicationContext.getString(R.string.site_javchu_uses_a_fixed_url),
                    valueText = "https://javchu.com",
                    iconRes = R.drawable.baseline_domain_24,
                )
            }
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.proxy),
                summary = state.proxySummary,
                iconRes = R.drawable.baseline_vpn_24,
                onClick = { showProxyDialog = true },
            )
        }

        if (showBackupMediaCdn) {
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.use_backup_media_cdn),
                    summary = stringResource(R.string.use_backup_media_cdn_summary),
                    checked = useBackupMediaCdn,
                    iconRes = R.drawable.baseline_domain_24,
                    onCheckedChange = onUseBackupMediaCdnChange,
                )
            }
        }

        item { SettingsGroupTitle(stringResource(R.string.builtin_dns)) }

        item {
            SettingSwitchItem(
                title = stringResource(R.string.use_built_in_hosts),
                summary = stringResource(R.string.use_built_in_hosts_summary),
                checked = state.useBuiltInHosts,
                iconRes = R.drawable.baseline_hosts_24,
                onCheckedChange = onUseBuiltInHostsChange,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.custom_hosts),
                summary = if (customHostsData.isBlank()) stringResource(R.string.custom_hosts_empty_summary) else customHostsData.take(60),
                iconRes = R.drawable.baseline_edit_24,
                onClick = { showCustomHostsDialog = true },
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.use_doh),
                summary = state.dohSummary,
                iconRes = R.drawable.baseline_doh_24,
                onClick = { showDohDialog = true },
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.debug)) }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.view_node_latency),
                summary = state.delaySummary,
                iconRes = R.drawable.baseline_delay_24,
                onClick = onOpenDelayTest,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.test_doh),
                summary = stringResource(R.string.test_doh_summary),
                iconRes = R.drawable.baseline_doh_24,
                onClick = onOpenDohTest,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(R.string.apply_deep_links),
                summary = stringResource(R.string.apply_deep_links_summary),
                iconRes = R.drawable.baseline_add_link_24,
                onClick = onOpenApplyDeepLinks,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun NetworkSettingsScreenPreview() {
    ComponentPreview {
        NetworkSettingsScreen(
            state = NetworkSettingsUiState(
                domainName = "https://hanime1.me/",
                domainDisplay = "hanime1.me (Default)",
                proxySummary = "System Proxy",
                useBuiltInHosts = false,
                useBackupMediaCdn = false,
                useCustomMirrorSite = false,
                customMirrorSite = "",
                appendCustomMirrorPath = true,
                useDoH = false,
                dohSummary = "Disabled",
                delaySummary = "Enable built-in hosts to detect latency",
            ),
            domainOptions = listOf(
                "hanime1.me (Default)" to "https://hanime1.me/",
                "hanime1.com (Backup)" to "https://hanime1.com/",
            ),
            currentHost = "https://hanime1.me/",
            delayResults = listOf(
                DelayResultUi("1.1.1.1", 82),
                DelayResultUi("8.8.8.8", 164),
                DelayResultUi("9.9.9.9", -1),
            ),
            dohTestResults = listOf(
                DohTestResultUi("hanime1.me", listOf("1.1.1.1"), 82, ""),
            ),
            isDelayTesting = false,
            isDohTesting = false,
            useCustomMirrorSite = false,
            customMirrorSite = "",
            appendCustomMirrorPath = true,
            customMirrorTestResult = null,
            isCustomMirrorTesting = false,
            showCustomMirror = true,
            isJavchu = false,
            proxyType = HProxySelector.TYPE_SYSTEM,
            proxyIp = "",
            proxyPort = -1,
            dohEnabled = false,
            dohPreset = "Cloudflare",
            dohCustomUrl = "",
            dohBootstrapIps = "1.1.1.1, 8.8.8.8",
            dohTimeoutSeconds = 10,
            onDomainChange = {},
            onSaveCustomMirrorSite = { _, _, _ -> },
            onTestCustomMirrorSite = { _, _ -> },
            useBackupMediaCdn = false,
            onUseBackupMediaCdnChange = {},
            onUseBuiltInHostsChange = {},
            onSaveCustomHosts = {},
            customHostsData = "",
            onSaveDohSettings = { _, _, _, _, _ -> },
            onOpenDelayTest = {},
            onOpenDohTest = {},
            onDismissDelayTest = {},
            onDismissDohTest = {},
            onApplyProxy = { _, _, _ -> },
            onOpenApplyDeepLinks = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DelayTestDialogPreview() {
    ComponentPreview {
        DelayTestDialog(
            currentHost = "https://hanime1.me/",
            results = listOf(
                DelayResultUi("1.1.1.1", 82),
                DelayResultUi("8.8.8.8", 164),
                DelayResultUi("9.9.9.9", -1),
            ),
            onDismiss = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProxyDialogPreview() {
    ComponentPreview {
        ProxyDialog(
            initialType = 1,
            initialIp = "1.1.1.1",
            initialPort = 8080,
            onDismiss = { },
            onConfirm = { _, _, _ -> },
        )
    }
}
