package com.yenaly.han1meviewer.FuckingSession.data.remote

import com.yenaly.han1meviewer.FuckingSession.common.FuckingSessionConstants
import com.yenaly.han1meviewer.FuckingSession.data.model.FuckingSessionDetail
import com.yenaly.han1meviewer.FuckingSession.data.model.FuckingSessionListPage
import com.yenaly.han1meviewer.FuckingSession.data.parser.FuckingSessionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object FuckingSessionRepo {

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = FuckingSessionNetwork.service.get(url)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
        response.body()?.string().orEmpty()
    }

    suspend fun listing(path: String, page: Int): Result<FuckingSessionListPage> = runCatching {
        withContext(Dispatchers.Default) {
            FuckingSessionParser.parseListPage(fetchHtml(FuckingSessionConstants.pagedUrl(path, page)))
        }
    }

    suspend fun search(query: String, page: Int): Result<FuckingSessionListPage> = runCatching {
        withContext(Dispatchers.Default) {
            FuckingSessionParser.parseListPage(fetchHtml(FuckingSessionConstants.searchUrl(query, page)))
        }
    }

    /** Loads the post, then resolves its embed(s) into direct stream URLs. */
    suspend fun detail(code: String): Result<FuckingSessionDetail> = runCatching {
        val parsed = withContext(Dispatchers.Default) {
            val html = fetchHtml(FuckingSessionConstants.videoUrl(code))
            FuckingSessionParser.parseDetail(html, code)
                ?: throw IOException("Could not read video page")
        }
        val sources = withContext(Dispatchers.IO) {
            FuckingSessionStreamResolver.resolve(parsed.embeds, parsed.pageUrl)
        }
        sources.firstOrNull()?.referer?.takeIf { it.isNotBlank() }?.let {
            FuckingSessionNetwork.rememberStreamReferer(code, it)
        }
        parsed.copy(sources = sources)
    }
}
