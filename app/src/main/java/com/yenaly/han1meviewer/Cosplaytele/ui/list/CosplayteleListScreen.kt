package com.yenaly.han1meviewer.Cosplaytele.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold

@Composable
fun CosplayteleListScreen(
    path: String,
    title: String,
    collectTerms: Boolean,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CosplayteleTerm) -> Unit,
    viewModel: CosplayteleListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(path, collectTerms) {
        viewModel.setSource(CosplayteleSource.Page(path, collectTerms))
    }
    val gridState = rememberLazyGridState()
    HanimeScaffold(title = title, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                CosplayteleCardGrid(
                    state = state,
                    gridState = gridState,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpenPost = onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
