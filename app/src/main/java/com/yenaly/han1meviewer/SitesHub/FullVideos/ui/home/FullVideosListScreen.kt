package com.yenaly.han1meviewer.SitesHub.FullVideos.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun FullVideosListScreen(
    title: String,
    path: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    viewModel: FullVideosListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sortKey by rememberSaveable { mutableStateOf(FullVideosConstants.ENTITY_SORTS.first().key) }
    val sort = FullVideosConstants.ENTITY_SORTS.first { it.key == sortKey }

    LaunchedEffect(path, sortKey) {
        viewModel.setSource(FullVideosSource.Path(path.trimEnd('/') + sort.path))
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
                FullVideosVideoGrid(
                    header = {
                        SiteChipRow(
                            options = FullVideosConstants.ENTITY_SORTS,
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
