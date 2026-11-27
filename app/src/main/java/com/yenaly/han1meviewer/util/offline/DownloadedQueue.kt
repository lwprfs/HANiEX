package com.yenaly.han1meviewer.util.offline

import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.DownloadSort
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download.downloadSortComparator
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The play order of downloaded videos. The downloads page sets the folder (group / site) and sort
 * it was opened with, the downloaded-video player then uses it for previous / next and for its
 * "related downloads" tab.
 */
object DownloadedQueue {

    sealed interface Scope {
        data object All : Scope
        data class Group(val id: Int) : Scope
        data class Site(val site: SiteType) : Scope
    }

    @Volatile
    var scope: Scope = Scope.All

    @Volatile
    var sort: DownloadSort = DownloadSort.DATE_DESC

    fun setContext(scope: Scope, sort: DownloadSort) {
        this.scope = scope
        this.sort = sort
    }

    fun siteOf(code: String): SiteType = SiteCheckIn.siteOfDishCode(code) ?: SiteType.HANIME

    /** Finished downloads in play order, one entry per video code. */
    suspend fun load(currentCode: String?): List<HanimeDownloadEntity> = withContext(Dispatchers.IO) {
        val all = runCatching {
            DownloadDatabase.instance.hanimeDownloadDao.getFinishedNewestFirst()
        }.getOrDefault(emptyList()).distinctBy { it.videoCode }
        val scoped = when (val s = scope) {
            Scope.All -> all
            is Scope.Group -> all.filter { it.groupId == s.id }
            is Scope.Site -> all.filter { siteOf(it.videoCode) == s.site }
        }
        val base = if (currentCode == null || scoped.any { it.videoCode == currentCode }) scoped else all
        base.map { VideoWithCategories(it, emptyList()) }
            .sortedWith(downloadSortComparator(sort))
            .map { it.video }
    }

    fun previousOf(list: List<HanimeDownloadEntity>, code: String): HanimeDownloadEntity? {
        val i = list.indexOfFirst { it.videoCode == code }
        return if (i > 0) list[i - 1] else null
    }

    fun nextOf(list: List<HanimeDownloadEntity>, code: String): HanimeDownloadEntity? {
        val i = list.indexOfFirst { it.videoCode == code }
        return if (i >= 0 && i < list.lastIndex) list[i + 1] else null
    }

    /**
     * 10-16 downloads around [code]: up to [perSide] before and the same amount after it. When one
     * side is short, the other side fills in (up to [maxTotal]).
     */
    fun relatedWindow(
        list: List<HanimeDownloadEntity>,
        code: String,
        perSide: Int = 8,
        maxTotal: Int = 16,
    ): List<HanimeDownloadEntity> {
        val i = list.indexOfFirst { it.videoCode == code }
        if (i < 0) return list.take(maxTotal)
        var before = minOf(perSide, i)
        var after = minOf(perSide, list.lastIndex - i)
        if (before < perSide) after = minOf(list.lastIndex - i, maxTotal - before)
        if (after < perSide) before = minOf(i, maxTotal - after)
        return list.subList(i - before, i) + list.subList(i + 1, i + 1 + after)
    }
}
