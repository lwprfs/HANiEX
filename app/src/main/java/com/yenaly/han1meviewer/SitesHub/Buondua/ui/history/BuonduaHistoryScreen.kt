package com.yenaly.han1meviewer.SitesHub.Buondua.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.SitesHub.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.SitesHub.Buondua.data.local.BuonduaHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Buondua.data.repository.BuonduaHistoryRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SiteHistoryEntry
import com.yenaly.han1meviewer.ui.component.SiteHistoryScreen
import kotlinx.coroutines.flow.map

@Composable
fun BuonduaHistoryScreen(
    onNavigateToAlbum: (String) -> Unit,
    onBack: () -> Unit,
) {
    BuonduaHistoryRepo.init(LocalContext.current)
    val entries = remember { BuonduaHistoryRepo.loadAll().map { list -> list.map { it.toEntry() } } }
    SiteHistoryScreen(
        site = SiteType.BUONDUA,
        entries = entries,
        emptyHint = R.string.buondua_no_history,
        imageLoader = { BuonduaImageLoader.get(it) },
        onDeleteAll = { BuonduaHistoryRepo.deleteAll() },
        onDelete = { BuonduaHistoryRepo.deleteByVideoCode(it) },
        onNavigateToVideo = onNavigateToAlbum,
        onBack = onBack,
    )
}

private fun BuonduaHistoryEntity.toEntry() = SiteHistoryEntry(
    videoCode = videoCode,
    title = title,
    coverUrl = coverUrl,
    progress = null,
    watchDate = watchDate,
)
