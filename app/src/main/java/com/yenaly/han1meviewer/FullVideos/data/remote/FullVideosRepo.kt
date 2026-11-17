package com.yenaly.han1meviewer.FullVideos.data.remote

import com.yenaly.han1meviewer.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosDetail
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosEntityPage
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosListPage
import com.yenaly.han1meviewer.FullVideos.data.parser.FullVideosParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object FullVideosRepo {

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = FullVideosNetwork.service.get(url)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
        response.body()?.string().orEmpty()
    }

    suspend fun listing(path: String, page: Int): Result<FullVideosListPage> = runCatching {
        withContext(Dispatchers.Default) {
            FullVideosParser.parseListPage(fetchHtml(FullVideosConstants.pagedUrl(path, page)))
        }
    }

    suspend fun search(query: String, page: Int): Result<FullVideosListPage> = runCatching {
        withContext(Dispatchers.Default) {
            FullVideosParser.parseListPage(fetchHtml(FullVideosConstants.searchUrl(query, page)))
        }
    }

    suspend fun entities(
        kind: FullVideosConstants.EntityKind,
        sortPath: String,
        page: Int,
    ): Result<FullVideosEntityPage> = runCatching {
        withContext(Dispatchers.Default) {
            val html = fetchHtml(FullVideosConstants.entityListUrl(kind, sortPath, page))
            when (kind) {
                FullVideosConstants.EntityKind.CATEGORIES -> FullVideosParser.parseCategories(html)
                FullVideosConstants.EntityKind.MODELS -> FullVideosParser.parseModels(html)
                FullVideosConstants.EntityKind.SITES -> FullVideosParser.parseSites(html)
            }
        }
    }

    suspend fun detail(code: String): Result<FullVideosDetail> = runCatching {
        withContext(Dispatchers.Default) {
            val html = fetchHtml(FullVideosConstants.videoUrl(code))
            FullVideosParser.parseDetail(html, code)
                ?: throw IOException("Could not read video page")
        }
    }
}
