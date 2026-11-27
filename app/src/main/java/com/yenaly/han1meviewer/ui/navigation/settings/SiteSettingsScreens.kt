package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.navigation.Navigator

@Composable
fun SiteSettingsRowsScreen(
    rows: List<SiteSettingsRow>,
    navigator: Navigator,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    ) {
        items(rows, key = { it.key }) { row ->
            SettingNavigationItem(
                title = stringResource(row.titleRes),
                summary = row.summaryRes?.let { stringResource(it) },
                icon = row.icon,
                onClick = { navigator.navigateSafely(row.route) },
            )
        }
    }
}

@Composable
fun SettingsRedirect(navigator: Navigator) {
    LaunchedEffect(Unit) {
        if (!navigator.popTo(HomeSettingsRoute)) {
            navigator.replaceTop(HomeSettingsRoute)
        }
    }
}
