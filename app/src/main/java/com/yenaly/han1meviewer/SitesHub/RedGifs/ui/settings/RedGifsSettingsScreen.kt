package com.yenaly.han1meviewer.SitesHub.RedGifs.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsConstants
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsSession
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsSettings
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.ui.component.SettingChoiceDialogItem
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingRadioItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

@Composable
fun RedGifsSettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val epoch by RedGifsSession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { RedGifsAuth.isLoggedIn }
    val saved = stringResource(R.string.redgifs_prefs_saved)
    val failed = stringResource(R.string.site_shorts_action_failed)

    var quality by remember { mutableStateOf(RedGifsSettings.quality) }
    var verifiedOnly by remember { mutableStateOf(RedGifsSettings.verifiedOnly) }
    var orientations by remember { mutableStateOf<Set<String>>(emptySet()) }
    var blocked by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(epoch, loggedIn) {
        if (loggedIn) {
            RedGifsRepo.me().onSuccess {
                orientations = it.preferences.map { p -> p.lowercase() }.toSet()
                blocked = it.blockedTags.joinToString(", ")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.redgifs_settings_title)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingChoiceDialogItem(
                title = stringResource(R.string.redgifs_pref_quality),
                dialogMessage = stringResource(R.string.redgifs_pref_quality_summary),
                options = listOf(
                    stringResource(R.string.redgifs_quality_hd) to RedGifsSettings.QUALITY_HD,
                    stringResource(R.string.redgifs_quality_sd) to RedGifsSettings.QUALITY_SD,
                ),
                selectedValue = quality,
                onSelect = {
                    quality = it
                    RedGifsSettings.quality = it
                },
            )
            SettingSwitchItem(
                title = stringResource(R.string.redgifs_pref_verified_only),
                summary = stringResource(R.string.redgifs_pref_verified_only_summary),
                checked = verifiedOnly,
                onCheckedChange = {
                    verifiedOnly = it
                    RedGifsSettings.verifiedOnly = it
                },
            )

            SettingSectionHeader(stringResource(R.string.redgifs_pref_content))
            if (!loggedIn) {
                SettingHintText(stringResource(R.string.redgifs_pref_content_login))
            } else {
                SettingHintText(stringResource(R.string.redgifs_pref_content_summary))
                RedGifsConstants.ORIENTATIONS.forEach { o ->
                    SettingSwitchItem(
                        title = o.replaceFirstChar { it.uppercase() },
                        checked = o in orientations,
                        onCheckedChange = { on -> orientations = if (on) orientations + o else orientations - o },
                    )
                }
                OutlinedTextField(
                    value = blocked,
                    onValueChange = { blocked = it },
                    label = { Text(stringResource(R.string.redgifs_pref_blocked_tags)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        scope.launch {
                            val tags = blocked.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                            RedGifsRepo.updatePreferences(orientations.toList(), tags)
                                .onSuccess { GlobalToasts.show(saved, level = GlobalToasts.ToastLevel.SUCCESS) }
                                .onFailure {
                                    GlobalToasts.show(it.message ?: failed, level = GlobalToasts.ToastLevel.ERROR)
                                }
                            saving = false
                        }
                    },
                    modifier = Modifier.padding(16.dp),
                ) { Text(stringResource(R.string.redgifs_save)) }
            }
        }
    }
}
