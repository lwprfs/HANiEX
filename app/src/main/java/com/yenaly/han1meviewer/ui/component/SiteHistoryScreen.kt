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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.yenaly.han1meviewer.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

data class SiteHistoryEntry(
    val videoCode: String,
    val title: String,
    val coverUrl: String,

    val progress: Float?,
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
) {
    val scope = rememberCoroutineScope()
    val items by entries.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.watch_history)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { scope.launch { onDeleteAll() } }) {
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
                    HistoryGridCard(
                        title = titleOf(entry.title),
                        coverUrl = entry.coverUrl,
                        onClick = { onNavigateToVideo(entry.videoCode) },
                        onDelete = { scope.launch { onDelete(entry.videoCode) } },
                        progress = entry.progress,
                        imageLoader = loader,
                    )
                }
            }
        }
    }
}
