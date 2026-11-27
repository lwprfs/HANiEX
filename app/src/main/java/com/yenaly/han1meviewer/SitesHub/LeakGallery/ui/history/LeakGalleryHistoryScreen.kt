package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryImageLoader
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.local.LeakGalleryHistoryEntity
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.repository.LeakGalleryHistoryRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun LeakGalleryHistoryScreen(
    onNavigateToPost: (String) -> Unit,
    onBack: () -> Unit,
) {
    LeakGalleryHistoryRepo.init(LocalContext.current)
    val entries = remember { LeakGalleryHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        site = SiteType.LEAKGALLERY,
        entries = entries,
        emptyHint = R.string.leakgallery_no_history,
        imageLoader = { LeakGalleryImageLoader.get(it) },
        onDeleteAll = { LeakGalleryHistoryRepo.deleteAll() },
        onDelete = { LeakGalleryHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToPost,
        onBack = onBack,
    )
}

private fun LeakGalleryHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = if (totalDuration > 0L) lastPosition.toFloat() / totalDuration else null,
    positionMs = lastPosition,
    durationMs = totalDuration,
    watchDate = watchDate,
)
