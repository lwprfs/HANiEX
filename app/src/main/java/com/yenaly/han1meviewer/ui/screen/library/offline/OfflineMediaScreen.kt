package com.yenaly.han1meviewer.ui.screen.library.offline

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.screen.home.download.DownloadSort
import com.yenaly.han1meviewer.util.compactCount
import com.yenaly.han1meviewer.util.formatFileSizeV2
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.util.offline.OfflineDownloads
import com.yenaly.han1meviewer.util.offline.OfflineFs
import com.yenaly.han1meviewer.util.offline.OfflineGroup
import com.yenaly.han1meviewer.util.offline.OfflineGroups
import com.yenaly.han1meviewer.util.offline.OfflineItem
import com.yenaly.han1meviewer.util.offline.OfflineKind
import com.yenaly.han1meviewer.util.offline.OfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun sortComparator(sort: DownloadSort): Comparator<OfflineItem> = when (sort) {
    DownloadSort.DATE_DESC -> compareByDescending { it.addedAt }
    DownloadSort.DATE_ASC -> compareBy { it.addedAt }
    DownloadSort.FIRST_LETTER_ASC -> compareBy<OfflineItem, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
    DownloadSort.FIRST_LETTER_DESC -> compareBy<OfflineItem, String>(String.CASE_INSENSITIVE_ORDER) { it.title }.reversed()
}

