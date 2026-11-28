package com.yenaly.han1meviewer.ui.screen.history

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.local.FapFolderHistoryRepo
import com.yenaly.han1meviewer.SitesHub.SupJav.data.repository.SupJavHistoryRepo
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.repository.RouVideoHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Buondua.data.repository.BuonduaHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.repository.CosplayteleHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Coomer.data.repository.CoomerHistoryRepo
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.repository.LeakGalleryHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Coser.data.repository.CoserHistoryRepo
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.repository.FullVideosHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.repository.WumaobiHistoryRepo
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.repository.FuckingSessionHistoryRepo
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.repository.XXXTubeHistoryRepo
import com.yenaly.han1meviewer.SitesHub.XHamster.data.repository.XHamsterHistoryRepo
import com.yenaly.han1meviewer.SitesHub.HentaiMama.data.local.HentaiMamaHistoryRepo
import com.yenaly.han1meviewer.SitesHub.MissAV.data.repository.MissAvHistoryRepo
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.local.StrawberryHistoryRepo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.CrossSiteNavigator
import com.yenaly.han1meviewer.ui.navigation.PendingVideoOpen
import com.yenaly.han1meviewer.ui.component.HistoryGridCard
import com.yenaly.han1meviewer.ui.component.rememberHistoryTags
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.util.offline.LocalWatchEntry
import com.yenaly.han1meviewer.util.offline.LocalWatchStore
import com.yenaly.han1meviewer.ui.component.SiteImageLoaders
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

data class UnifiedHistoryItem(
    val site: SiteType,
    val code: String,
    val title: String,
    val coverUrl: String,
    val watchDate: Long,
    val positionMs: Long,
    val durationMs: Long,
    val path: String?,
    val subtitle: String?,
    val completed: Boolean,
    val autoPlay: Boolean = false,
) {
    val key: String get() = "${site.name}:$code"

    val progress: Float?
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null

    fun toRequest(): PendingVideoOpen = PendingVideoOpen(
        site = site,
        videoCode = code,
        path = path,

        resumePosition = if (site == SiteType.HENTAIMAMA && !completed) positionMs else 0L,
        title = title,
        autoPlay = autoPlay,
    )
}

class UnifiedHistoryViewModel(application: Application) : AndroidViewModel(application) {

    init {

        MissAvHistoryRepo.init(application)
        HentaiMamaHistoryRepo.init(application)
        OnAirHistoryRepo.init(application)
        StrawberryHistoryRepo.init(application)
        FapFolderHistoryRepo.init(application)
        SupJavHistoryRepo.init(application)
        RouVideoHistoryRepo.init(application)
        XHamsterHistoryRepo.init(application)
        BuonduaHistoryRepo.init(application)
        CosplayteleHistoryRepo.init(application)
        CoserHistoryRepo.init(application)
        FullVideosHistoryRepo.init(application)
        FuckingSessionHistoryRepo.init(application)
        WumaobiHistoryRepo.init(application)
        XXXTubeHistoryRepo.init(application)
        CoomerHistoryRepo.init(application)
        LeakGalleryHistoryRepo.init(application)
    }

    private fun <T> safe(flow: kotlinx.coroutines.flow.Flow<List<T>>) =
        flow.catch { emit(emptyList()) }

