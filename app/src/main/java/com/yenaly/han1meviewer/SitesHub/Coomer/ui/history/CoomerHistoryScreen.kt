package com.yenaly.han1meviewer.SitesHub.Coomer.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerImageLoader
import com.yenaly.han1meviewer.SitesHub.Coomer.data.local.CoomerHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Coomer.data.repository.CoomerHistoryRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun CoomerHistoryScreen(
    onNavigateToPost: (String) -> Unit,
    onBack: () -> Unit,
) {
    CoomerHistoryRepo.init(LocalContext.current)
    val entries = remember { CoomerHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        entries = entries,
        emptyHint = R.string.coomer_no_history,
        imageLoader = { CoomerImageLoader.get(it) },
        onDeleteAll = { CoomerHistoryRepo.deleteAll() },
        onDelete = { CoomerHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToPost,
        onBack = onBack,
    )
}

private fun CoomerHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = if (totalDuration > 0L) lastPosition.toFloat() / totalDuration else null,
)
