package com.yenaly.han1meviewer.SitesHub.RedGifs.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsFeedSource
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.feed.RedGifsFeedContent
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar

private data class HomeTab(val key: String, val titleRes: Int, val source: RedGifsFeedSource)

private val HOME_TABS = listOf(
    HomeTab("trending", R.string.redgifs_tab_trending, RedGifsFeedSource.Trending),
    HomeTab("foryou", R.string.redgifs_tab_for_you, RedGifsFeedSource.ForYou),
    HomeTab("latest", R.string.redgifs_tab_new, RedGifsFeedSource.Latest),
    HomeTab("popular", R.string.redgifs_tab_popular, RedGifsFeedSource.Popular),
)

@Composable
fun RedGifsHomeScreen(
    onSearch: () -> Unit,
    onLogin: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedKey by rememberSaveable { mutableStateOf(HOME_TABS.first().key) }
    val selected = HOME_TABS.first { it.key == selectedKey }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = SiteType.REDGIFS,
                hint = stringResource(R.string.redgifs_search_hint),
                onSearchClick = onSearch,
                action = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.redgifs_settings_title))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.padding(horizontal = 12.dp)) {
                SiteChipRow(
                    options = HOME_TABS,
                    selected = selected,
                    label = { context.getString(it.titleRes) },
                    onSelect = { selectedKey = it.key },
                    key = { it.key },
                    horizontalPadding = 0.dp,
                )
            }
            key(selected.key) {
                RedGifsFeedContent(source = selected.source, onLogin = onLogin)
            }
        }
    }
}
