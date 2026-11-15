package com.yenaly.han1meviewer.ui.screen.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.screen.getColumnCount
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.local.LocalPlaylist
import com.yenaly.han1meviewer.logic.local.LocalPlaylistItem
import com.yenaly.han1meviewer.logic.local.LocalPlaylistStore
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.navigation.PendingVideoOpen
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LocalPlaylistsScreen(
    activity: MainActivity,
    onBack: () -> Unit,
) {
    val playlists by LocalPlaylistStore.playlists.collectAsStateWithLifecycle()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<LocalPlaylist?>(null) }
    var deleting by remember { mutableStateOf<LocalPlaylist?>(null) }
    var menuFor by remember { mutableStateOf<LocalPlaylist?>(null) }
    val open = playlists.firstOrNull { it.id == openId }

    HanimeScaffold(
        title = stringResource(R.string.local_playlists),
        onBack = onBack,
        actions = {
            FilledIconButton(onClick = { creating = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.local_playlist_create))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (playlists.isEmpty()) {
                EmptyContent(stringResource(R.string.local_playlist_empty))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(maxOf(2, getColumnCount(180))),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(playlists, key = { it.id }) { p ->
                        LocalPlaylistCard(
                            playlist = p,
                            modifier = Modifier.height(140.dp),
                            onClick = { openId = p.id },
                            onLongClick = { menuFor = p },
                        )
                    }
                }
            }
        }
    }

    if (open != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { openId = null },
            sheetState = sheetState,
            dragHandle = null,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    open.items.firstOrNull()?.let { first ->
                        RetryableImage(
                            model = first.coverUrl ?: "",
                            contentDescription = first.title,
                            placeholder = painterResource(R.drawable.h_chan_loading),
                            error = painterResource(R.drawable.h_chan_load_failed),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    )
                                )
                            ),
                    )
                    Text(
                        text = open.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.local_playlist_count, open.items.size),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                        )
                        Button(
                            onClick = { deleting = open },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_baseline_delete_24),
                                stringResource(R.string.delete),
                                tint = Color.White,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { renaming = open },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.7f)),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_edit_24),
                                stringResource(R.string.edit),
                                tint = Color.Red,
                            )
                        }
                    }
                }
                if (open.items.isEmpty()) {
                    EmptyContent(stringResource(R.string.local_playlist_empty_items))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(open.items, key = { it.key }) { item ->
                            LocalPlaylistVideoCard(
                                item = item,
                                onClick = {
                                    val site = item.siteType ?: return@LocalPlaylistVideoCard
                                    activity.requestCrossSiteOpen(
                                        PendingVideoOpen(
                                            site = site,
                                            videoCode = item.videoCode,
                                            title = item.title,
                                        ),
                                    )
                                },
                                onRemove = { LocalPlaylistStore.remove(open.id, item) },
                            )
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { p ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    TextButton(onClick = { renaming = p; menuFor = null }) {
                        Text(stringResource(R.string.local_playlist_rename))
                    }
                    TextButton(onClick = { deleting = p; menuFor = null }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { menuFor = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (creating) NameDialog(null, { creating = false }) { LocalPlaylistStore.create(it) }
    renaming?.let { p -> NameDialog(p.name, { renaming = null }) { LocalPlaylistStore.rename(p.id, it) } }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(p.name) },
            confirmButton = {
                TextButton(onClick = {
                    if (openId == p.id) openId = null
                    LocalPlaylistStore.delete(p.id)
                    deleting = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalPlaylistCard(
    playlist: LocalPlaylist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            Box(modifier = Modifier.height(100.dp)) {
                RetryableImage(
                    model = playlist.items.firstOrNull()?.coverUrl ?: "",
                    contentDescription = playlist.name,
                    placeholder = painterResource(R.drawable.h_chan_loading),
                    error = painterResource(R.drawable.h_chan_load_failed),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.video_count, playlist.items.size),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LocalPlaylistVideoCard(
    item: LocalPlaylistItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                RetryableImage(
                    model = item.coverUrl ?: "",
                    contentDescription = item.title,
                    placeholder = painterResource(R.drawable.h_chan_loading),
                    error = painterResource(R.drawable.h_chan_load_failed),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .padding(4.dp),
                    )
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.site,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun NameDialog(initial: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.local_playlist_create else R.string.local_playlist_rename)) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, singleLine = true,
                placeholder = { Text(stringResource(R.string.local_playlist_name)) },
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text); onDismiss() }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun LocalPlaylistPickerDialog(
    item: LocalPlaylistItem,
    onDismiss: () -> Unit,
) {
    val playlists by LocalPlaylistStore.playlists.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(LocalPlaylistStore.containing(item)) }
    var creating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.local_playlist_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (playlists.isEmpty()) Text(stringResource(R.string.local_playlist_empty))
                playlists.forEach { p ->
                    val checked = p.id in selected
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = if (checked) selected - p.id else selected + p.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { selected = if (checked) selected - p.id else selected + p.id })
                        Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                TextButton(onClick = { creating = true }) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.local_playlist_create))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                LocalPlaylistStore.setMembership(item, selected)
                android.widget.Toast.makeText(context, R.string.local_playlist_saved, android.widget.Toast.LENGTH_SHORT).show()
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (creating) NameDialog(null, { creating = false }) { name ->
        selected = selected + LocalPlaylistStore.create(name)
    }
}
