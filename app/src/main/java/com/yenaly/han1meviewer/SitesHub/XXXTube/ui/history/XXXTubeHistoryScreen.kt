package com.yenaly.han1meviewer.SitesHub.XXXTube.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeImageLoader
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.local.XXXTubeHistoryEntity
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.repository.XXXTubeHistoryRepo
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun XXXTubeHistoryScreen(
    onNavigateToVideo: (String) -> Unit,
    onBack: () -> Unit,
) {
    XXXTubeHistoryRepo.init(LocalContext.current)
    val entries = remember { XXXTubeHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        entries = entries,
        emptyHint = R.string.xxxtube_no_history,
        imageLoader = { XXXTubeImageLoader.get(it) },
        onDeleteAll = { XXXTubeHistoryRepo.deleteAll() },
        onDelete = { XXXTubeHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToVideo,
        onBack = onBack,
    )
}

private fun XXXTubeHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = if (totalDuration > 0L) lastPosition.toFloat() / totalDuration else null,
)
