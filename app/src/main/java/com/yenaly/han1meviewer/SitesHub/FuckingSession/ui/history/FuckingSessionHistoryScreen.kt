package com.yenaly.han1meviewer.SitesHub.FuckingSession.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.FuckingSession.common.FuckingSessionImageLoader
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.local.FuckingSessionHistoryEntity
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.repository.FuckingSessionHistoryRepo
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun FuckingSessionHistoryScreen(
    onNavigateToVideo: (String) -> Unit,
    onBack: () -> Unit,
) {
    FuckingSessionHistoryRepo.init(LocalContext.current)
    val entries = remember { FuckingSessionHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        site = SiteType.FUCKINGSESSION,
        entries = entries,
        emptyHint = R.string.fuckingsession_no_history,
        imageLoader = { FuckingSessionImageLoader.get(it) },
        onDeleteAll = { FuckingSessionHistoryRepo.deleteAll() },
        onDelete = { FuckingSessionHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToVideo,
        onBack = onBack,
    )
}

private fun FuckingSessionHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = if (totalDuration > 0L) lastPosition.toFloat() / totalDuration else null,
    positionMs = lastPosition,
    durationMs = totalDuration,
    watchDate = watchDate,
)
