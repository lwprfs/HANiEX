package com.yenaly.han1meviewer.SitesHub.Cosplaytele.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleImageLoader
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.local.CosplayteleHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.repository.CosplayteleHistoryRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun CosplayteleHistoryScreen(
    onNavigateToPost: (String) -> Unit,
    onBack: () -> Unit,
) {
    CosplayteleHistoryRepo.init(LocalContext.current)
    val entries = remember { CosplayteleHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        entries = entries,
        emptyHint = R.string.cosplaytele_no_history,
        imageLoader = { CosplayteleImageLoader.get(it) },
        onDeleteAll = { CosplayteleHistoryRepo.deleteAll() },
        onDelete = { CosplayteleHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToPost,
        onBack = onBack,
    )
}

private fun CosplayteleHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = if (totalDuration > 0L) lastPosition.toFloat() / totalDuration else null,
)
