package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.logic.state.DownloadState
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.DownloadHeaderNode
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.DownloadItemNode
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.DownloadedNode
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.DownloadEvent
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.DownloadFolder
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.util.offline.DownloadedQueue
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.DownloadSort
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.DownloadUiState
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.DownloadedScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.DownloadingScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.MoveGroupDialog
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.downloadSortComparator
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.toDisplayGroups
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.toFlatNodeList
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.toNodeList
import com.yenaly.han1meviewer.util.toSimplified
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadScreen(
    downloadingFlow: Flow<List<HanimeDownloadEntity>>,
    downloadedFlow: StateFlow<List<VideoWithCategories>>,
    downloadedGroupsFlow: StateFlow<List<DownloadGroupEntity>>,
    collapseDownloadedGroup: Boolean,
    onBack: () -> Unit,
    onLoadDownloaded: () -> Unit,
    onEvent: (DownloadEvent) -> Unit,
) {

    val downloadingItemsState by remember(downloadingFlow) {
        downloadingFlow.map { it as List<HanimeDownloadEntity>? }
    }.collectAsStateWithLifecycle(initialValue = null)
    val downloadingItems = downloadingItemsState.orEmpty()
    val downloadedItems by downloadedFlow.collectAsStateWithLifecycle()
    val downloadedGroups by downloadedGroupsFlow.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    var defaultPageApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(downloadingItemsState) {
        val items = downloadingItemsState ?: return@LaunchedEffect
        if (!defaultPageApplied) {
            defaultPageApplied = true

            val targetPage = if (items.isEmpty()) 1 else 0
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }
    var folderBySite by rememberSaveable { mutableStateOf(false) }
    var openFolderKey by rememberSaveable { mutableStateOf<String?>(null) }
    val downloadedLazyListState = rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) {
        androidx.compose.foundation.lazy.grid.LazyGridState()
    }
    val scope = rememberCoroutineScope()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedVideoIds by remember { mutableStateOf(setOf<Int>()) }
    var pendingBatchMove by remember { mutableStateOf(false) }
    var pendingBatchMoveConfirm by remember { mutableStateOf<Pair<List<VideoWithCategories>, Int>?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(DownloadSort.DATE_DESC) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var pendingBatchDelete by remember { mutableStateOf<List<VideoWithCategories>?>(null) }

    val displayGroups = downloadedGroups.toDisplayGroups()

    val filteredDownloadedItems =
        remember(downloadedItems, searchQuery, sort) {
            val filtered = if (searchQuery.isBlank()) {
                downloadedItems
            } else {
                val normalizedQuery = searchQuery.toSimplified()
                downloadedItems.filter {
                    it.video.title.toSimplified().contains(normalizedQuery, ignoreCase = true) ||
                        it.video.videoCode.contains(searchQuery, ignoreCase = true)
                }
            }
            filtered.sortedWith(downloadSortComparator(sort))
        }

    val searching = searchQuery.isNotBlank()

    val folders = remember(filteredDownloadedItems, displayGroups, folderBySite) {
        if (folderBySite) {
            filteredDownloadedItems
                .groupBy { DownloadedQueue.siteOf(it.video.videoCode) }
                .map { (site, videos) ->
                    DownloadFolder(
                        key = "s:${site.name}",
                        title = CrossSiteNavigator.siteLabel(site),
                        videos = videos,
                        scope = DownloadedQueue.Scope.Site(site),
                    )
                }
                .sortedBy { it.title.lowercase() }
        } else {
            val names = displayGroups.associate { it.id to it.name }
            filteredDownloadedItems
                .groupBy { it.video.groupId }
                .toSortedMap()
                .map { (groupId, videos) ->
                    DownloadFolder(
                        key = "g:$groupId",
                        title = names[groupId] ?: "ID: $groupId",
                        videos = videos,
                        scope = DownloadedQueue.Scope.Group(groupId),
                        groupId = groupId,
                    )
                }
        }
    }
    val openFolder = folders.firstOrNull { it.key == openFolderKey }
    LaunchedEffect(openFolder, openFolderKey, downloadedItems) {

        if (openFolderKey != null && openFolder == null && downloadedItems.isNotEmpty() && !searching) {
            openFolderKey = null
        }
    }

    val downloadedNodes: List<DownloadedNode> = when {
        searching -> filteredDownloadedItems.map { DownloadItemNode(it, it.video.groupId) }
        openFolder != null -> openFolder.videos.map { DownloadItemNode(it, openFolder.groupId ?: -1) }
        else -> emptyList()
    }

    val uiState = DownloadUiState(
        downloadingItems = downloadingItems,
        downloadedNodes = downloadedNodes,
        displayGroups = displayGroups,
        currentPage = pagerState.currentPage,
        showCreateGroupDialog = showCreateGroupDialog,
        multiSelectMode = multiSelectMode,
        selectedVideoIds = selectedVideoIds,
        folders = folders,
        openFolderKey = openFolder?.key,
        folderBySite = folderBySite,
        searching = searching,
    )

    val handleEvent: (DownloadEvent) -> Unit = { event ->
        when (event) {

            is DownloadEvent.OnOpenFolder -> {
                openFolderKey = event.key
                multiSelectMode = false
                selectedVideoIds = emptySet()
            }
            is DownloadEvent.OnFolderModeChange -> {
                folderBySite = event.bySite
                openFolderKey = null
            }
            is DownloadEvent.OnLocalPlayback, is DownloadEvent.OnOpenDownloadedVideo -> {

                DownloadedQueue.setContext(openFolder?.scope ?: DownloadedQueue.Scope.All, sort)
                onEvent(event)
            }
            is DownloadEvent.OnCreateGroupDialogChange -> showCreateGroupDialog = event.visible
            is DownloadEvent.OnPageChange -> {
                scope.launch { pagerState.animateScrollToPage(event.page) }
            }

            is DownloadEvent.OnToggleMultiSelect -> {
                multiSelectMode = !multiSelectMode
                if (!multiSelectMode) selectedVideoIds = emptySet()
            }
            is DownloadEvent.OnToggleVideoSelection -> {
                selectedVideoIds = if (event.videoId in selectedVideoIds) {
                    selectedVideoIds - event.videoId
                } else {
                    selectedVideoIds + event.videoId
                }
            }
            is DownloadEvent.OnSelectAllCurrentGroup -> {
                val groupVideos = downloadedNodes.filterIsInstance<DownloadItemNode>()
                    .filter { it.parentKey == event.groupId }
                selectedVideoIds = if (event.select) {
                    selectedVideoIds + groupVideos.map { it.data.video.id }.toSet()
                } else {
                    selectedVideoIds - groupVideos.map { it.data.video.id }.toSet()
                }
            }
            is DownloadEvent.OnBatchMoveRequest -> { pendingBatchMove = true }

            else -> onEvent(event)
        }
    }

    LaunchedEffect(Unit) {
        onLoadDownloaded()
    }

    BackHandler(enabled = pagerState.currentPage == 1 && (multiSelectMode || (openFolder != null && !searching))) {
        if (multiSelectMode) handleEvent(DownloadEvent.OnToggleMultiSelect) else openFolderKey = null
    }

    val allDownloadedItems = downloadedNodes.filterIsInstance<DownloadItemNode>()
    val selectedDownloaded = allDownloadedItems.filter { it.data.video.id in selectedVideoIds }.map { it.data }

    HanimeScaffold(
        title = if (uiState.multiSelectMode) {
            stringResource(R.string.offl_selected_fmt, selectedDownloaded.size, allDownloadedItems.size)
        } else if (uiState.currentPage == 1 && openFolder != null && !searching) {
            openFolder.title
        } else {
            stringResource(R.string.download)
        },
        onBack = if (uiState.multiSelectMode) {
            { handleEvent(DownloadEvent.OnToggleMultiSelect) }
        } else if (uiState.currentPage == 1 && openFolder != null && !searching) {
            { openFolderKey = null }
        } else {
            onBack
        },
        actions = {
            if (uiState.currentPage == 0) {

                val hasActive = uiState.downloadingItems.any {
                    it.state == DownloadState.Downloading || it.state == DownloadState.Queued
                }
                val hasResumable = uiState.downloadingItems.any {
                    it.state == DownloadState.Paused || it.state == DownloadState.Unknown
                }
                FilledIconButton(
                    onClick = {
                        if (hasActive) {
                            handleEvent(DownloadEvent.OnPauseAll(uiState.downloadingItems))
                        } else {
                            handleEvent(DownloadEvent.OnResumeAll(uiState.downloadingItems))
                        }
                    },
                    enabled = hasActive || hasResumable
                ) {
                    Icon(
                        painter = painterResource(
                            if (hasActive) R.drawable.ic_baseline_pause_24
                            else R.drawable.ic_baseline_play_arrow_24
                        ),
                        contentDescription = stringResource(
                            if (hasActive) R.string.pause_all else R.string.start_all
                        ),
                    )
                }
            } else {
                if (!uiState.multiSelectMode) {
                    FilledIconButton(
                        onClick = { showSearchBar = !showSearchBar }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_search_24),
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                    Box {
                        FilledIconButton(
                            onClick = { showSortMenu = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_sort_24),
                                contentDescription = stringResource(R.string.sort_option),
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            DownloadSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelRes)) },
                                    onClick = {
                                        sort = option
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                    if (downloadedNodes.isNotEmpty()) {
                        FilledIconButton(
                            onClick = { handleEvent(DownloadEvent.OnToggleMultiSelect) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_format_list_bulleted_24),
                                contentDescription = stringResource(R.string.edit),
                            )
                        }
                    }
                    FilledIconButton(
                        onClick = { handleEvent(DownloadEvent.OnCreateGroupDialogChange(true)) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_add_24),
                            contentDescription = stringResource(R.string.create_new_group),
                        )
                    }
                    FilledIconButton(
                        onClick = { handleEvent(DownloadEvent.OnImportDownloaded) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_download_24),
                            contentDescription = stringResource(R.string.read_download_dir_title),
                        )
                    }
                } else {
                    FilledIconButton(onClick = {
                        selectedVideoIds = if (selectedVideoIds.size == allDownloadedItems.size) {
                            emptySet()
                        } else {
                            allDownloadedItems.map { it.data.video.id }.toSet()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.SelectAll,
                            contentDescription = stringResource(R.string.select_all),
                        )
                    }
                    FilledIconButton(
                        enabled = selectedDownloaded.isNotEmpty(),
                        onClick = { handleEvent(DownloadEvent.OnBatchMoveRequest) },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = stringResource(R.string.move_group),
                        )
                    }
                    FilledIconButton(
                        enabled = selectedDownloaded.isNotEmpty(),
                        onClick = { pendingBatchDelete = selectedDownloaded },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(selectedTabIndex = uiState.currentPage) {
                Tab(
                    selected = uiState.currentPage == 0,
                    onClick = { handleEvent(DownloadEvent.OnPageChange(0)) },
                    text = { Text(stringResource(R.string.downloading)) },
                )
                Tab(
                    selected = uiState.currentPage == 1,
                    onClick = { handleEvent(DownloadEvent.OnPageChange(1)) },
                    text = { Text(stringResource(R.string.downloaded)) },
                )
            }

            AnimatedVisibility(visible = showSearchBar && uiState.currentPage == 1) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_downloaded_hint)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_search_24),
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.close),
                                )
                            }
                        }
                    },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> DownloadingScreen(
                        uiState = uiState,
                        onEvent = handleEvent,
                    )

                    else -> DownloadedScreen(
                        uiState = uiState,
                        listState = downloadedLazyListState,
                        onEvent = handleEvent,
                    )
                }
            }
        }
    }

    pendingBatchDelete?.let { videos ->
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.delete),
            message = stringResource(R.string.confirm_delete_videos, videos.size),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                handleEvent(DownloadEvent.OnBatchDelete(videos))
                pendingBatchDelete = null
                multiSelectMode = false
                selectedVideoIds = emptySet()
            },
            onDismiss = { pendingBatchDelete = null },
        )
    }

    com.yenaly.han1meviewer.ui.screen.library.offline.OfflineMigrationGate(
        onMigrated = { onLoadDownloaded() },
    )

    if (pendingBatchMove) {
        val selectedVideos = downloadedNodes
            .filterIsInstance<DownloadItemNode>()
            .filter { it.data.video.id in selectedVideoIds }
            .map { it.data }
        if (selectedVideos.isNotEmpty()) {
            MoveGroupDialog(
                video = selectedVideos.first(),
                groups = displayGroups,
                onDismiss = { pendingBatchMove = false },
                onConfirm = { _, groupId ->
                    pendingBatchMoveConfirm = Pair(selectedVideos, groupId)
                    pendingBatchMove = false
                },
            )
        } else {
            pendingBatchMove = false
        }
    }

    pendingBatchMoveConfirm?.let { (videos, groupId) ->
        val groupName = displayGroups.find { it.id == groupId }?.name ?: "ID:$groupId"
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.move_group),
            message = stringResource(R.string.confirm_move_videos,videos.size,groupName),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                handleEvent(DownloadEvent.OnBatchMoveGroup(videos, groupId))
                pendingBatchMoveConfirm = null
                multiSelectMode = false
                selectedVideoIds = emptySet()
            },
            onDismiss = { pendingBatchMoveConfirm = null },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadScreenPreview() {
    ComponentPreview {
        DownloadScreen(
            downloadingFlow = flowOf(emptyList()),
            downloadedFlow = MutableStateFlow(emptyList()),
            downloadedGroupsFlow = MutableStateFlow(emptyList()),
            collapseDownloadedGroup = false,
            onBack = {},
            onLoadDownloaded = {},
            onEvent = {},
        )
    }
}

private fun intBooleanMapSaver(): Saver<Map<Int, Boolean>, ArrayList<String>> {
    return Saver(
        save = { state -> ArrayList(state.map { (key, value) -> "$key=$value" }) },
        restore = { saved ->
            saved.mapNotNull { item ->
                val separatorIndex = item.lastIndexOf('=')
                if (separatorIndex <= 0) return@mapNotNull null
                val key = item.substring(0, separatorIndex).toIntOrNull()
                    ?: return@mapNotNull null
                val value = item.substring(separatorIndex + 1).toBooleanStrictOrNull()
                    ?: return@mapNotNull null
                key to value
            }.toMap()
        },
    )
}
