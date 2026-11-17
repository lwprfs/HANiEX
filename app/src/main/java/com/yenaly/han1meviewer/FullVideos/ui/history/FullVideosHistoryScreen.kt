package com.yenaly.han1meviewer.FullVideos.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.FullVideos.common.FullVideosImageLoader
import com.yenaly.han1meviewer.FullVideos.data.local.FullVideosHistoryEntity
import com.yenaly.han1meviewer.FullVideos.data.repository.FullVideosHistoryRepo
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun FullVideosHistoryScreen(
    onNavigateToVideo: (String) -> Unit,
    onBack: () -> Unit,
) {
    FullVideosHistoryRepo.init(LocalContext.current)
    val entries = remember { FullVideosHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        entries = entries,
        emptyHint = R.string.fullvideos_no_history,
        imageLoader = { FullVideosImageLoader.get(it) },
        onDeleteAll = { FullVideosHistoryRepo.deleteAll() },
        onDelete = { FullVideosHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToVideo,
        onBack = onBack,
    )
}

private fun FullVideosHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = if (totalDuration > 0L) lastPosition.toFloat() / totalDuration else null,
)
