package com.yenaly.han1meviewer.FullVideos.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.FullVideos.common.FullVideosImageLoader
import com.yenaly.han1meviewer.FullVideos.ui.home.EntityCard
import com.yenaly.han1meviewer.FullVideos.ui.home.FullVideosEntityViewModel
import com.yenaly.han1meviewer.FullVideos.ui.home.FullVideosListViewModel
import com.yenaly.han1meviewer.FullVideos.ui.home.FullVideosSource
import com.yenaly.han1meviewer.FullVideos.ui.home.FullVideosVideoGrid
import com.yenaly.han1meviewer.FullVideos.ui.home.browseLabel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private data class PageOption(
    val key: String,
    val section: FullVideosConstants.Section? = null,
    val kind: FullVideosConstants.EntityKind? = null,
)

private val PAGE_OPTIONS: List<PageOption> = listOf(
    PageOption("top-rated", section = FullVideosConstants.SECTIONS.first { it.key == "top-rated" }),
    PageOption("most-popular", section = FullVideosConstants.SECTIONS.first { it.key == "most-popular" }),
    PageOption("categories", kind = FullVideosConstants.EntityKind.CATEGORIES),
    PageOption("models", kind = FullVideosConstants.EntityKind.MODELS),
    PageOption("sites", kind = FullVideosConstants.EntityKind.SITES),
)

@Composable
fun FullVideosPagesScreen(
    onNavigateToVideo: (String) -> Unit,
    onOpenEntity: (title: String, path: String) -> Unit,
    modifier: Modifier = Modifier,
    listViewModel: FullVideosListViewModel = viewModel(),
    entityViewModel: FullVideosEntityViewModel = viewModel(),
) {
    val listState by listViewModel.state.collectAsStateWithLifecycle()
    val entityState by entityViewModel.state.collectAsStateWithLifecycle()
    val imageLoader = FullVideosImageLoader.get(LocalContext.current)

    var pageKey by rememberSaveable { mutableStateOf(PAGE_OPTIONS.first().key) }
    var filterKey by rememberSaveable { mutableStateOf("") }
    var sortKey by rememberSaveable { mutableStateOf("") }

    val page = PAGE_OPTIONS.firstOrNull { it.key == pageKey } ?: PAGE_OPTIONS.first()
    val filters = page.section?.let { FullVideosConstants.TIME_FILTERS[it.key] }.orEmpty()
    val filter = filters.firstOrNull { it.key == filterKey } ?: filters.firstOrNull()
    val sorts = page.kind?.let { FullVideosConstants.entitySorts(it) }.orEmpty()
    val sort = sorts.firstOrNull { it.key == sortKey } ?: sorts.firstOrNull()

    val labels = PAGE_OPTIONS.associate { option ->
        option.key to (option.section?.title ?: browseLabel(option.kind!!))
    }

    LaunchedEffect(page.key, filter?.key) {
        page.section?.let {
            listViewModel.setSource(FullVideosSource.Path(it.path + filter?.path.orEmpty()))
        }
    }
    LaunchedEffect(page.key, sort?.key) {
        page.kind?.let { entityViewModel.setSource(it, sort?.path.orEmpty()) }
    }

    val header: @Composable () -> Unit = {
        Column {
            SiteChipRow(
                options = PAGE_OPTIONS,
                selected = page,
                label = { labels[it.key].orEmpty() },
                onSelect = {
                    pageKey = it.key
                    filterKey = ""
                    sortKey = ""
                },
                key = { it.key },
                horizontalPadding = 0.dp,
            )
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
            if (sorts.isNotEmpty() && sort != null) {
                SiteChipRow(
                    options = sorts,
                    selected = sort,
                    label = { it.title },
                    onSelect = { sortKey = it.key },
                    key = { it.key },
                    horizontalPadding = 0.dp,
                )
            }
        }
    }

    HanimeScaffold(
        title = stringResource(R.string.hub_pages_title),
        onBack = null,
        modifier = modifier,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            if (page.section != null) {
                val gridState = rememberLazyGridState()
                SitePullToRefresh(
                    loading = listState.isLoading,
                    onRefresh = { listViewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    FullVideosVideoGrid(
                        header = header,
                        state = listState,
                        gridState = gridState,
                        onRetry = { listViewModel.refresh() },
                        onLoadMore = { listViewModel.loadMore() },
                        onOpenVideo = onNavigateToVideo,
                    )
                }
                SiteScrollToTopButton(gridState)
            } else {
                val gridState = rememberLazyGridState()
                SitePullToRefresh(
                    loading = entityState.isLoading,
                    onRefresh = { entityViewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (entityState.items.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp)) {
                                header()
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when {
                                    entityState.isLoading -> SiteLoadingContent()
                                    entityState.error != null -> SiteErrorContent(
                                        message = entityState.error,
                                        onRetry = { entityViewModel.refresh() },
                                    )

                                    else -> SiteEmptyContent(
                                        hint = stringResource(R.string.fullvideos_nothing_here),
                                    )
                                }
                            }
                        }
                    } else {
                        val nearEnd by remember(gridState) {
                            derivedStateOf {
                                val info = gridState.layoutInfo
                                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                                info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
                            }
                        }
                        LaunchedEffect(gridState, entityState.items.size) {
                            snapshotFlow { nearEnd }
                                .distinctUntilChanged()
                                .filter { it }
                                .collect { entityViewModel.loadMore() }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(150.dp),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) { header() }
                            items(entityState.items, key = { it.path }) { entity ->
                                EntityCard(
                                    entity = entity,
                                    imageLoader = imageLoader,
                                    onClick = { onOpenEntity(entity.name, entity.path) },
                                )
                            }
                            if (entityState.isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { CircularProgressIndicator() }
                                }
                            }
                        }
                    }
                }
                SiteScrollToTopButton(gridState)
            }
        }
    }
}
