package com.yenaly.han1meviewer.Coser.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yenaly.han1meviewer.Coser.common.CoserConstants
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold

@Composable
fun CoserListScreen(
    path: String,
    title: String,
    collectTerms: Boolean,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CoserTerm) -> Unit,
    viewModel: CoserListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filters by rememberSaveable(stateSaver = CoserFiltersSaver) { mutableStateOf(CoserConstants.Filters()) }
    LaunchedEffect(path, collectTerms, filters) {
        viewModel.setSource(CoserSource.Page(path, collectTerms, filters))
    }
    val gridState = rememberLazyGridState()
    val header: (@Composable () -> Unit)? =
        if (collectTerms) null else ({ CoserFilterHeader(filters = filters, onChange = { filters = it }) })
    HanimeScaffold(title = title, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                CoserCardGrid(
                    header = header,
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
