package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.network.DohConfig
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.SiteNetworkPrefs
import com.yenaly.han1meviewer.logic.network.SiteNetworks
import com.yenaly.han1meviewer.ui.screen.settings.NetworkSettingsUiState

internal fun buildNetworkSettingsUiState(context: Context, site: SiteType): NetworkSettingsUiState {
    val profile = SiteNetworks.forSite(site)
    val selected = SiteNetworkPrefs.selectedMirrorUrl(site)
    val inheritedSuffix = " " + context.getString(R.string.network_inherited_suffix)
    val proxyInherited = site != SiteType.HANIME && SiteNetworkPrefs.isProxyInherited(site)
    val proxyText = when (SiteNetworkPrefs.proxyType(site)) {
        HProxySelector.TYPE_DIRECT -> context.getString(R.string.direct)
        HProxySelector.TYPE_SYSTEM -> context.getString(R.string.system_proxy)
        HProxySelector.TYPE_HTTP -> context.getString(
            R.string.http_proxy,
            SiteNetworkPrefs.proxyIp(site),
            SiteNetworkPrefs.proxyPort(site)
        )

        HProxySelector.TYPE_SOCKS -> context.getString(
            R.string.socks_proxy,
            SiteNetworkPrefs.proxyIp(site),
            SiteNetworkPrefs.proxyPort(site)
        )

        else -> context.getString(R.string.direct)
    }
    return NetworkSettingsUiState(
        domainName = selected,
        domainDisplay = profile.mirrors.firstOrNull { it.baseUrl == selected }?.host ?: selected,
        proxySummary = if (proxyInherited) proxyText + inheritedSuffix else proxyText,
        useBuiltInHosts = SiteNetworkPrefs.useBuiltInHosts(site),
        useBackupMediaCdn = Preferences.useBackupMediaCdn,
        useCustomMirrorSite = SiteNetworkPrefs.useCustomMirror(site),
        customMirrorSite = SiteNetworkPrefs.customMirror(site),
        appendCustomMirrorPath = SiteNetworkPrefs.appendCustomPath(site),
        useDoH = SiteNetworkPrefs.useDoH(site),
        dohSummary = buildDohSummary(context, site),
        delaySummary = context.getString(R.string.node_latency_sum),
    )
}

internal fun buildDohSummary(context: Context, site: SiteType): String {
    if (!SiteNetworkPrefs.useDoH(site)) return context.getString(R.string.doh_disabled_summary)
    if (SiteNetworkPrefs.useBuiltInHosts(site)) return context.getString(R.string.doh_conflict_message)
    val core = if (SiteNetworkPrefs.dohPreset(site) == "custom") {
        SiteNetworkPrefs.dohCustomUrl(site).ifBlank { context.getString(R.string.custom) }
    } else {
        DohConfig.selectedPreset(site).title
    }
    val bootstrap = DohConfig.bootstrapIps(site).takeIf { it.isNotEmpty() }?.joinToString()
    val text = if (bootstrap != null) "$core\nBootstrap: $bootstrap" else core
    return if (site != SiteType.HANIME && SiteNetworkPrefs.isDohInherited(site)) {
        text + "\n" + context.getString(R.string.network_inherited_suffix)
    } else {
        text
    }
}
