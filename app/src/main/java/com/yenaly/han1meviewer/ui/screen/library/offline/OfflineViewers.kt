package com.yenaly.han1meviewer.ui.screen.library.offline

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.shorts.SiteShort
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsPlayer
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsSource
import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.formatFileSizeV2
import com.yenaly.han1meviewer.util.offline.OfflineItem

/** Plays downloaded clips with the same vertical-scroll player that plays shorts online. */
private class LocalShortsSource(override val site: SiteType) : SiteShortsSource

fun siteTypeForFolder(folder: String): SiteType =
    SiteType.entries.firstOrNull { DownloadLayout.siteFolderName(it).equals(folder, ignoreCase = true) }
        ?: SiteType.REDGIFS

@Composable
fun OfflineShortsViewer(
    clips: List<OfflineItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (clips.isEmpty()) {
        onDismiss()
        return
    }
    val start = initialIndex.coerceIn(0, clips.lastIndex)
    val source = remember(clips[start].site) { LocalShortsSource(siteTypeForFolder(clips[start].site)) }
    val shorts = remember(clips) {
        clips.map {
            SiteShort(
                id = it.key,
                title = it.title,
                pageUrl = "",
                videoUrl = it.mediaUris.first(),
                coverUrl = it.coverUri.orEmpty(),
                views = it.views,
                isGif = false,
                durationSeconds = it.durationSeconds,
            )
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            SiteShortsPlayer(
                source = source,
                items = shorts,
                initialIndex = start,
                onDismiss = onDismiss,
            )
        }
    }
}

/**
 * Full-screen photo viewer for one album: swipe left/right through the album's photos, pinch or
 * double tap to zoom, thumbnail strip, details and delete.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OfflinePhotoViewer(
    album: OfflineItem,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onDeletePhoto: (uri: String) -> Unit,
) {
    val context = LocalContext.current
    val uris = album.mediaUris
    if (uris.isEmpty()) {
        onDismiss()
        return
    }
    val pager = rememberPagerState(initialPage = initialIndex.coerceIn(0, uris.lastIndex), pageCount = { uris.size })
    val strip = rememberLazyListState()
    var zoomed by remember { mutableStateOf(false) }
    var chrome by remember { mutableStateOf(true) }
    var showInfo by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(pager.currentPage) {
        strip.animateScrollToItem((pager.currentPage - 2).coerceAtLeast(0))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pager,
                userScrollEnabled = !zoomed,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ZoomableViewerImage(
                    uri = uris[page],
                    name = album.mediaNames.getOrElse(page) { album.title },
                    onZoomChanged = { zoomed = it },
                    onTap = { chrome = !chrome },
                )
            }
            if (chrome) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            album.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                        )
                        Text(
                            stringResource(R.string.offl_photo_counter, pager.currentPage + 1, uris.size),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.offl_details), tint = Color.White)
                    }
                    IconButton(onClick = { onDeletePhoto(uris[pager.currentPage]) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.offl_delete_photo), tint = Color.White)
                    }
                }
                LazyRow(
                    state = strip,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .navigationBarsPadding()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                ) {
                    itemsIndexed(uris) { index, uri ->
                        val current = index == pager.currentPage
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 54.dp, height = 72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (current) 2.dp else 0.dp,
                                    color = if (current) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable {
                                    zoomed = false
                                    scope.launch { pager.animateScrollToPage(index) }
                                },
                        )
                    }
                }
            }
            if (showInfo) {
                Dialog(onDismissRequest = { showInfo = false }) {
                    Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(album.title, style = MaterialTheme.typography.titleLarge)
                            DetailLine(stringResource(R.string.offl_detail_site), album.site)
                            DetailLine(stringResource(R.string.offl_detail_channel), album.channel)
                            DetailLine(stringResource(R.string.offl_photos_title), stringResource(R.string.offl_photos_fmt, album.count))
                            DetailLine(stringResource(R.string.offl_detail_size), album.sizeBytes.formatFileSizeV2())
                            DetailLine(stringResource(R.string.offl_detail_source), album.sourceUrl)
                            DetailLine(
                                stringResource(R.string.offl_detail_tags),
                                album.tags.takeIf { it.isNotEmpty() }?.joinToString(", "),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableViewerImage(
    uri: String,
    name: String,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                        onZoomChanged(scale > 1f)
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multiTouch = event.changes.size > 1
                        if (multiTouch || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            val maxX = size.width * (scale - 1f) / 2f
                            val maxY = size.height * (scale - 1f) / 2f
                            offset = if (scale <= 1f) Offset.Zero else Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY),
                            )
                            onZoomChanged(scale > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        AsyncImage(
            model = uri,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}
