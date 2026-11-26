package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiImageLoader
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiTranslation
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.local.WumaobiHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.repository.WumaobiHistoryRepo
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun WumaobiHistoryScreen(
    onNavigateToVideo: (String) -> Unit,
    onBack: () -> Unit,
) {
    WumaobiHistoryRepo.init(LocalContext.current)
    val entries = remember { WumaobiHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        entries = entries,
        emptyHint = R.string.wumaobi_no_history,
        imageLoader = { WumaobiImageLoader.get(it) },
        onDeleteAll = { WumaobiHistoryRepo.deleteAll() },
        onDelete = { WumaobiHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToVideo,
        onBack = onBack,
        titleOf = { WumaobiTranslation.title(it) },
    )
}

private fun WumaobiHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = if (totalDuration > 0L) lastPosition.toFloat() / totalDuration else null,
)
