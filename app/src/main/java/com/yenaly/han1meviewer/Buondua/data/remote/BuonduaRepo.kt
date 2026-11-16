package com.yenaly.han1meviewer.Buondua.data.remote

import com.yenaly.han1meviewer.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaAlbumPage
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaListPage
import com.yenaly.han1meviewer.Buondua.data.parser.BuonduaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

object BuonduaRepo {

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = BuonduaNetwork.service.get(url)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
        response.body()?.string().orEmpty()
    }

    suspend fun list(url: String): Result<BuonduaListPage> = runCatching {
        val html = fetchHtml(url)
        withContext(Dispatchers.Default) { BuonduaParser.parseListPage(html) }
    }

    suspend fun category(path: String): Result<BuonduaListPage> =
        list(BuonduaConstants.categoryUrl(path))

    suspend fun search(query: String): Result<BuonduaListPage> =
        list(BuonduaConstants.searchUrl(query))

    suspend fun album(code: String, page: Int): Result<BuonduaAlbumPage> = runCatching {
        val html = fetchHtml(BuonduaConstants.albumUrl(code, page))
        withContext(Dispatchers.Default) {
            BuonduaParser.parseAlbum(html, code, page)
                ?: throw IOException("Could not read album page")
        }
    }

    suspend fun allPhotoUrls(
        code: String,
        loaded: List<String>,
        totalPages: Int,
        expectedCount: Int? = null,
    ): List<String> {
        val urls = LinkedHashSet<String>(loaded)
        var page = 2
        while (page <= MAX_PAGES) {
            val probing = page > totalPages
            // Past the advertised page count only keep going while photos are still missing.
            if (probing && (expectedCount == null || urls.size >= expectedCount)) break
            val next = album(code, page).getOrNull()
                ?: album(code, page).getOrNull() // one retry
            if (next == null) {
                if (probing) break
            } else {
                val before = urls.size
                urls += next.photos.map { it.url }
                if (probing && urls.size == before) break
            }
            page++
        }
        return urls.toList()
    }

    const val MAX_PAGES = 200

    suspend fun allVideos(code: String, first: BuonduaAlbumPage) =
        buildList {
            addAll(first.videos)
            for (p in 2..first.totalPages) {
                album(code, p).getOrNull()?.let { addAll(it.videos) }
            }
        }.distinctBy { it.url }

    suspend fun isInlinePlayable(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).get()
                .header("Range", "bytes=0-1")
                .apply { BuonduaNetwork.mediaHeaders().forEach { (k, v) -> header(k, v) } }
                .build()
            BuonduaNetwork.streamClient.newCall(request).execute().use { r ->
                if (!(r.code == 200 || r.code == 206)) return@use false
                val type = r.header("Content-Type").orEmpty().lowercase()
                val disposition = r.header("Content-Disposition").orEmpty().lowercase()
                val videoLike = type.startsWith("video/") ||
                        type.contains("mpegurl") || type.contains("octet-stream")
                videoLike && !disposition.contains("attachment")
            }
        }.getOrDefault(false)
    }
}
