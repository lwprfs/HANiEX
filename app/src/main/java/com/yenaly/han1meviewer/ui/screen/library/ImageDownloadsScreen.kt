package com.yenaly.han1meviewer.ui.screen.library

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.util.FapFolderImageLocation
import com.yenaly.han1meviewer.util.ImageDownloadTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Sites whose posts / albums can be saved as images. */
val IMAGE_DOWNLOAD_SITES = setOf(SiteType.FAPFOLDER, SiteType.BUONDUA, SiteType.XHAMSTER, SiteType.COSPLAYTELE)

private data class PhotoEntry(
    val docId: String,
    val name: String,
    val isDir: Boolean,
    val uri: Uri,
    val modified: Long,
)

private data class FolderCrumb(val docId: String, val name: String)

private fun listChildren(context: Context, tree: Uri, parentDocId: String): List<PhotoEntry> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocId)
    val out = ArrayList<PhotoEntry>()
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )
    runCatching {
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1).orEmpty()
                val mime = c.getString(2).orEmpty()
                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                if (!isDir && !mime.startsWith("image/")) continue
                out += PhotoEntry(
                    docId = id,
                    name = name,
                    isDir = isDir,
                    uri = DocumentsContract.buildDocumentUriUsingTree(tree, id),
                    modified = c.getLong(3),
                )
            }
        }
    }
    return out
}

