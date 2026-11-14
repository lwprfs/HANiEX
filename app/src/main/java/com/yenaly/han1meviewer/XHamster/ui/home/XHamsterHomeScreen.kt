package com.yenaly.han1meviewer.XHamster.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteSearchTopBar
import com.yenaly.han1meviewer.XHamster.ui.discover.XHamsterDiscoverActions
import com.yenaly.han1meviewer.XHamster.ui.discover.XHamsterDiscoverStrips

@Composable
fun XHamsterHomeScreen(
    onNavigateToVideo: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToCategories: () -> Unit,
    discover: XHamsterDiscoverActions,
    modifier: Modifier = Modifier,
    viewModel: XHamsterListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable { mutableStateOf(XHamsterConstants.TABS.first().key) }
    val selected = XHamsterConstants.TABS.first { it.key == selectedKey }

    LaunchedEffect(selectedKey) {
        viewModel.setSource(XHamsterSource.Path(selected.path))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = com.yenaly.han1meviewer.SiteType.XHAMSTER,
                hint = stringResource(R.string.xhamster_search_hint),
                onSearchClick = { onNavigateToSearch(null) },
                action = {
                    SiteBarIconButton(
                        imageVector = Icons.Default.Category,
                        contentDescription = stringResource(R.string.xhamster_categories),
                        onClick = onNavigateToCategories,
                    )
                },
            )
        },
    ) { padding ->
        val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                XHamsterVideoGrid(
                    // Filter chips and discover strips scroll together with the videos.
                    header = {
                        Column {
                            SiteChipRow(
                                options = XHamsterConstants.TABS,
                                selected = selected,
                                label = { it.title },
                                onSelect = { selectedKey = it.key },
                                key = { it.key },
                                horizontalPadding = 0.dp,
                            )
                            XHamsterDiscoverStrips(discover)
                        }
                    },
                    state = state,
                    gridState = gridState,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenVideo = onNavigateToVideo,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
