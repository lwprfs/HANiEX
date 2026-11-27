package com.yenaly.han1meviewer.util.offline

import android.util.Log
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.Coomer.data.repository.CoomerHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Coser.data.repository.CoserHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.repository.CosplayteleHistoryRepo
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.local.FapFolderHistoryRepo
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.repository.FuckingSessionHistoryRepo
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.repository.FullVideosHistoryRepo
import com.yenaly.han1meviewer.SitesHub.HentaiMama.data.local.HentaiMamaHistoryRepo
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.repository.LeakGalleryHistoryRepo
import com.yenaly.han1meviewer.SitesHub.MissAV.data.repository.MissAvHistoryRepo
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirHistoryRepo
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.repository.RouVideoHistoryRepo
import com.yenaly.han1meviewer.SitesHub.SupJav.data.repository.SupJavHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.repository.WumaobiHistoryRepo
import com.yenaly.han1meviewer.SitesHub.XHamster.data.repository.XHamsterHistoryRepo
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.repository.XXXTubeHistoryRepo
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.util.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalWatchRecorder {

    private const val TAG = "LocalWatchRecorder"
    private const val COMPLETE_GRACE_MS = 3_000L

    suspend fun record(
        dishCode: String,
        title: String,
        coverUrl: String,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
    ) = withContext(Dispatchers.IO) {
        if (dishCode.isBlank() || dishCode == "-1") return@withContext
        if (positionMs <= 0L && durationMs <= 0L) return@withContext

        val completed = durationMs > 0 && positionMs >= durationMs - COMPLETE_GRACE_MS
        val entry = LocalWatchEntry(
            code = dishCode,
            title = title,
            coverUrl = coverUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            watchedAt = System.currentTimeMillis(),
            completed = completed,
        )
        LocalWatchStore.record(entry)
        val saved = LocalWatchStore.get(dishCode) ?: entry
        runCatching { writeSiteHistory(saved, isPlaying) }
            .onFailure { Log.w(TAG, "site history write failed for $dishCode", it) }
    }

    private suspend fun writeSiteHistory(e: LocalWatchEntry, isPlaying: Boolean) {
        val raw = e.rawCode
        val pos = e.positionMs
        val total = e.durationMs
        val done = e.completed
        when (e.site) {
            SiteType.HANIME, SiteType.JAVCHU -> {
                val existing = DatabaseRepo.WatchHistory.findBy(e.code)
                val now = System.currentTimeMillis()
                DatabaseRepo.WatchHistory.insert(
                    WatchHistoryEntity(
                        coverUrl = e.coverUrl.ifBlank { existing?.coverUrl.orEmpty() },
                        title = e.title.ifBlank { existing?.title.orEmpty() },
                        releaseDate = existing?.releaseDate ?: now,
                        watchDate = now,
                        videoCode = e.code,
                        progress = pos,
                        watched = done || existing?.watched == true,
                    )
                )
                DatabaseRepo.WatchHistory.updateProgress(e.code, pos)
                if (done) DatabaseRepo.WatchHistory.markWatched(e.code)
            }

            SiteType.ONAIR -> {
                OnAirHistoryRepo.init(application)
                OnAirHistoryRepo.saveProgress(
                    videoId = raw, title = e.title, coverUrl = e.coverUrl, uploaderName = null,
                    position = pos, duration = total, isPlaying = isPlaying,
                )
            }

            SiteType.FAPFOLDER -> {
                val postId = raw.toLongOrNull() ?: return
                FapFolderHistoryRepo.init(application)
                FapFolderHistoryRepo.saveProgress(
                    postId = postId, title = e.title, coverUrl = e.coverUrl, isVideo = true,
                    positionMs = pos, durationMs = total, isPlaying = isPlaying, completed = done,
                )
            }

            SiteType.HENTAIMAMA -> {
                HentaiMamaHistoryRepo.init(application)
                HentaiMamaHistoryRepo.saveProgress(
                    videoCode = raw, title = e.title, coverUrl = e.coverUrl,
                    episodeUrl = "", episodeNumber = 0f, episodeTitle = "",
                    position = pos, duration = total, isPlaying = isPlaying, completed = done,
                )
            }

            SiteType.MISSAV -> {
                MissAvHistoryRepo.init(application)
                MissAvHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.SUPJAV -> {
                SupJavHistoryRepo.init(application)
                SupJavHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.ROUVIDEO -> {
                RouVideoHistoryRepo.init(application)
                RouVideoHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.XHAMSTER -> {
                XHamsterHistoryRepo.init(application)
                XHamsterHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.COSPLAYTELE -> {
                CosplayteleHistoryRepo.init(application)
                CosplayteleHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.COSER -> {
                CoserHistoryRepo.init(application)
                CoserHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.FULLVIDEOS -> {
                FullVideosHistoryRepo.init(application)
                FullVideosHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.FUCKINGSESSION -> {
                FuckingSessionHistoryRepo.init(application)
                FuckingSessionHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.WUMAOBI -> {
                WumaobiHistoryRepo.init(application)
                WumaobiHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.XXXTUBE -> {
                XXXTubeHistoryRepo.init(application)
                XXXTubeHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.COOMER -> {
                CoomerHistoryRepo.init(application)
                CoomerHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.LEAKGALLERY -> {
                LeakGalleryHistoryRepo.init(application)
                LeakGalleryHistoryRepo.upsertWatchProgress(raw, e.title, e.coverUrl, pos, total, isPlaying, done)
            }

            SiteType.BUONDUA, SiteType.REDGIFS -> Unit
        }
    }
}