private fun listAllImages(
    context: Context,
    tree: Uri,
    rootDocId: String,
    cap: Int = 4000,
): List<PhotoEntry> {
    val result = ArrayList<PhotoEntry>()
    val queue = ArrayDeque<String>()
    queue.add(rootDocId)
    while (queue.isNotEmpty() && result.size < cap) {
        val dir = queue.removeFirst()
        for (e in listChildren(context, tree, dir)) {
            if (e.isDir) queue.add(e.docId) else result += e
            if (result.size >= cap) break
        }
    }
    return result.sortedByDescending { it.modified }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageDownloadsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var showAll by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    val crumbs = remember { mutableStateListOf<FolderCrumb>() }
    val selected = remember { mutableStateListOf<String>() }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PhotoEntry?>(null) }
    val selecting = selected.isNotEmpty()
    val jobs by ImageDownloadTracker.jobs.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            FapFolderImageLocation.persist(context, tree)
            FapFolderImageLocation.confirmedThisSession = true
            crumbs.clear()
            refresh++
        }
    }

    val ready = refresh >= 0 && FapFolderImageLocation.isReady(context)
    val treeUri = remember(refresh) {
        FapFolderImageLocation.uri?.let { runCatching { it.toUri() }.getOrNull() }
    }
    val rootDocId = remember(treeUri) {
        treeUri?.let { runCatching { DocumentsContract.getTreeDocumentId(it) }.getOrNull() }
    }
    val currentDocId = crumbs.lastOrNull()?.docId ?: rootDocId

    var loading by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<PhotoEntry>>(emptyList()) }

    val activeCount = jobs.size
    LaunchedEffect(ready, treeUri, currentDocId, showAll, refresh, activeCount) {
        if (!ready || treeUri == null || currentDocId == null) {
            entries = emptyList()
            return@LaunchedEffect
        }
        loading = true
        entries = withContext(Dispatchers.IO) {
            if (showAll) listAllImages(context, treeUri, currentDocId)
            else listChildren(context, treeUri, currentDocId)
                .sortedWith(compareByDescending<PhotoEntry> { it.isDir }.thenBy { it.name.lowercase() })
        }
        loading = false
    }

    val photos = remember(entries) { entries.filter { !it.isDir } }
    val dirs = remember(entries) { entries.filter { it.isDir } }

    LaunchedEffect(entries) {
        val valid = entries.mapTo(HashSet()) { it.docId }
        selected.removeAll { it !in valid }
    }
    LaunchedEffect(currentDocId, showAll) { selected.clear() }

    BackHandler(enabled = selecting || viewerIndex != null || crumbs.isNotEmpty()) {
        when {
            selecting -> selected.clear()
            viewerIndex != null -> viewerIndex = null
            crumbs.isNotEmpty() -> crumbs.removeAt(crumbs.lastIndex)
        }
    }

    HanimeScaffold(
        title = if (selecting) stringResource(R.string.fap_selected, selected.size)
        else crumbs.lastOrNull()?.name ?: stringResource(R.string.image_dl_title),
        onBack = {
            when {
                selecting -> selected.clear()
                crumbs.isNotEmpty() -> crumbs.removeAt(crumbs.lastIndex)
                else -> onBack()
            }
        },
        actions = {
            if (selecting) {
                FilledIconButton(onClick = {
                    selected.clear()
                    selected.addAll(photos.map { it.docId })
                }) {
                    Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.fap_select_all))
                }
                FilledIconButton(onClick = { confirmBatchDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.fap_delete_selected))
                }
            } else {
                FilledIconButton(onClick = { picker.launch(null) }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.fap_change_folder))
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (crumbs.isEmpty()) {
                item(key = "location", span = { GridItemSpan(maxLineSpan) }) {
                    LocationCard(
                        ready = ready,
                        hasSaved = FapFolderImageLocation.uri != null,
                        name = if (ready) FapFolderImageLocation.displayName(context) else null,
                        onPick = { picker.launch(null) },
                        onForget = {
                            FapFolderImageLocation.uri = null
                            FapFolderImageLocation.confirmedThisSession = false
                            crumbs.clear()
                            refresh++
                        },
                    )
                }
            }
            if (jobs.isNotEmpty()) {
                item(key = "jobs_header", span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(stringResource(R.string.image_dl_active))
                }
                items(jobs, key = { "job_${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.photos, job.label),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${job.done}/${job.total}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { job.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            if (ready) {
                item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = !showAll,
                            onClick = { showAll = false },
                            label = { Text(stringResource(R.string.fap_folders)) },
                        )
                        FilterChip(
                            selected = showAll,
                            onClick = { showAll = true },
                            label = { Text(stringResource(R.string.fap_all_photos)) },
                        )
                    }
                }
                if (!showAll) {
                    items(dirs, key = { "d:" + it.docId }, span = { GridItemSpan(maxLineSpan) }) { dir ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { crumbs.add(FolderCrumb(dir.docId, dir.name)) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    dir.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                items(photos, key = { "p:" + it.docId }) { photo ->
                    val isSelected = photo.docId in selected
                    val toggle = {
                        if (isSelected) selected.remove(photo.docId) else selected.add(photo.docId)
                        Unit
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .combinedClickable(
                                onClick = {
                                    if (selecting) toggle() else viewerIndex = photos.indexOf(photo)
                                },
                                onLongClick = { toggle() },
                            ),
                    ) {
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = photo.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (isSelected) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            )
                        }
                        if (selecting) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Black.copy(alpha = 0.45f)
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                if (loading) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(32.dp))
                        }
                    }
                } else if (entries.isEmpty() && jobs.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(320.dp)) {
                            EmptyContent(stringResource(R.string.fap_no_downloaded_photos_here_yet))
                        }
                    }
                }
            }
        }
    }

    if (confirmBatchDelete && selecting) {
        val count = selected.size
        AlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { Text(stringResource(R.string.fap_delete_photo_s, count)) },
            text = { Text(stringResource(R.string.fap_the_selected_photos_will_be_removed)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmBatchDelete = false
                    val targets = photos.filter { it.docId in selected }
                    scope.launch {
                        val deleted = withContext(Dispatchers.IO) {
                            targets.count { t ->
                                runCatching {
                                    DocumentsContract.deleteDocument(context.contentResolver, t.uri)
                                }.getOrDefault(false)
                            }
                        }
                        selected.clear()
                        refresh++
                        val failed = targets.size - deleted
                        Toast.makeText(
                            context,
                            if (failed == 0) context.getString(R.string.fap_deleted_photo_s, deleted)
                            else context.getString(R.string.fap_deleted_failed, deleted, failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBatchDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.fap_delete_photo)) },
            text = { Text(stringResource(R.string.fap_will_be_removed_from_your_device, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                DocumentsContract.deleteDocument(context.contentResolver, target.uri)
                            }.getOrDefault(false)
                        }
                        if (ok) {
                            viewerIndex = null
                            refresh++
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.fap_couldn_t_delete_this_photo),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    val startIndex = viewerIndex
    if (startIndex != null && photos.isNotEmpty()) {
        Dialog(
            onDismissRequest = { viewerIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val pagerState = rememberPagerState(
                initialPage = startIndex.coerceIn(0, photos.lastIndex),
                pageCount = { photos.size },
            )
            var zoomed by remember { mutableStateOf(false) }
            var chromeVisible by remember { mutableStateOf(true) }
            val current = photos.getOrNull(pagerState.currentPage)

            Box(Modifier.fillMaxSize().background(Color.Black)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !zoomed,
                ) { page ->
                    ZoomableViewerImage(
                        uri = photos[page].uri,
                        name = photos[page].name,
                        onZoomChanged = { zoomed = it },
                        onTap = { chromeVisible = !chromeVisible },
                    )
                }
                if (chromeVisible) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { viewerIndex = null }) {
                            Icon(Icons.Default.Close, stringResource(R.string.close), tint = Color.White)
                        }
                        Text(
                            text = "${pagerState.currentPage + 1} / ${photos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            current?.let { photo ->
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, photo.uri)
                                    clipData = ClipData.newRawUri("", photo.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching {
                                    context.startActivity(
                                        Intent.createChooser(send, context.getString(R.string.fap_share_photo))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, stringResource(R.string.share), tint = Color.White)
                        }
                        IconButton(onClick = { deleteTarget = current }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun LocationCard(
    ready: Boolean,
    hasSaved: Boolean,
    name: String?,
    onPick: () -> Unit,
    onForget: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.image_dl_location),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = when {
                            ready -> name.orEmpty()
                            hasSaved -> stringResource(R.string.fap_access_to_the_saved_folder_was)
                            else -> stringResource(R.string.fap_no_folder_chosen_yet_you_will)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onPick) {
                    Text(stringResource(if (ready) R.string.fap_change_folder else R.string.fap_choose_folder))
                }
                if (hasSaved) {
                    TextButton(onClick = onForget) { Text(stringResource(R.string.fap_forget_folder)) }
                }
            }
        }
    }
}

@Composable
private fun ZoomableViewerImage(
    uri: Uri,
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
