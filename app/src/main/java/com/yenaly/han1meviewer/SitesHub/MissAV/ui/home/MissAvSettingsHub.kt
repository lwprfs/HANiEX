package com.yenaly.han1meviewer.SitesHub.MissAV.ui.home
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class SettingsHubEntry(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissAvSettingsHubScreen(
    onBack: () -> Unit,
    onNavigateToHomeCategories: () -> Unit,
    onNavigateToHost: () -> Unit,
) {
    val entries = listOf(
        SettingsHubEntry(
            key = "home_categories",
            title = stringResource(R.string.drawer_home_categories),
            subtitle = stringResource(R.string.site_home_categories_subtitle),
            icon = Icons.Default.Category,
            onClick = onNavigateToHomeCategories,
        ),
        SettingsHubEntry(
            key = "host",
            title = stringResource(R.string.drawer_host_mirror),
            subtitle = stringResource(R.string.missav_host_subtitle),
            icon = Icons.Default.Dns,
            onClick = onNavigateToHost,
        ),
    )

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.missav_missav_settings)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            items(entries, key = { it.key }) { entry ->
                SettingsHubRow(entry = entry)
            }
        }
    }
}

@Composable
private fun SettingsHubRow(entry: SettingsHubEntry) {
    SettingNavigationItem(
        title = entry.title,
        summary = entry.subtitle,
        icon = entry.icon,
        onClick = entry.onClick,
    )
}
