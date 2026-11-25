package com.yenaly.han1meviewer.SitesHub.XHamster.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterSettings
import com.yenaly.han1meviewer.ui.component.SettingChoiceDialogItem
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingRadioItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun XHamsterSettingsScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf(XHamsterSettings.preferredQuality) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.xhamster_settings_title)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val autoLabel = stringResource(R.string.xhamster_quality_auto)
            SettingChoiceDialogItem(
                title = stringResource(R.string.xhamster_pref_quality),
                dialogMessage = stringResource(R.string.xhamster_pref_quality_summary),
                options = XHamsterConstants.QUALITIES.map { (if (it == "auto") autoLabel else it) to it },
                selectedValue = selected,
                onSelect = {
                    selected = it
                    XHamsterSettings.preferredQuality = it
                },
            )
        }
    }
}
