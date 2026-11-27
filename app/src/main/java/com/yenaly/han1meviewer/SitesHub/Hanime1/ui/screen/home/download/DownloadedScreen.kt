package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.screen.library.offline.DetailLine
import com.yenaly.han1meviewer.ui.screen.library.offline.MediaGridCard
import com.yenaly.han1meviewer.util.formatFileSizeV2

private fun formatAdded(ms: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        .format(java.util.Date(ms))

@Composable
fun DownloadedScreen(
    uiState: DownloadUiState,
    listState: LazyGridState,
    onEvent: (DownloadEvent) -> Unit,
) {
    var pendingRename by remember { mutableStateOf<DownloadHeaderNode?>(null) }
    var popupVideo by remember { mutableStateOf<VideoWithCategories?>(null) }

    CreateGroupDialog(
        visible = uiState.showCreateGroupDialog,
        groups = uiState.displayGroups,
        onDismiss = { onEvent(DownloadEvent.OnCreateGroupDialogChange(false)) },
        onConfirm = {
            onEvent(DownloadEvent.OnCreateGroup(it))
            onEvent(DownloadEvent.OnCreateGroupDialogChange(false))
        },
        onDeleteGroup = { onEvent(DownloadEvent.OnDeleteGroup(it)) },
    )

    GroupRenameDialog(
        header = pendingRename,
        groups = uiState.displayGroups,
        onDismiss = { pendingRename = null },
        onConfirm = { header, newName ->
            uiState.displayGroups.find { it.id == header.groupId }?.let { group ->
                onEvent(DownloadEvent.OnRenameGroup(group.id, newName))
            }
            pendingRename = null
        },
        onDelete = { header ->
            uiState.displayGroups.find { it.id == header.groupId }
                ?.let { onEvent(DownloadEvent.OnDeleteGroup(it)) }
            pendingRename = null
        },
    )

    if (uiState.folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyContent(
                hint = stringResource(R.string.empty_content),
                subHint = stringResource(R.string.downloaded),
            )
        }
        return
    }

    if (uiState.downloadedNodes.isEmpty()) {
        // Folder list: every group (or site) is a card that opens its own page.
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !uiState.folderBySite,
                    onClick = { onEvent(DownloadEvent.OnFolderModeChange(false)) },
                    label = { Text(stringResource(R.string.download_folder_by_group)) },
                )
                FilterChip(
                    selected = uiState.folderBySite,
                    onClick = { onEvent(DownloadEvent.OnFolderModeChange(true)) },
                    label = { Text(stringResource(R.string.download_folder_by_site)) },
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.folders, key = { it.key }) { folder ->
                    val first = folder.videos.firstOrNull()?.video
                    DownloadFolderCard(
                        title = folder.title,
                        count = folder.videos.size,
                        cover = first?.coverUri ?: first?.coverUrl?.takeIf { it.isNotBlank() },
                        onClick = { onEvent(DownloadEvent.OnOpenFolder(folder.key)) },
                        onLongClick = folder.groupId
                            ?.takeIf { it != DownloadGroupEntity.DEFAULT_GROUP_ID }
                            ?.let { gid ->
                                {
                                    pendingRename = DownloadHeaderNode(
                                        groupId = gid,
                                        groupKey = folder.title,
                                        originalVideos = folder.videos,
                                    )
                                }
                            },
                    )
                }
            }
        }
        return
    }

    // Folder page (or search results): the videos themselves.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = uiState.downloadedNodes.filterIsInstance<DownloadItemNode>(),
            key = { "item-${it.parentKey}-${it.data.video.id}" },
        ) { node ->
            val video = node.data
            val id = video.video.id
            MediaGridCard(
                thumb = video.video.coverUri ?: video.video.coverUrl.takeIf { it.isNotBlank() },
                title = video.video.title,
                subtitle = "${video.video.quality} · ${video.video.length.formatFileSizeV2()}",
                selecting = uiState.multiSelectMode,
                selected = id in uiState.selectedVideoIds,
                aspect = 1.45f,
                onClick = {
                    if (uiState.multiSelectMode) onEvent(DownloadEvent.OnToggleVideoSelection(id))
                    else popupVideo = video
                },
                onDoubleClick = { onEvent(DownloadEvent.OnLocalPlayback(video)) },
                onLongClick = {
                    if (!uiState.multiSelectMode) {
                        onEvent(DownloadEvent.OnToggleMultiSelect)
                        onEvent(DownloadEvent.OnToggleVideoSelection(id))
                    } else {
                        onEvent(DownloadEvent.OnToggleVideoSelection(id))
                    }
                },
            )
        }
    }

    popupVideo?.let { video ->
        VideoDetailsPopup(
            video = video,
            groupName = uiState.displayGroups.firstOrNull { it.id == video.video.groupId }?.name.orEmpty(),
            onDismiss = { popupVideo = null },
            onLocal = { popupVideo = null; onEvent(DownloadEvent.OnLocalPlayback(video)) },
            onExternal = { popupVideo = null; onEvent(DownloadEvent.OnExternalPlayback(video)) },
            onDelete = { popupVideo = null; onEvent(DownloadEvent.OnDeleteDownloadedVideo(video)) },
        )
    }
}

@Composable
private fun VideoDetailsPopup(
    video: VideoWithCategories,
    groupName: String,
    onDismiss: () -> Unit,
    onLocal: () -> Unit,
    onExternal: () -> Unit,
    onDelete: () -> Unit,
) {
    val v = video.video
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val cover = v.coverUri ?: v.coverUrl.takeIf { it.isNotBlank() }
                if (cover != null) {
                    AsyncImage(
                        model = cover,
                        contentDescription = v.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)),
                    )
                }
                Text(v.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                DetailLine(stringResource(R.string.offl_detail_code), v.videoCode)
                DetailLine(stringResource(R.string.offl_detail_quality), v.quality)
                DetailLine(stringResource(R.string.offl_detail_size), v.length.formatFileSizeV2())
                DetailLine(stringResource(R.string.offl_detail_added), formatAdded(v.addDate))
                DetailLine(stringResource(R.string.offl_detail_group), groupName)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = onLocal,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.local_playback), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                    FilledTonalButton(
                        onClick = onExternal,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_ext_link), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.ext_player), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_baseline_delete_24), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.delete), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
