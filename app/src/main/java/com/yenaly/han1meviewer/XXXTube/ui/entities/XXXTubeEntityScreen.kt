package com.yenaly.han1meviewer.XXXTube.ui.entities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeEntity
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar

@Composable
fun XXXTubeEntityScreen(
    kind: XXXTubeConstants.EntityKind,
    onBack: () -> Unit,
    onOpen: (XXXTubeEntity) -> Unit,
    viewModel: XXXTubeEntityViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(kind) { viewModel.setSource(XXXTubeEntitySource(kind)) }

    val title = when (kind) {
        XXXTubeConstants.EntityKind.MODELS -> stringResource(R.string.xxxtube_models)
        XXXTubeConstants.EntityKind.CATEGORIES -> stringResource(R.string.xxxtube_categories)
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        val gridState = rememberLazyGridState()
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
                XXXTubeEntityGrid(
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMore() },
                    onOpen = onOpen,
                    gridState = gridState,
                )
            }
            SiteScrollToTopButton(gridState)
        }
    }
}
