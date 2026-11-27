package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.yenaly.han1meviewer.ui.component.rememberLocalWatchTags
import com.yenaly.han1meviewer.ui.screen.library.offline.MediaGridCard
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.formatFileSizeV2
import com.yenaly.han1meviewer.util.offline.DownloadedQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class DownloadedRelatedState {
    var items by mutableStateOf<List<HanimeDownloadEntity>>(emptyList())
    var loaded by mutableStateOf(false)
    var reloadKey by mutableIntStateOf(0)
    var selecting by mutableStateOf(false)
    var selected by mutableStateOf(setOf<String>())
    var confirmDelete by mutableStateOf(false)

    fun exitSelection() {
        selecting = false
        selected = emptySet()
    }

    fun toggle(code: String) {
        selected = if (code in selected) selected - code else selected + code
        if (selected.isEmpty()) selecting = false
    }

    fun toggleAll() {
        selected = if (selected.size == items.size) emptySet() else items.map { it.videoCode }.toSet()
        if (selected.isEmpty()) selecting = false
    }
}

@Composable
fun rememberDownloadedRelatedState(currentCode: String): DownloadedRelatedState {
    val state = remember(currentCode) { DownloadedRelatedState() }
    LaunchedEffect(state, state.reloadKey) {
        val queue = DownloadedQueue.load(currentCode)
        state.items = DownloadedQueue.relatedWindow(queue, currentCode)
        state.loaded = true
    }
    return state
}

@Composable
fun DownloadedRelatedSection(
    state: DownloadedRelatedState,
    onOpen: (HanimeDownloadEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (state.selecting) {
                    stringResource(R.string.offl_selected_fmt, state.selected.size, state.items.size)
                } else {
                    stringResource(R.string.related_downloads)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (state.selecting) {
                IconButton(onClick = state::toggleAll) {
                    Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all))
                }
                IconButton(
                    enabled = state.selected.isNotEmpty(),
                    onClick = { state.confirmDelete = true },
                ) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
                IconButton(onClick = state::exitSelection) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
        }

        if (state.loaded && state.items.isEmpty()) {
            Text(
                text = stringResource(R.string.here_is_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val spacing = 8.dp
                val columns = ((maxWidth + spacing) / (150.dp + spacing)).toInt().coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    state.items.chunked(columns).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            rowItems.forEach { video ->
                                key(video.videoCode) {
                                    MediaGridCard(
                                        thumb = video.coverUri ?: video.coverUrl.takeIf { it.isNotBlank() },
                                        videoUri = video.videoUri,
                                        title = video.title,
                                        subtitle = "${video.quality} · ${video.length.formatFileSizeV2()}",
                                        selecting = state.selecting,
                                        selected = video.videoCode in state.selected,
                                        aspect = 1.45f,
                                        tags = rememberLocalWatchTags(video.videoCode),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (state.selecting) state.toggle(video.videoCode) else onOpen(video)
                                        },
                                        onLongClick = {
                                            if (!state.selecting) {
                                                state.selecting = true
                                                state.selected = setOf(video.videoCode)
                                            } else {
                                                state.toggle(video.videoCode)
                                            }
                                        },
                                    )
                                }
                            }
                            repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadedRelatedDeleteDialog(
    state: DownloadedRelatedState,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ConfirmDialog(
        visible = state.confirmDelete,
        title = stringResource(R.string.delete),
        message = stringResource(R.string.confirm_delete_videos, state.selected.size),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            val victims = state.items.filter { it.videoCode in state.selected }
            state.confirmDelete = false
            state.exitSelection()
            scope.launch {
                withContext(Dispatchers.IO) {
                    victims.forEach { video ->
                        runCatching {
                            SafFileManager.deleteVideoFolderAndForget(context, video.videoCode)
                            DatabaseRepo.HanimeDownload.delete(video.videoCode)
                        }
                    }
                }
                state.reloadKey++
                onChanged()
            }
        },
        onDismiss = { state.confirmDelete = false },
    )
}
