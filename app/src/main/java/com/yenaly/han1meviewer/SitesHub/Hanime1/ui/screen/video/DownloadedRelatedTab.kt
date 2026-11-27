package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.component.rememberLocalWatchTags
import com.yenaly.han1meviewer.ui.screen.library.offline.MediaGridCard
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.formatFileSizeV2
import com.yenaly.han1meviewer.util.offline.DownloadedQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DownloadedRelatedTab(
    currentCode: String,
    onOpen: (HanimeDownloadEntity) -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<HanimeDownloadEntity>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(currentCode, reloadKey) {
        val queue = DownloadedQueue.load(currentCode)
        items = DownloadedQueue.relatedWindow(queue, currentCode)
        loaded = true
    }

    fun exitSelection() {
        selecting = false
        selected = emptySet()
    }

    fun toggle(code: String) {
        selected = if (code in selected) selected - code else selected + code
        if (selected.isEmpty()) selecting = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selecting) {
                    stringResource(R.string.offl_selected_fmt, selected.size, items.size)
                } else {
                    stringResource(R.string.related_downloads)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (selecting) {
                IconButton(onClick = {
                    selected = if (selected.size == items.size) emptySet() else items.map { it.videoCode }.toSet()
                    if (selected.isEmpty()) selecting = false
                }) { Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all)) }
                IconButton(
                    enabled = selected.isNotEmpty(),
                    onClick = { confirmDelete = true },
                ) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
                IconButton(onClick = ::exitSelection) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
        }

        if (loaded && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyContent(hint = stringResource(R.string.here_is_empty))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.videoCode }) { video ->
                    MediaGridCard(
                        thumb = video.coverUri ?: video.coverUrl.takeIf { it.isNotBlank() },
                        title = video.title,
                        subtitle = "${video.quality} · ${video.length.formatFileSizeV2()}",
                        selecting = selecting,
                        selected = video.videoCode in selected,
                        aspect = 1.45f,
                        tags = rememberLocalWatchTags(video.videoCode),
                        onClick = {
                            if (selecting) toggle(video.videoCode) else onOpen(video)
                        },
                        onLongClick = {
                            if (!selecting) {
                                selecting = true
                                selected = setOf(video.videoCode)
                            } else {
                                toggle(video.videoCode)
                            }
                        },
                    )
                }
            }
        }
    }

    ConfirmDialog(
        visible = confirmDelete,
        title = stringResource(R.string.delete),
        message = stringResource(R.string.confirm_delete_videos, selected.size),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            val victims = items.filter { it.videoCode in selected }
            confirmDelete = false
            exitSelection()
            scope.launch {
                withContext(Dispatchers.IO) {
                    victims.forEach { video ->
                        runCatching {
                            SafFileManager.deleteVideoFolderAndForget(context, video.videoCode)
                            DatabaseRepo.HanimeDownload.delete(video.videoCode, video.quality)
                        }
                    }
                }
                reloadKey++
                onChanged()
            }
        },
        onDismiss = { confirmDelete = false },
    )
}
