package com.yenaly.han1meviewer.ui.screen.library.offline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.HistoryTag
import com.yenaly.han1meviewer.ui.component.HistoryTagRow
import com.yenaly.han1meviewer.ui.component.ThumbnailWithVideoFallback
import com.yenaly.han1meviewer.util.offline.OfflineFs
import com.yenaly.han1meviewer.util.offline.OfflineGroup
import com.yenaly.han1meviewer.util.offline.OfflineGroups
import com.yenaly.han1meviewer.util.offline.OfflineMigration

@Composable
fun OfflineMigrationGate(onMigrated: () -> Unit = {}) {
    val context = LocalContext.current
    var plan by remember { mutableStateOf<OfflineMigration.Plan?>(null) }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 0) }

    LaunchedEffect(Unit) {
        if (!OfflineMigration.isDone() && !OfflineMigration.postponedThisSession && OfflineFs.isReady(context)) {
            plan = OfflineMigration.detect(context).takeIf { it.needed }
        }
    }

    val current = plan
    if (current != null && !running) {
        AlertDialog(
            onDismissRequest = {
                OfflineMigration.postponedThisSession = true
                plan = null
            },
            title = { Text(stringResource(R.string.offl_migrate_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.offl_migrate_message,
                        current.videoFolders, current.photoAlbums, current.shortFolders,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    running = true
                    OfflineMigration.migrateAsync(
                        context,
                        onProgress = { d, t -> progress = d to t },
                        onDone = { report ->
                            running = false
                            plan = null
                            GlobalToasts.show(
                                if (report.failed == 0) {
                                    context.getString(R.string.offl_migrate_done, report.moved)
                                } else {
                                    context.getString(R.string.offl_migrate_partial, report.moved, report.failed)
                                },
                                level = if (report.failed == 0) GlobalToasts.ToastLevel.SUCCESS
                                else GlobalToasts.ToastLevel.WARNING,
                            )
                            onMigrated()
                        },
                    )
                }) { Text(stringResource(R.string.offl_migrate_action)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    OfflineMigration.postponedThisSession = true
                    plan = null
                }) { Text(stringResource(R.string.offl_migrate_later)) }
            },
        )
    }
    if (running) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.offl_migrate_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.offl_migrating))
                    LinearProgressIndicator(
                        progress = {
                            val (d, t) = progress
                            if (t > 0) d.toFloat() / t else 0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {},
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridCard(
    thumb: String?,
    title: String,
    subtitle: String,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    showPlayIcon: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
    aspect: Float = 0.75f,
    tags: List<HistoryTag> = emptyList(),
    videoUri: String? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    val hasThumb = thumb != null || !videoUri.isNullOrBlank()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onDoubleClick = if (selecting) null else onDoubleClick,
            ),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspect)) {
            if (hasThumb) {
                ThumbnailWithVideoFallback(
                    model = thumb,
                    videoUri = videoUri,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                )
            }
            if (showPlayIcon && hasThumb) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center).size(34.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                    .padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 6.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (badge != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (tags.isNotEmpty() && !selecting) {
                HistoryTagRow(
                    tags = tags,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(0.8f)
                        .padding(6.dp),
                )
            }
            if (selecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent),
                )
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(24.dp),
                )
            }
        }
    }
}

@Composable
fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp).fillMaxWidth(0.28f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun GroupNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.offl_group_name_hint)) },
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.offl_delete_group), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
fun MoveGroupPicker(
    groups: List<OfflineGroup>,
    defaultName: String,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    var picked by remember { mutableStateOf(OfflineGroups.DEFAULT_ID) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.offl_move_to_group)) },
        text = {
            Column {
                groups.forEach { g ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { picked = g.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = picked == g.id, onClick = { picked = g.id })
                        Text(if (g.id == OfflineGroups.DEFAULT_ID) defaultName else g.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(picked) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun BorderChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.Transparent,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
