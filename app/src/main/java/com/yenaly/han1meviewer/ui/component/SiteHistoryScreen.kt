package com.yenaly.han1meviewer.ui.component

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.util.offline.LocalWatchStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

data class SiteHistoryEntry(
    val videoCode: String,
    val title: String,
    val coverUrl: String,

    val progress: Float?,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val watchDate: Long = 0L,
)

@Composable
fun SiteHistoryScreen(
    entries: Flow<List<SiteHistoryEntry>>,
    @StringRes emptyHint: Int,
    imageLoader: (Context) -> ImageLoader,
    onDeleteAll: suspend () -> Unit,
    onDelete: suspend (videoCode: String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onBack: () -> Unit,
    titleOf: (String) -> String = { it },
    site: SiteType? = null,
) {
    val scope = rememberCoroutineScope()
    val rawItems by entries.collectAsState(initial = emptyList())
    // one card per video: the list is newest first, so the first entry is the latest state
    val items = remember(rawItems) { rawItems.distinctBy { it.videoCode } }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.watch_history)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                onDeleteAll()
                                if (site != null) LocalWatchStore.clear(site)
                            }
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding)) { SiteEmptyContent(hint = stringResource(emptyHint)) }
        } else {
            val loader = imageLoader(LocalContext.current)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(12.dp),
            ) {
                items(items, key = { it.videoCode }) { entry ->
                    val tags = if (site != null) {
                        rememberHistoryTags(
                            site = site,
                            rawCode = entry.videoCode,
                            positionMs = entry.positionMs,
                            durationMs = entry.durationMs,
                            completed = entry.durationMs > 0L && entry.positionMs >= entry.durationMs - 3_000L,
                            watchDate = entry.watchDate,
                        )
                    } else emptyList()
                    HistoryGridCard(
                        title = titleOf(entry.title),
                        coverUrl = entry.coverUrl,
                        onClick = { onNavigateToVideo(entry.videoCode) },
                        onDelete = {
                            scope.launch {
                                onDelete(entry.videoCode)
                                if (site != null) LocalWatchStore.forget(SiteCheckIn.dishCode(site, entry.videoCode))
                            }
                        },
                        progress = entry.progress,
                        tags = tags,
                        imageLoader = loader,
                    )
                }
            }
        }
    }
}
