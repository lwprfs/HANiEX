package com.yenaly.han1meviewer.SitesHub.Coomer.data.remote

import com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerConstants
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerDetail
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerListPage
import com.yenaly.han1meviewer.SitesHub.Coomer.data.parser.CoomerParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object CoomerRepo {

    private val detailCache = object : LinkedHashMap<String, CoomerDetail>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CoomerDetail>?) = size > 12
    }

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = CoomerNetwork.service.get(url)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
        response.body()?.string().orEmpty()
    }

    suspend fun page(url: String): Result<CoomerListPage> = runCatching {
        val html = fetchHtml(url)
        withContext(Dispatchers.Default) { CoomerParser.parseListPage(html, url) }
    }

    suspend fun search(query: String, posts: Boolean): Result<CoomerListPage> =
        page(CoomerConstants.searchUrl(query, posts, 1))

    suspend fun detail(slug: String): Result<CoomerDetail> = runCatching {
        val url = CoomerConstants.postUrl(slug)
        val html = fetchHtml(url)
        withContext(Dispatchers.Default) {
            CoomerParser.parseDetail(html, slug, url) ?: throw IOException("Could not read post")
        }
    }.onSuccess { d -> synchronized(detailCache) { detailCache[slug] = d } }

    suspend fun detailCached(slug: String): Result<CoomerDetail> {
        val hit = synchronized(detailCache) { detailCache[slug] }
        return if (hit != null) Result.success(hit) else detail(slug)
    }
}
