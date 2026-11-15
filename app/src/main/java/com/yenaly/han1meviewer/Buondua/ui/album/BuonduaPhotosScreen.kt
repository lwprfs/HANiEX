package com.yenaly.han1meviewer.Buondua.ui.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.Buondua.common.BuonduaDownloads
import com.yenaly.han1meviewer.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Photos only: back + "download all" at the top, a grid below that keeps loading album pages as
 * you scroll. Tapping a photo opens the full-screen pager; going back lands here again.
 */
@Composable
fun BuonduaPhotosScreen(
    code: String,
    onBack: () -> Unit,
    viewModel: BuonduaAlbumViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = BuonduaImageLoader.get(context)
    val gridState = rememberLazyGridState()
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(code) { viewModel.load(code, photosOnly = true) }

    val nearEnd by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 8
        }
    }
    LaunchedEffect(gridState, state.photos.size) {
        snapshotFlow { nearEnd }.distinctUntilChanged().filter { it }
            .collect { viewModel.loadNextPage() }
    }

    viewerIndex?.let { start ->
        BuonduaPhotoLightbox(
            photos = state.photos,
            startIndex = start,
            title = state.title,
            onNearEnd = { viewModel.loadNextPage() },
            onDismiss = { viewerIndex = null },
        )
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = {
                    val count = state.photoCount ?: state.photos.size
                    Text(
                        text = "${stringResource(R.string.buondua_section_photos)} ($count)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    IconButton(
                        enabled = state.photos.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                BuonduaDownloads.downloadAllPhotos(
                                    context = context,
                                    code = state.code,
                                    title = state.title,
                                    loadedUrls = state.photos.map { it.url },
                                    totalPages = state.totalPages,
                                )
                            }
                        },
                    ) {
                        Icon(Icons.Default.Download, stringResource(R.string.xhamster_download_all))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> SiteLoadingContent()
                state.error != null -> SiteErrorContent(
                    message = state.error.orEmpty(),
                    onRetry = { viewModel.retry() },
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp, top = 8.dp, end = 8.dp,
                        bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(state.photos, key = { _, p -> "photo_" + p.url }) { index, photo ->
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            imageLoader = imageLoader,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewerIndex = index },
                        )
                    }
                    item(key = "pager_footer", span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator()
                            } else {
                                Text(
                                    text = "${state.loadedPages} / ${state.totalPages}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