    val items: StateFlow<List<UnifiedHistoryItem>?> = combine(
        combine(
        combine(
        combine(
        safe(DatabaseRepo.WatchHistory.loadAll().map { it.toList() }),
        safe(MissAvHistoryRepo.loadAll()),
        safe(HentaiMamaHistoryRepo.loadAll()),
        safe(OnAirHistoryRepo.loadAll()),
        safe(FapFolderHistoryRepo.loadAll()),
        safe(StrawberryHistoryRepo.loadAll()),
    ) { hanime, missav, mama, onair, fap, strawberry ->
        buildList {
            hanime.forEach { add(it.toItem()) }
            missav.forEach {
                add(
                    UnifiedHistoryItem(
                        site = SiteType.MISSAV,
                        code = it.videoCode,
                        title = it.title,
                        coverUrl = it.coverUrl,
                        watchDate = it.watchDate,
                        positionMs = it.lastPosition,
                        durationMs = it.totalDuration,
                        path = "/en/${it.videoCode}",
                        subtitle = null,
                        completed = it.totalDuration > 0 &&
                                it.lastPosition >= it.totalDuration - 3_000L,
                    )
                )
            }
            mama.forEach {
                add(
                    UnifiedHistoryItem(
                        site = SiteType.HENTAIMAMA,
                        code = it.videoCode,
                        title = it.title,
                        coverUrl = it.coverUrl,
                        watchDate = it.watchDate,
                        positionMs = it.lastPosition,
                        durationMs = it.totalDuration,
                        path = it.lastEpisodeUrl.ifBlank { null },
                        subtitle = it.lastEpisodeTitle.ifBlank { null },
                        completed = it.completed,
                    )
                )
            }
            onair.forEach {
                add(
                    UnifiedHistoryItem(
                        site = SiteType.ONAIR,
                        code = it.videoId,
                        title = it.title,
                        coverUrl = it.coverUrl,
                        watchDate = it.watchDate,
                        positionMs = it.lastPosition,
                        durationMs = it.totalDuration,
                        path = null,
                        subtitle = it.uploaderName,
                        completed = it.completed,
                    )
                )
            }
            strawberry.forEach {
                add(
                    UnifiedHistoryItem(
                        site = SiteType.STRAWBERRY,
                        code = it.videoId,
                        title = it.title,
                        coverUrl = it.coverUrl,
                        watchDate = it.watchDate,
                        positionMs = it.lastPosition,
                        durationMs = it.totalDuration,
                        path = null,
                        subtitle = it.uploaderName,
                        completed = it.completed,
                    )
                )
            }
            fap.forEach {
                add(
                    UnifiedHistoryItem(
                        site = SiteType.FAPFOLDER,
                        code = it.postId.toString(),
                        title = it.title,
                        coverUrl = it.coverUrl,
                        watchDate = it.watchDate,
                        positionMs = it.lastPosition,
                        durationMs = it.totalDuration,

                        path = it.postSlug?.ifBlank { null },
                        subtitle = it.authorName,
                        completed = it.completed,
                        autoPlay = it.isVideo,
                    )
                )
            }
        }
    },
        safe(SupJavHistoryRepo.loadAll()),
    ) { base, supjav ->
        base to supjav
    },
        safe(RouVideoHistoryRepo.loadAll()),
    ) { (base, supjav), rouvideo ->
        (base + rouvideo.map {
            UnifiedHistoryItem(
                site = SiteType.ROUVIDEO,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        } + supjav.map {
            UnifiedHistoryItem(
                site = SiteType.SUPJAV,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    },
        combine(safe(XHamsterHistoryRepo.loadAll()), safe(BuonduaHistoryRepo.loadAll())) { x, b -> x to b },
    ) { base, (xhamster, buondua) ->
        (base + buondua.map {
            UnifiedHistoryItem(
                site = SiteType.BUONDUA,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = 0L,
                durationMs = 0L,
                path = null,
                subtitle = null,
                completed = false,
            )
        } + xhamster.map {
            UnifiedHistoryItem(
                site = SiteType.XHAMSTER,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(safe(CosplayteleHistoryRepo.loadAll())) { base, cosplaytele ->
        (base + cosplaytele.map {
            UnifiedHistoryItem(
                site = SiteType.COSPLAYTELE,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(safe(CoserHistoryRepo.loadAll())) { base, coser ->
        (base + coser.map {
            UnifiedHistoryItem(
                site = SiteType.COSER,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(safe(FullVideosHistoryRepo.loadAll())) { base, fullvideos ->
        (base + fullvideos.map {
            UnifiedHistoryItem(
                site = SiteType.FULLVIDEOS,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(safe(FuckingSessionHistoryRepo.loadAll())) { base, fuckingsession ->
        (base + fuckingsession.map {
            UnifiedHistoryItem(
                site = SiteType.FUCKINGSESSION,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(safe(WumaobiHistoryRepo.loadAll())) { base, wumaobi ->
        (base + wumaobi.map {
            UnifiedHistoryItem(
                site = SiteType.WUMAOBI,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(safe(XXXTubeHistoryRepo.loadAll())) { base, xxxtube ->
        (base + xxxtube.map {
            UnifiedHistoryItem(
                site = SiteType.XXXTUBE,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(safe(LeakGalleryHistoryRepo.loadAll())) { base, leakgallery ->
        (base + leakgallery.map {
            UnifiedHistoryItem(
                site = SiteType.LEAKGALLERY,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(safe(CoomerHistoryRepo.loadAll())) { base, coomer ->
        (base + coomer.map {
            UnifiedHistoryItem(
                site = SiteType.COOMER,
                code = it.videoCode,
                title = it.title,
                coverUrl = it.coverUrl,
                watchDate = it.watchDate,
                positionMs = it.lastPosition,
                durationMs = it.totalDuration,
                path = null,
                subtitle = null,
                completed = it.totalDuration > 0 &&
                        it.lastPosition >= it.totalDuration - 3_000L,
            )
        }).sortedByDescending { it.watchDate }
    }.combine(LocalWatchStore.entries) { base, local ->
        mergeHistory(base, local)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun WatchHistoryEntity.toItem() = UnifiedHistoryItem(
        site = SiteType.HANIME,
        code = videoCode,
        title = title,
        coverUrl = coverUrl,
        watchDate = watchDate,
        positionMs = progress,
        durationMs = 0L,
        path = null,
        subtitle = null,
        completed = watched,
    )

    fun delete(item: UnifiedHistoryItem) {
        LocalWatchStore.forget(SiteCheckIn.dishCode(item.site, item.code))
        viewModelScope.launch {
            runCatching {
                when (item.site) {
                    SiteType.HANIME, SiteType.JAVCHU -> {
                        val entity = DatabaseRepo.WatchHistory.findBy(item.code)
                        if (entity != null) DatabaseRepo.WatchHistory.delete(entity)
                    }

                    SiteType.MISSAV -> MissAvHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.HENTAIMAMA -> HentaiMamaHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.ONAIR -> OnAirHistoryRepo.deleteByVideoId(item.code)
                    SiteType.STRAWBERRY -> StrawberryHistoryRepo.deleteByVideoId(item.code)
                    SiteType.FAPFOLDER -> item.code.toLongOrNull()?.let {
                        FapFolderHistoryRepo.deleteByPostId(it)
                    }
                    SiteType.SUPJAV -> SupJavHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.ROUVIDEO -> RouVideoHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.XHAMSTER -> XHamsterHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.BUONDUA -> BuonduaHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.COSPLAYTELE -> CosplayteleHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.COSER -> CoserHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.FULLVIDEOS -> FullVideosHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.FUCKINGSESSION -> FuckingSessionHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.WUMAOBI -> WumaobiHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.REDGIFS -> Unit
                    SiteType.XXXTUBE -> XXXTubeHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.COOMER -> CoomerHistoryRepo.deleteByVideoCode(item.code)
                    SiteType.LEAKGALLERY -> LeakGalleryHistoryRepo.deleteByVideoCode(item.code)
                }
            }
        }
    }

    fun clear(site: SiteType?) {
        LocalWatchStore.clear(site)
        viewModelScope.launch {
            runCatching {
                if (site == null || site == SiteType.HANIME) DatabaseRepo.WatchHistory.deleteAll()
                if (site == null || site == SiteType.MISSAV) MissAvHistoryRepo.deleteAll()
                if (site == null || site == SiteType.HENTAIMAMA) HentaiMamaHistoryRepo.deleteAll()
                if (site == null || site == SiteType.ONAIR) OnAirHistoryRepo.deleteAll()
                if (site == null || site == SiteType.STRAWBERRY) StrawberryHistoryRepo.deleteAll()
                if (site == null || site == SiteType.FAPFOLDER) FapFolderHistoryRepo.deleteAll()
                if (site == null || site == SiteType.SUPJAV) SupJavHistoryRepo.deleteAll()
                if (site == null || site == SiteType.ROUVIDEO) RouVideoHistoryRepo.deleteAll()
                if (site == null || site == SiteType.XHAMSTER) XHamsterHistoryRepo.deleteAll()
                if (site == null || site == SiteType.BUONDUA) BuonduaHistoryRepo.deleteAll()
                if (site == null || site == SiteType.COSPLAYTELE) CosplayteleHistoryRepo.deleteAll()
                if (site == null || site == SiteType.COSER) CoserHistoryRepo.deleteAll()
                if (site == null || site == SiteType.FULLVIDEOS) FullVideosHistoryRepo.deleteAll()
                if (site == null || site == SiteType.FUCKINGSESSION) FuckingSessionHistoryRepo.deleteAll()
                if (site == null || site == SiteType.WUMAOBI) WumaobiHistoryRepo.deleteAll()
                if (site == null || site == SiteType.XXXTUBE) XXXTubeHistoryRepo.deleteAll()
                if (site == null || site == SiteType.COOMER) CoomerHistoryRepo.deleteAll()
                if (site == null || site == SiteType.LEAKGALLERY) LeakGalleryHistoryRepo.deleteAll()
            }
        }
    }
}

private fun mergeHistory(
    base: List<UnifiedHistoryItem>,
    local: Map<String, LocalWatchEntry>,
): List<UnifiedHistoryItem> {
    val byKey = LinkedHashMap<String, UnifiedHistoryItem>()
    for (item in base) {
        val old = byKey[item.key]
        if (old == null || item.watchDate > old.watchDate) byKey[item.key] = item
    }
    for (e in local.values) {
        val key = "${e.site.name}:${e.rawCode}"
        val cur = byKey[key]
        if (cur == null) {
            byKey[key] = UnifiedHistoryItem(
                site = e.site,
                code = e.rawCode,
                title = e.title,
                coverUrl = e.coverUrl,
                watchDate = e.watchedAt,
                positionMs = e.positionMs,
                durationMs = e.durationMs,
                path = null,
                subtitle = null,
                completed = e.completed,
            )
        } else if (e.watchedAt > cur.watchDate) {
            byKey[key] = cur.copy(
                watchDate = e.watchedAt,
                positionMs = e.positionMs,
                durationMs = if (e.durationMs > 0) e.durationMs else cur.durationMs,
                completed = e.completed,
                title = cur.title.ifBlank { e.title },
                coverUrl = cur.coverUrl.ifBlank { e.coverUrl },
            )
        }
    }
    return byKey.values.sortedByDescending { it.watchDate }
}

private val filterSites = listOf(
    SiteType.HANIME,
    SiteType.MISSAV,
    SiteType.HENTAIMAMA,
    SiteType.ONAIR,
    SiteType.STRAWBERRY,
    SiteType.FAPFOLDER,
    SiteType.SUPJAV,
    SiteType.ROUVIDEO,
    SiteType.XHAMSTER,
    SiteType.BUONDUA,
    SiteType.COSPLAYTELE,
    SiteType.COSER,
    SiteType.FULLVIDEOS,
    SiteType.FUCKINGSESSION,
    SiteType.WUMAOBI,
    SiteType.XXXTUBE,
    SiteType.COOMER,
    SiteType.LEAKGALLERY,
)

private fun siteBadge(site: SiteType): String = when (site) {
    SiteType.HANIME, SiteType.JAVCHU -> "Hanime / Javchu"
    else -> CrossSiteNavigator.siteLabel(site)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedHistoryScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    viewModel: UnifiedHistoryViewModel = viewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<SiteType?>(null) }
    var itemToDelete by remember { mutableStateOf<UnifiedHistoryItem?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val visible = remember(items, filter) {
        items?.filter { filter == null || it.site == filter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.site_watch_history_all_sites)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        enabled = !visible.isNullOrEmpty(),
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.site_clear_history))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text(stringResource(R.string.site_all)) },
                )
                filterSites.forEach { site ->
                    FilterChip(
                        selected = filter == site,
                        onClick = { filter = if (filter == site) null else site },
                        label = { Text(siteBadge(site)) },
                    )
                }
            }

            when {
                visible == null -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                visible.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.site_nothing_watched_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visible, key = { it.key }) { item ->
                        UnifiedHistoryCard(
                            item = item,
                            onClick = { activity.requestCrossSiteOpen(item.toRequest()) },
                            onDelete = { itemToDelete = item },
                        )
                    }
                }
            }
        }
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.site_remove_from_history)) },
            text = { Text(item.title.ifBlank { item.code }) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item)
                    itemToDelete = null
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.site_clear_history_2)) },
            text = {
                Text(
                    if (filter == null) stringResource(R.string.site_this_removes_the_watch_history_of)
                    else stringResource(R.string.site_this_removes_the_watch_history_of_2, siteBadge(filter!!))
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear(filter)
                    showClearConfirm = false
                }) { Text(stringResource(R.string.site_clear)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun UnifiedHistoryCard(
    item: UnifiedHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val dateText = remember(item.watchDate) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(item.watchDate))
    }

    val loader = remember(item.site) { SiteImageLoaders.forSite(item.site, context) }
    HistoryGridCard(
        title = item.title.ifBlank { item.code },
        coverUrl = item.coverUrl.ifBlank { "" },
        onClick = onClick,
        onDelete = onDelete,
        siteBadge = siteBadge(item.site),
        subtitle = item.subtitle,
        meta = dateText,
        progress = item.progress?.let { if (item.completed) 1f else it },
        tags = rememberHistoryTags(
            site = item.site,
            rawCode = item.code,
            positionMs = item.positionMs,
            durationMs = item.durationMs,
            completed = item.completed,
            watchDate = item.watchDate,
        ),
        imageLoader = loader,
    )
}
