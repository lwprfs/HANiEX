package com.yenaly.han1meviewer.logic

import android.content.Context
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.dao.HistoryDatabase
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.CheckInType
import com.yenaly.han1meviewer.logic.entity.SideDishEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.local.FapFolderDatabase
import com.yenaly.han1meviewer.SitesHub.HentaiMama.data.local.HentaiMamaDatabase
import com.yenaly.han1meviewer.SitesHub.MissAV.data.local.MissAvDatabase
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object SiteCheckIn {

    const val SEP = "\u001E"

    private fun prefixOf(site: SiteType): String? = when (site) {
        SiteType.HANIME, SiteType.JAVCHU -> null
        SiteType.MISSAV -> "missav"
        SiteType.HENTAIMAMA -> "hentaimama"
        SiteType.ONAIR -> "onair"
        SiteType.FAPFOLDER -> "fapfolder"
        SiteType.SUPJAV -> "supjav"
        SiteType.ROUVIDEO -> "rouvideo"
        SiteType.XHAMSTER -> "xhamster"
        SiteType.BUONDUA -> "buondua"
        SiteType.COSPLAYTELE -> "cosplaytele"
        SiteType.COSER -> "coser"
        SiteType.FULLVIDEOS -> "fullvideos"
        SiteType.FUCKINGSESSION -> "fuckingsession"
        SiteType.REDGIFS -> "redgifs"
        SiteType.XXXTUBE -> "xxxtube"
        SiteType.COOMER -> "coomer"
        SiteType.LEAKGALLERY -> "leakgallery"
        SiteType.WUMAOBI -> "wumaobi"
    }

    fun dishCode(site: SiteType, code: String): String {
        val prefix = prefixOf(site) ?: return code
        return "$prefix:$code"
    }

    fun siteOfDishCode(dishCode: String): SiteType? {
        val prefix = dishCode.substringBefore(":", "")
        if (prefix.isEmpty() || prefix == dishCode) return null
        return when (prefix) {
            "missav" -> SiteType.MISSAV
            "hentaimama" -> SiteType.HENTAIMAMA
            "onair" -> SiteType.ONAIR
            "fapfolder" -> SiteType.FAPFOLDER
            "supjav" -> SiteType.SUPJAV
            "rouvideo" -> SiteType.ROUVIDEO
            "xhamster" -> SiteType.XHAMSTER
            "buondua" -> SiteType.BUONDUA
            "cosplaytele" -> SiteType.COSPLAYTELE
            "coser" -> SiteType.COSER
            "fullvideos" -> SiteType.FULLVIDEOS
            "fuckingsession" -> SiteType.FUCKINGSESSION
            "redgifs" -> SiteType.REDGIFS
            "xxxtube" -> SiteType.XXXTUBE
            "coomer" -> SiteType.COOMER
            "leakgallery" -> SiteType.LEAKGALLERY
            "wumaobi" -> SiteType.WUMAOBI
            else -> null
        }
    }

    fun rawCodeOfDishCode(dishCode: String): String =
        if (siteOfDishCode(dishCode) != null) dishCode.substringAfter(":") else dishCode

    suspend fun todayCount(context: Context): Int {
        return CheckInRecordDatabase.getDatabase(context).checkInDao()
            .getCountByDate(LocalDate.now().toString())
    }

    suspend fun quickCheckIn(
        context: Context,
        site: SiteType,
        videoCode: String,
        title: String,
        coverUrl: String?,
        feeling: String,
    ) {
        val db = CheckInRecordDatabase.getDatabase(context)
        val code = dishCode(site, videoCode)
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val record = CheckInRecordEntity(
            date = LocalDate.now().toString(),
            time = time,
            type = CheckInType.MASTURBATION.storeName,
            sideDishes = "$title$SEP$code",
            feeling = feeling,
        )
        db.checkInDao().insert(record)
        if (!coverUrl.isNullOrBlank()) {
            db.sideDishDao().insertAll(
                listOf(SideDishEntity(videoCode = code, title = title, coverUrl = coverUrl))
            )
        }
    }

    suspend fun recentWatchedAllSites(context: Context, limit: Int): List<WatchHistoryEntity> {
        val result = mutableListOf<WatchHistoryEntity>()
        runCatching {
            result += HistoryDatabase.instance.watchHistory.getRecentWatches(limit)
        }
        runCatching {
            MissAvDatabase.getInstance(context).missAvHistoryDao().getPage(limit, 0).forEach {
                result += WatchHistoryEntity(
                    coverUrl = it.coverUrl,
                    title = it.title,
                    releaseDate = 0L,
                    watchDate = it.watchDate,
                    videoCode = dishCode(SiteType.MISSAV, it.videoCode),
                )
            }
        }
        runCatching {
            HentaiMamaDatabase.getInstance(context).historyDao().getPage(limit, 0).forEach {
                result += WatchHistoryEntity(
                    coverUrl = it.coverUrl,
                    title = if (it.lastEpisodeTitle.isBlank()) it.title
                    else "${it.title} - ${it.lastEpisodeTitle}",
                    releaseDate = 0L,
                    watchDate = it.watchDate,
                    videoCode = dishCode(SiteType.HENTAIMAMA, it.videoCode),
                )
            }
        }
        runCatching {
            OnAirDatabase.getInstance(context).onAirHistoryDao().getPage(limit, 0).forEach {
                result += WatchHistoryEntity(
                    coverUrl = it.coverUrl,
                    title = it.title,
                    releaseDate = 0L,
                    watchDate = it.watchDate,
                    videoCode = dishCode(SiteType.ONAIR, it.videoId),
                )
            }
        }
        runCatching {
            FapFolderDatabase.getInstance(context).fapFolderHistoryDao().getPage(limit, 0)
                .forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.FAPFOLDER, it.postId.toString()),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.SupJav.data.local.SupJavDatabase.getInstance(context)
                .supJavHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.SUPJAV, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.RouVideo.data.local.RouVideoDatabase.getInstance(context)
                .rouVideoHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.ROUVIDEO, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.XHamster.data.local.XHamsterDatabase.getInstance(context)
                .xHamsterHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.XHAMSTER, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.Buondua.data.local.BuonduaDatabase.getInstance(context)
                .buonduaHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.BUONDUA, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.local.CosplayteleDatabase.getInstance(context)
                .cosplayteleHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.COSPLAYTELE, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.Coomer.data.local.CoomerDatabase.getInstance(context)
                .coomerHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.COOMER, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.LeakGallery.data.local.LeakGalleryDatabase.getInstance(context)
                .leakgalleryHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.LEAKGALLERY, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.Coser.data.local.CoserDatabase.getInstance(context)
                .coserHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.COSER, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.FullVideos.data.local.FullVideosDatabase.getInstance(context)
                .fullVideosHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.FULLVIDEOS, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.FuckingSession.data.local.FuckingSessionDatabase.getInstance(context)
                .fuckingSessionHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.FUCKINGSESSION, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.Wumaobi.data.local.WumaobiDatabase.getInstance(context)
                .wumaobiHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.WUMAOBI, it.videoCode),
                    )
                }
        }
        runCatching {
            com.yenaly.han1meviewer.SitesHub.XXXTube.data.local.XXXTubeDatabase.getInstance(context)
                .xxxTubeHistoryDao().getPage(limit, 0).forEach {
                    result += WatchHistoryEntity(
                        coverUrl = it.coverUrl,
                        title = it.title,
                        releaseDate = 0L,
                        watchDate = it.watchDate,
                        videoCode = dishCode(SiteType.XXXTUBE, it.videoCode),
                    )
                }
        }
        return result.sortedByDescending { it.watchDate }.take(limit)
    }

    suspend fun rememberCover(context: Context, dishCode: String, title: String, coverUrl: String) {
        if (coverUrl.isBlank()) return
        CheckInRecordDatabase.getDatabase(context).sideDishDao()
            .insertAll(listOf(SideDishEntity(dishCode, title, coverUrl)))
    }
}
