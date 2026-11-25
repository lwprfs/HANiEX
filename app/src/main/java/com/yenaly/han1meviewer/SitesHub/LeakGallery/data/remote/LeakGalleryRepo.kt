package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote

import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryDetail
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryListPage
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.parser.LeakGalleryParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object LeakGalleryRepo {

    @Volatile private var token: String? = null

    private val detailCache = object : LinkedHashMap<String, LeakGalleryDetail>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LeakGalleryDetail>?) = size > 12
    }

    fun clearToken() {
        token = null
    }

    private fun pageHeaders(builder: Request.Builder) = builder
        .header("Referer", "${LeakGalleryConstants.BASE_URL}/")
        .header("Origin", LeakGalleryConstants.BASE_URL)

    /** The API token is embedded in the home page's transfer state; fetch and cache it. */
    private fun loadToken(force: Boolean): String {
        if (!force) token?.let { return it }
        val request = Request.Builder()
            .url(LeakGalleryConstants.BASE_URL + "/")
            .header("Accept", "text/html,application/xhtml+xml")
            .build()
        val html = LeakGalleryNetwork.sharedOkHttpClient.newCall(request).execute().use {
            if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
            it.body?.string().orEmpty()
        }
        val fresh = LeakGalleryParser.appToken(html) ?: throw IOException("API token not found")
        token = fresh
        return fresh
    }

    private fun buildRequest(url: String, token: String, body: String?): Request {
        val builder = pageHeaders(Request.Builder().url(url))
            .header("Accept", "application/json, text/plain, */*")
            .header("x-app-token", token)
        if (body != null) {
            builder.post(body.toRequestBody("application/json".toMediaType()))
        }
        return builder.build()
    }

    private suspend fun api(url: String, body: String? = null): String = withContext(Dispatchers.IO) {
        val client = LeakGalleryNetwork.sharedOkHttpClient
        var response = client.newCall(buildRequest(url, loadToken(false), body)).execute()
        if (response.code == 401 || response.code == 403) {
            response.close()
            response = client.newCall(buildRequest(url, loadToken(true), body)).execute()
        }
        response.use {
            if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
            it.body?.string().orEmpty()
        }
    }

    /** [page] is 1-based. Profile lists show media only; "new models" shows creators. */
    suspend fun page(path: String, page: Int): Result<LeakGalleryListPage> = runCatching {
        val json = api(LeakGalleryConstants.apiUrl(path, page))
        val base = path.substringBefore('?').trim('/')
        val creators = base.startsWith("popular/newest-profiles") || base.startsWith("popular/profile")
        withContext(Dispatchers.Default) {
            LeakGalleryParser.parseList(json, wantMedia = !creators, wantTerms = creators)
        }
    }

    suspend fun search(query: String, creators: Boolean, page: Int): Result<LeakGalleryListPage> =
        runCatching {
            val json = if (creators) {
                api(
                    LeakGalleryConstants.API_URL + "/search",
                    JSONObject().put("search", query).toString(),
                )
            } else {
                api(
                    LeakGalleryConstants.API_URL + "/search/advanced",
                    JSONObject()
                        .put("titleSearch", query)
                        .put("sort", "MostViewed")
                        .put("type", "all")
                        .put("page", page)
                        .toString(),
                )
            }
            withContext(Dispatchers.Default) {
                val result = LeakGalleryParser.parseList(json, wantMedia = !creators, wantTerms = creators)
                // The creator search is a single response; later pages would repeat it.
                if (creators && page > 1) LeakGalleryListPage(emptyList(), emptyList()) else result
            }
        }

    suspend fun detail(slug: String): Result<LeakGalleryDetail> = runCatching {
        val json = api("${LeakGalleryConstants.API_URL}/media/$slug")
        withContext(Dispatchers.Default) {
            LeakGalleryParser.parseDetail(json, slug) ?: throw IOException("Could not read media")
        }
    }.onSuccess { d -> synchronized(detailCache) { detailCache[slug] = d } }

    suspend fun detailCached(slug: String): Result<LeakGalleryDetail> {
        val hit = synchronized(detailCache) { detailCache[slug] }
        return if (hit != null) Result.success(hit) else detail(slug)
    }
}
