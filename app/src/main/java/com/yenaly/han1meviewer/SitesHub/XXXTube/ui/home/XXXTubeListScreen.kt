package com.yenaly.han1meviewer.SitesHub.XXXTube.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun XXXTubeListScreen(
    title: String,
    path: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    viewModel: XXXTubeListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val basePath = remember(path) { path.substringBefore('?') }
    var sortKey by rememberSaveable(path) {
        mutableStateOf(
            XXXTubeConstants.ORDERS
                .firstOrNull { path.contains(it.path.removePrefix("?")) }?.key
                ?: XXXTubeConstants.ORDERS.first().key
        )
    }
    val sort = XXXTubeConstants.ORDERS.first { it.key == sortKey }

    LaunchedEffect(basePath, sortKey) {
        viewModel.setSource(XXXTubeListSource.Path(XXXTubeConstants.withQuery(basePath, sort.path)))
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                XXXTubeVideoGrid(
                    header = {
                        SiteChipRow(
                            options = XXXTubeConstants.ORDERS,
                            selected = sort,
                            label = { it.title },
                            onSelect = { sortKey = it.key },
                            key = { it.key },
                            horizontalPadding = 0.dp,
                        )
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
