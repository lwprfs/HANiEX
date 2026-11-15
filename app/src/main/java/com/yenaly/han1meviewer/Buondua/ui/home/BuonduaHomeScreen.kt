package com.yenaly.han1meviewer.Buondua.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton

@Composable
fun BuonduaHomeScreen(
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BuonduaListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable { mutableStateOf(BuonduaConstants.CATEGORIES.first().key) }
    val selected = BuonduaConstants.CATEGORIES.first { it.key == selectedKey }

    LaunchedEffect(selectedKey) { viewModel.setSource(BuonduaSource.Category(selected.path)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = SiteType.BUONDUA,
                hint = stringResource(R.string.buondua_search_hint),
                onSearchClick = { onNavigateToSearch(null) },
                action = {
                    SiteBarIconButton(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.watch_history),
                        onClick = onNavigateToHistory,
                    )
                },
            )
        },
    ) { padding ->
        val gridState = rememberLazyGridState()
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                BuonduaAlbumGrid(
                    header = {
                        SiteChipRow(
                            options = BuonduaConstants.CATEGORIES,
                            selected = selected,
                            label = { it.title },
                            onSelect = { selectedKey = it.key },
                            key = { it.key },
                            horizontalPadding = 0.dp,
                        )
                    },
                    state = state,
                    gridState = gridState,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenAlbum = onNavigateToAlbum,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
