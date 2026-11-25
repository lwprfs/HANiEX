package com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.browse

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.list.CosplayteleCardGrid
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.list.CosplayteleListViewModel
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.list.CosplayteleSource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold

@Composable
fun CosplayteleBrowseScreen(
    onNavigateToPost: (String) -> Unit,
    onNavigateToTerm: (CosplayteleTerm) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CosplayteleListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable { mutableStateOf(CosplayteleConstants.PAGES_SECTIONS.first().key) }
    val selected = CosplayteleConstants.PAGES_SECTIONS.firstOrNull { it.key == selectedKey }
        ?: CosplayteleConstants.PAGES_SECTIONS.first()

    LaunchedEffect(selected.key) {
        viewModel.setSource(CosplayteleSource.Page(selected.path, selected.collectTerms))
    }

    HanimeScaffold(
        title = stringResource(R.string.hub_pages_title),
        onBack = null,
        modifier = modifier,
    ) { padding ->
        val gridState = rememberLazyGridState()
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            SitePullToRefresh(
                loading = state.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                CosplayteleCardGrid(
                    header = {
                        SiteChipRow(
                            options = CosplayteleConstants.PAGES_SECTIONS,
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
                    onOpenPost = onNavigateToPost,
                    onOpenTerm = onNavigateToTerm,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