private fun formatDate(ms: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        .format(java.util.Date(ms))

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OfflineMediaScreen(kind: OfflineKind, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allTasks by OfflineDownloads.tasks.collectAsStateWithLifecycle()
    val tasks = remember(allTasks, kind) { allTasks.filter { it.request.kind == kind } }
    val revision by OfflineDownloads.revision.collectAsStateWithLifecycle()
    val groupsState by remember(kind) { OfflineGroups.state(kind) }.collectAsStateWithLifecycle()

    var items by remember { mutableStateOf<List<OfflineItem>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload, revision, kind) {
        items = OfflineRepository.scan(context, kind)
    }

    val pager = rememberPagerState(pageCount = { 2 })
    var defaultPageApplied by remember { mutableStateOf(false) }
    LaunchedEffect(tasks.isEmpty()) {
        if (!defaultPageApplied) {
            defaultPageApplied = true
            if (tasks.isEmpty() && pager.currentPage != 1) pager.scrollToPage(1)
        }
    }

    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(DownloadSort.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var selecting by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(setOf<Int>()) }

    var popup by remember { mutableStateOf<OfflineItem?>(null) }
    var photoViewer by remember { mutableStateOf<OfflineItem?>(null) }
    var shortsViewerIndex by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf<List<OfflineItem>?>(null) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var editGroup by remember { mutableStateOf<OfflineGroup?>(null) }
    var showMove by remember { mutableStateOf(false) }
    var cancelTask by remember { mutableStateOf<OfflineDownloads.Task?>(null) }

    val ungrouped = stringResource(R.string.ungrouped)

    val shown by remember(items, query, sort) {
        derivedStateOf {
            val q = query.trim()
            val filtered = if (q.isEmpty()) items else items.filter {
                it.title.contains(q, true) || it.site.contains(q, true) ||
                        it.channel.orEmpty().contains(q, true)
            }
            filtered.sortedWith(sortComparator(sort))
        }
    }

    val displayOrder = remember(shown, groupsState, query) {
        if (query.isNotBlank()) shown
        else groupsState.groups.flatMap { g -> shown.filter { groupsState.groupOf(it.key) == g.id } }
    }

    fun exitSelection() {
        selecting = false
        selected = emptySet()
    }

    fun openItem(item: OfflineItem) {
        if (kind == OfflineKind.PHOTOS) {
            photoViewer = item
        } else {
            shortsViewerIndex = displayOrder.indexOfFirst { it.key == item.key }.coerceAtLeast(0)
        }
    }

    fun toggle(item: OfflineItem) {
        selected = if (item.key in selected) selected - item.key else selected + item.key
        if (selected.isEmpty()) selecting = false
    }

    val title = if (selecting) {
        stringResource(R.string.offl_selected_fmt, selected.size, shown.size)
    } else {
        stringResource(if (kind == OfflineKind.PHOTOS) R.string.offl_photos_title else R.string.offl_shorts_title)
    }

    HanimeScaffold(
        title = title,
        onBack = if (selecting) ::exitSelection else onBack,
        actions = {
            if (selecting) {
                FilledIconButton(onClick = {
                    selected = if (selected.size == shown.size) emptySet() else shown.map { it.key }.toSet()
                    if (selected.isEmpty()) selecting = false
                }) { Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all)) }
                FilledIconButton(
                    enabled = selected.isNotEmpty(),
                    onClick = { showMove = true },
                ) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(R.string.move_group)) }
                FilledIconButton(
                    enabled = selected.isNotEmpty(),
                    onClick = { confirmDelete = shown.filter { it.key in selected } },
                ) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
            } else if (pager.currentPage == 0) {
                val hasActive = tasks.any { it.isActive }
                val hasResumable = tasks.any {
                    it.state == OfflineDownloads.State.PAUSED || it.state == OfflineDownloads.State.FAILED
                }
                FilledIconButton(
                    enabled = hasActive || hasResumable,
                    onClick = {
                        if (hasActive) OfflineDownloads.pauseAll(kind) else OfflineDownloads.resumeAll(kind)
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            if (hasActive) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24
                        ),
                        contentDescription = stringResource(if (hasActive) R.string.pause_all else R.string.start_all),
                    )
                }
            } else {
                FilledIconButton(onClick = { showSearch = !showSearch }) {
                    Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = stringResource(R.string.search))
                }
                Box {
                    FilledIconButton(onClick = { showSortMenu = true }) {
                        Icon(painterResource(R.drawable.baseline_sort_24), contentDescription = stringResource(R.string.sort_option))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DownloadSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes)) },
                                onClick = { sort = option; showSortMenu = false },
                            )
                        }
                    }
                }
                FilledIconButton(onClick = { showCreateGroup = true }) {
                    Icon(painterResource(R.drawable.baseline_add_24), contentDescription = stringResource(R.string.create_new_group))
                }
                FilledIconButton(onClick = { reload++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.offl_refresh))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pager.currentPage) {
                Tab(
                    selected = pager.currentPage == 0,
                    onClick = { exitSelection(); scope.launch { pager.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.downloading)) },
                )
                Tab(
                    selected = pager.currentPage == 1,
                    onClick = { scope.launch { pager.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.downloaded)) },
                )
            }
            AnimatedVisibility(visible = showSearch && pager.currentPage == 1 && !selecting) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.offl_search_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.close))
                            }
                        }
                    },
                )
            }
            HorizontalPager(
                state = pager,
                userScrollEnabled = !selecting,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                if (page == 0) {
                    DownloadingTab(kind, tasks, onCancel = { cancelTask = it })
                } else if (shown.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyContent(
                            hint = stringResource(R.string.empty_content),
                            subHint = stringResource(
                                if (kind == OfflineKind.PHOTOS) R.string.offl_empty_photos else R.string.offl_empty_shorts
                            ),
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(112.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val flat = query.isNotBlank()
                        val groupsToShow = if (flat) listOf(OfflineGroup(-1, "")) else groupsState.groups
                        groupsToShow.forEach { group ->
                            val members = if (flat) shown else shown.filter { groupsState.groupOf(it.key) == group.id }
                            if (!flat) {
                                item(key = "h_${group.id}", span = { GridItemSpan(maxLineSpan) }) {
                                    GroupHeader(
                                        name = if (group.id == OfflineGroups.DEFAULT_ID) ungrouped else group.name,
                                        count = members.size,
                                        expanded = group.id !in collapsed,
                                        onToggle = {
                                            collapsed = if (group.id in collapsed) collapsed - group.id else collapsed + group.id
                                        },
                                        onLongClick = {
                                            if (group.id != OfflineGroups.DEFAULT_ID && !selecting) editGroup = group
                                        },
                                    )
                                }
                            }
                            if (flat || group.id !in collapsed) {
                                items(members, key = { it.key }) { item ->
                                    MediaGridCard(
                                        thumb = item.coverUri,
                                        title = item.title,
                                        subtitle = cardSubtitle(item),
                                        selecting = selecting,
                                        selected = item.key in selected,
                                        badge = if (kind == OfflineKind.PHOTOS) item.count.toString() else null,
                                        showPlayIcon = kind == OfflineKind.SHORTS,
                                        onClick = {
                                            if (selecting) toggle(item)
                                            else if (kind == OfflineKind.PHOTOS) openItem(item)
                                            else popup = item
                                        },
                                        onDoubleClick = { openItem(item) },
                                        onLongClick = {
                                            if (!selecting) {
                                                selecting = true
                                                selected = setOf(item.key)
                                            } else toggle(item)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    popup?.let { item ->
        ShortDetailsPopup(
            item = item,
            onDismiss = { popup = null },
            onPlay = { popup = null; openItem(item) },
            onExternal = {
                popup = null
                context.openDownloadedHanimeVideoLocally(item.mediaUris.first()) {
                    GlobalToasts.show(context.getString(R.string.offl_not_found), level = GlobalToasts.ToastLevel.WARNING)
                }
            },
            onDelete = { popup = null; confirmDelete = listOf(item) },
            groupName = groupLabel(groupsState.groups, groupsState.groupOf(item.key), ungrouped),
        )
    }

    photoViewer?.let { album ->
        OfflinePhotoViewer(
            album = album,
            onDismiss = { photoViewer = null },
            onDeletePhoto = { uri ->
                photoViewer = null
                scope.launch {
                    withContext(Dispatchers.IO) {
                        OfflineFs.delete(context, uri)
                        if (album.count <= 1) {
                            OfflineFs.deleteDir(context, album.dirSegments)
                            OfflineGroups.forget(kind, listOf(album.key))
                        }
                    }
                    reload++
                }
            },
        )
    }

    shortsViewerIndex?.let { index ->
        val clips = displayOrder
        if (clips.isNotEmpty()) {
            OfflineShortsViewer(clips = clips, initialIndex = index, onDismiss = { shortsViewerIndex = null })
        } else {
            shortsViewerIndex = null
        }
    }

    confirmDelete?.let { list ->
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.delete),
            message = stringResource(R.string.offl_confirm_delete_items, list.size),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                confirmDelete = null
                exitSelection()
                scope.launch {
                    OfflineRepository.delete(context, list)
                    reload++
                }
            },
            onDismiss = { confirmDelete = null },
        )
    }

    cancelTask?.let { task ->
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.delete),
            message = stringResource(R.string.offl_confirm_cancel_task),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = { OfflineDownloads.cancel(task.id); cancelTask = null },
            onDismiss = { cancelTask = null },
        )
    }

    if (showCreateGroup) {
        GroupNameDialog(
            title = stringResource(R.string.offl_new_group),
            initial = "",
            onDismiss = { showCreateGroup = false },
            onConfirm = { OfflineGroups.create(kind, it); showCreateGroup = false },
        )
    }

    editGroup?.let { group ->
        GroupNameDialog(
            title = stringResource(R.string.offl_rename_group),
            initial = group.name,
            onDismiss = { editGroup = null },
            onConfirm = { OfflineGroups.rename(kind, group.id, it); editGroup = null },
            onDelete = { OfflineGroups.delete(kind, group.id); editGroup = null },
        )
    }

    if (showMove) {
        MoveGroupPicker(
            groups = groupsState.groups,
            defaultName = ungrouped,
            onDismiss = { showMove = false },
            onPick = { id ->
                OfflineGroups.move(kind, selected, id)
                showMove = false
                exitSelection()
            },
        )
    }

    OfflineMigrationGate(onMigrated = { reload++ })
}

private fun groupLabel(groups: List<OfflineGroup>, id: Int, ungrouped: String): String =
    if (id == OfflineGroups.DEFAULT_ID) ungrouped else groups.firstOrNull { it.id == id }?.name ?: ungrouped

@Composable
private fun cardSubtitle(item: OfflineItem): String {
    val parts = ArrayList<String>()
    if (item.kind == OfflineKind.PHOTOS) {
        parts += stringResource(R.string.offl_photos_fmt, item.count)
    } else {
        item.channel?.let { parts += it }
        item.views.compactCount()?.let { parts += it }
    }
    parts += item.sizeBytes.formatFileSizeV2()
    return parts.joinToString(" · ")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupHeader(
    name: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onToggle, onLongClick = onLongClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.offl_items_fmt, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = onToggle,
                label = { Text(stringResource(if (expanded) R.string.collapse else R.string.expand)) },
            )
        }
    }
}

@Composable
private fun DownloadingTab(
    kind: OfflineKind,
    tasks: List<OfflineDownloads.Task>,
    onCancel: (OfflineDownloads.Task) -> Unit,
) {
    if (tasks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyContent(
                hint = stringResource(R.string.empty_content),
                subHint = stringResource(R.string.offl_empty_downloading),
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(task, onCancel = { onCancel(task) })
        }
    }
}

@Composable
private fun TaskCard(task: OfflineDownloads.Task, onCancel: () -> Unit) {
    val req = task.request
    val state = task.state
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = state == OfflineDownloads.State.FAILED) { OfflineDownloads.resume(task.id) },
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!req.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = req.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 60.dp, height = 80.dp).clip(RoundedCornerShape(10.dp)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(req.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(req.siteFolder, req.channel ?: req.folder).distinct().joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LinearProgressIndicator(progress = { task.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(
                    text = when (state) {
                        OfflineDownloads.State.QUEUED -> stringResource(R.string.offl_task_queued)
                        OfflineDownloads.State.DOWNLOADING ->
                            if (req.kind == OfflineKind.PHOTOS) {
                                stringResource(R.string.offl_task_progress_photos, task.done, task.total)
                            } else {
                                stringResource(R.string.download_progress_percent, (task.progress * 100).toInt())
                            }
                        OfflineDownloads.State.PAUSED -> stringResource(R.string.paused)
                        OfflineDownloads.State.FAILED -> stringResource(R.string.offl_task_failed)
                        OfflineDownloads.State.DONE -> stringResource(R.string.offl_task_done)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state == OfflineDownloads.State.FAILED) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (state) {
                OfflineDownloads.State.QUEUED, OfflineDownloads.State.DOWNLOADING ->
                    IconButton(onClick = { OfflineDownloads.pause(task.id) }) {
                        Icon(painterResource(R.drawable.ic_baseline_pause_24), contentDescription = stringResource(R.string.paused))
                    }
                OfflineDownloads.State.PAUSED, OfflineDownloads.State.FAILED ->
                    IconButton(onClick = { OfflineDownloads.resume(task.id) }) {
                        Icon(painterResource(R.drawable.ic_baseline_play_arrow_24), contentDescription = stringResource(R.string.retry))
                    }
                OfflineDownloads.State.DONE -> Unit
            }
            IconButton(onClick = onCancel) {
                Icon(painterResource(R.drawable.ic_baseline_delete_24), contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun ShortDetailsPopup(
    item: OfflineItem,
    groupName: String,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onExternal: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (item.coverUri != null) {
                    AsyncImage(
                        model = item.coverUri,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)),
                    )
                }
                Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                DetailLine(stringResource(R.string.offl_detail_site), item.site)
                DetailLine(stringResource(R.string.offl_detail_channel), item.channel)
                DetailLine(stringResource(R.string.offl_detail_views), item.views.compactCount())
                DetailLine(stringResource(R.string.offl_detail_likes), item.likes.compactCount())
                DetailLine(stringResource(R.string.offl_detail_duration), item.durationSeconds?.let(::formatDuration))
                DetailLine(stringResource(R.string.offl_detail_size), item.sizeBytes.formatFileSizeV2())
                DetailLine(stringResource(R.string.offl_detail_added), formatDate(item.addedAt))
                DetailLine(stringResource(R.string.offl_detail_group), groupName)
                DetailLine(stringResource(R.string.offl_detail_source), item.sourceUrl)
                DetailLine(
                    stringResource(R.string.offl_detail_tags),
                    item.tags.takeIf { it.isNotEmpty() }?.joinToString(", "),
                )
                DetailLine(item.description?.let { stringResource(R.string.offl_details) } ?: "", item.description)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = onPlay,
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
