package com.yenaly.han1meviewer.Buondua.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

/** Albums of one tag (`/tag/<slug>-<id>`). Paging follows the tag page's own "next" link. */
@Composable
fun BuonduaTagScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    viewModel: BuonduaListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(url) { viewModel.setSource(BuonduaSource.Url(url)) }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                BuonduaAlbumGrid(
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenAlbum = onNavigateToAlbum,
                )
            }
        }
    }
}
