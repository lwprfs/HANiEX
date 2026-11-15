package com.yenaly.han1meviewer.Buondua.ui.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.Buondua.data.remote.BuonduaNetwork
import com.yenaly.han1meviewer.Buondua.data.remote.BuonduaRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.SiteVideoActionBar
import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.SiteImageSaver
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * An album page = two clearly separated sections:
 *  1. **Videos** (only when the album has some) – each with its published title, a copyable
 *     link and a downloader button; an inline player only appears for files that stream.
 *  2. **Photos** – full-width images, loaded page by page, tap for the full-screen viewer.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun BuonduaAlbumScreen(
    code: String,
    onBack: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToTag: (url: String, title: String) -> Unit,
    viewModel: BuonduaAlbumViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val inline by viewModel.inlinePlayable.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = BuonduaImageLoader.get(context)
    val listState: LazyListState = rememberLazyListState()
    var lightboxIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(code) { viewModel.load(code) }

    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(listState, state.photos.size) {
        snapshotFlow { nearEnd }.distinctUntilChanged().filter { it }
            .collect { viewModel.loadNextPage() }
    }

    lightboxIndex?.let { start ->
        BuonduaPhotoLightbox(photos = state.photos, startIndex = start) { lightboxIndex = null }
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(state.title.ifBlank { stringResource(R.string.buondua_album) }, maxLines = 1) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
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
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            val counts = buildList {
                                state.photoCount?.let { add("$it photos") }
                                state.videoCount?.let { add("$it videos") }
                            }.joinToString(" + ")
                            if (counts.isNotBlank()) {
                                Text(
                                    text = counts,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    item(key = "actions") {
                        SiteVideoActionBar(
                            site = SiteType.BUONDUA,
                            videoCode = state.code,
                            title = state.title,
                            coverUrl = state.coverUrl,
                            webUrl = state.pageUrl,
                            onDownloadClick = {
                                scope.launch {
                                    GlobalToasts.show(
                                        context.getString(R.string.buondua_photos_download_started),
                                        level = GlobalToasts.ToastLevel.INFO,
                                    )
                                    val urls = BuonduaRepo.allPhotoUrls(
                                        state.code, state.photos.map { it.url }, state.totalPages,
                                    )
                                    SiteImageSaver.saveBatchInBackground(
                                        context = context,
                                        urls = urls,
                                        segments = listOf("Buondua", "Photos", DownloadLayout.sanitize(state.title)),
                                        baseName = DownloadLayout.sanitize(state.title, "photo"),
                                        client = BuonduaNetwork.streamClient,
                                        headers = BuonduaNetwork.mediaHeaders(),
                                    ) { result ->
                                        val msg = when (result) {
                                            is SiteImageSaver.SaveResult.Saved ->
                                                "${result.count} saved, ${result.skipped} skipped, ${result.failed} failed"
                                            is SiteImageSaver.SaveResult.Error -> result.message
                                        }
                                        GlobalToasts.show(
                                            msg,
                                            level = if (result is SiteImageSaver.SaveResult.Error)
                                                GlobalToasts.ToastLevel.ERROR else GlobalToasts.ToastLevel.SUCCESS,
                                        )
                                    }
                                }
                            },
                        )
                    }

                    if (state.tags.isNotEmpty()) {
                        item(key = "tags") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                state.tags.forEach { tag ->
                                    AssistChip(
                                        onClick = { onNavigateToTag(tag.url, tag.title) },
                                        label = { Text(tag.title) },
                                    )
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------- Section 1: videos
                    if (state.videos.isNotEmpty()) {
                        item(key = "videos_header") {
                            SectionTitle("${stringResource(R.string.buondua_section_videos)} (${state.videos.size})")
                        }
                        items(state.videos, key = { "video_" + it.url }) { video ->
                            BuonduaVideoItem(
                                albumCode = state.code,
                                coverUrl = state.coverUrl,
                                video = video,
                                inline = inline[video.url],
                            )
                        }
                    }

                    // ---------------------------------------------------- Section 2: photos
                    item(key = "photos_header") {
                        SectionTitle(
                            "${stringResource(R.string.buondua_section_photos)} " +
                                    "(${state.photoCount ?: state.photos.size})"
                        )
                    }
                    itemsIndexed(state.photos, key = { _, p -> "photo_" + p.url }) { index, photo ->
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            imageLoader = imageLoader,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .clickable { lightboxIndex = index },
                        )
                    }
                    item(key = "pager_footer") {
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

                    if (state.related.isNotEmpty()) {
                        item(key = "related_header") {
                            SectionTitle(stringResource(R.string.buondua_section_related))
                        }
                        items(state.related, key = { "related_" + it.videoCode }) { rel ->
                            Text(
                                text = rel.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToAlbum(rel.videoCode) }
                                    .padding(vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    androidx.compose.foundation.layout.Column {
        HorizontalDivider()
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
