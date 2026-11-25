package com.yenaly.han1meviewer.SitesHub.Coser.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserImageLoader
import com.yenaly.han1meviewer.SitesHub.Coser.data.local.CoserHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Coser.data.repository.CoserHistoryRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun CoserHistoryScreen(
    onNavigateToPost: (String) -> Unit,
    onBack: () -> Unit,
) {
    CoserHistoryRepo.init(LocalContext.current)
    val entries = remember { CoserHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        entries = entries,
        emptyHint = R.string.coser_no_history,
        imageLoader = { CoserImageLoader.get(it) },
        onDeleteAll = { CoserHistoryRepo.deleteAll() },
        onDelete = { CoserHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToPost,
        onBack = onBack,
    )
}

private fun CoserHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = if (totalDuration > 0L) lastPosition.toFloat() / totalDuration else null,
)
