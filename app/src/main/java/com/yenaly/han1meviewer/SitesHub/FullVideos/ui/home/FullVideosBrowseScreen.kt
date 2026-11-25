package com.yenaly.han1meviewer.SitesHub.FullVideos.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosImageLoader
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.model.FullVideosEntity
import com.yenaly.han1meviewer.R
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteChipRow
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SitePullToRefresh
import com.yenaly.han1meviewer.ui.component.SiteScrollToTopButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun FullVideosBrowseScreen(
    kindKey: String,
    onBack: () -> Unit,
    onOpenEntity: (title: String, path: String) -> Unit,
    viewModel: FullVideosEntityViewModel = viewModel(),
) {
    val kind = remember(kindKey) { FullVideosConstants.EntityKind.fromKey(kindKey) }
    val sorts = remember(kind) { FullVideosConstants.entitySorts(kind) }
    var sortKey by rememberSaveable { mutableStateOf(sorts.firstOrNull()?.key.orEmpty()) }
    val sort = sorts.firstOrNull { it.key == sortKey } ?: sorts.firstOrNull()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader = FullVideosImageLoader.get(LocalContext.current)

    LaunchedEffect(kind, sort?.key) {
        viewModel.setSource(kind, sort?.path.orEmpty())
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(browseLabel(kind), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (sorts.isNotEmpty() && sort != null) {
                SiteChipRow(
                    options = sorts,
                    selected = sort,
                    label = { it.title },
                    onSelect = { sortKey = it.key },
                    key = { it.key },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SitePullToRefresh(
                    loading = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        state.items.isEmpty() && state.isLoading -> SiteLoadingContent()
                        state.items.isEmpty() && state.error != null -> SiteErrorContent(
                            message = state.error,
                            onRetry = { viewModel.refresh() },
                        )

                        state.items.isEmpty() -> SiteEmptyContent(
                            hint = stringResource(R.string.fullvideos_nothing_here)
                        )

                        else -> {
                            val gridState = rememberLazyGridState()
                            val nearEnd by remember(gridState) {
                                derivedStateOf {
                                    val info = gridState.layoutInfo
                                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
                                }
                            }
                            LaunchedEffect(gridState, state.items.size) {
                                snapshotFlow { nearEnd }
                                    .distinctUntilChanged()
                                    .filter { it }
                                    .collect { viewModel.loadMore() }
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(state.items, key = { it.path }) { entity ->
                                    EntityCard(
                                        entity = entity,
                                        imageLoader = imageLoader,
                                        onClick = { onOpenEntity(entity.name, entity.path) },
                                    )
                                }
                                if (state.isLoadingMore) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) { CircularProgressIndicator() }
                                    }
                                }
                            }
                            SiteScrollToTopButton(gridState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EntityCard(
    entity: FullVideosEntity,
    imageLoader: coil3.ImageLoader,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            if (entity.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = entity.imageUrl,
                    contentDescription = entity.name,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = entity.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entity.subtitle.isNotBlank()) {
                    Text(
                        text = entity.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
