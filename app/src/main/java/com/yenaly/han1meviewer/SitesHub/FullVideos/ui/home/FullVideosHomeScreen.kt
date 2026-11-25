package com.yenaly.han1meviewer.SitesHub.FullVideos.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosSettings
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteHomeTopBar
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton

@Composable
fun FullVideosHomeScreen(
    onNavigateToVideo: (String) -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FullVideosListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sections = remember { FullVideosSettings.visibleSections() }
    var sectionKey by rememberSaveable { mutableStateOf(sections.first().key) }
    var filterKey by rememberSaveable { mutableStateOf("") }
    val section = sections.firstOrNull { it.key == sectionKey } ?: sections.first()
    val filters = FullVideosConstants.TIME_FILTERS[section.key].orEmpty()
    val filter = filters.firstOrNull { it.key == filterKey } ?: filters.firstOrNull()

    LaunchedEffect(section.key, filter?.key) {
        viewModel.setSource(FullVideosSource.Path(section.path + filter?.path.orEmpty()))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteHomeTopBar(
                site = com.yenaly.han1meviewer.SiteType.FULLVIDEOS,
                hint = stringResource(R.string.fullvideos_search_hint),
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
                FullVideosVideoGrid(
                    header = {
                        Column {
                            if (sections.size > 1) {
                                SiteChipRow(
                                    options = sections,
                                    selected = section,
                                    label = { it.title },
                                    onSelect = {
                                        sectionKey = it.key
                                        filterKey = ""
                                    },
                                    key = { it.key },
                                    horizontalPadding = 0.dp,
                                )
                            }
                            if (filters.isNotEmpty() && filter != null) {
                                SiteChipRow(
                                    options = filters,
                                    selected = filter,
                                    label = { it.title },
                                    onSelect = { filterKey = it.key },
                                    key = { it.key },
                                    horizontalPadding = 0.dp,
                                )
                            }
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

@Composable
internal fun browseLabel(kind: FullVideosConstants.EntityKind): String = stringResource(
    when (kind) {
        FullVideosConstants.EntityKind.CATEGORIES -> R.string.fullvideos_categories
        FullVideosConstants.EntityKind.MODELS -> R.string.fullvideos_models
        FullVideosConstants.EntityKind.SITES -> R.string.fullvideos_sites
    }
)
