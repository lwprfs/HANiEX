package com.yenaly.han1meviewer.SitesHub.Coser.data.remote

import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserConstants
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserDetail
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserListPage
import com.yenaly.han1meviewer.SitesHub.Coser.data.parser.CoserParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object CoserRepo {

    private val detailCache = object : LinkedHashMap<String, CoserDetail>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CoserDetail>?) = size > 12
    }

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = CoserNetwork.service.get(url)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
        response.body()?.string().orEmpty()
    }

    suspend fun page(url: String, collectTerms: Boolean): Result<CoserListPage> = runCatching {
        val html = fetchHtml(url)
        withContext(Dispatchers.Default) { CoserParser.parseListPage(html, url, collectTerms) }
    }

    suspend fun search(query: String, filters: CoserConstants.Filters = CoserConstants.Filters()): Result<CoserListPage> =
        page(CoserConstants.searchUrl(query, 1, filters), collectTerms = false)

    suspend fun detail(slug: String): Result<CoserDetail> = runCatching {
        val url = CoserConstants.postUrl(slug)
        val html = fetchHtml(url)
        withContext(Dispatchers.Default) {
            CoserParser.parseDetail(html, slug, url) ?: throw IOException("Could not read post")
        }
    }.onSuccess { d -> synchronized(detailCache) { detailCache[slug] = d } }

    suspend fun detailCached(slug: String): Result<CoserDetail> {
        val hit = synchronized(detailCache) { detailCache[slug] }
        return if (hit != null) Result.success(hit) else detail(slug)
    }
}
