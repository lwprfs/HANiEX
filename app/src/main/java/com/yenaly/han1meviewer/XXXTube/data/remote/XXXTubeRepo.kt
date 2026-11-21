package com.yenaly.han1meviewer.XXXTube.data.remote

import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeDetail
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeEntityPage
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeListPage
import com.yenaly.han1meviewer.XXXTube.data.parser.XXXTubeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object XXXTubeRepo {

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = XXXTubeNetwork.service.get(url)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
        response.body()?.string().orEmpty()
    }

    /** [url] is an absolute URL: either a listing's first page or a `nextUrl` from a previous page. */
    suspend fun videos(url: String): Result<XXXTubeListPage> = runCatching {
        withContext(Dispatchers.Default) { XXXTubeParser.parseListPage(fetchHtml(url)) }
    }

    suspend fun entities(url: String, kind: XXXTubeConstants.EntityKind): Result<XXXTubeEntityPage> =
        runCatching {
            withContext(Dispatchers.Default) { XXXTubeParser.parseEntityPage(fetchHtml(url), kind) }
        }

    suspend fun detail(code: String): Result<XXXTubeDetail> = runCatching {
        val parsed = withContext(Dispatchers.Default) {
            val html = fetchHtml(XXXTubeConstants.videoUrl(code))
            XXXTubeParser.parseDetail(html, code)
                ?: throw IOException("Could not read video page")
        }
        parsed.sources.firstOrNull()?.referer?.takeIf { it.isNotBlank() }?.let {
            XXXTubeNetwork.rememberStreamReferer(code, it)
        }
        parsed
    }
}
