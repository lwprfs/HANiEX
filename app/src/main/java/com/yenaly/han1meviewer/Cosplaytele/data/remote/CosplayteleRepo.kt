package com.yenaly.han1meviewer.Cosplaytele.data.remote

import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleCard
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleDetail
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleListPage
import com.yenaly.han1meviewer.Cosplaytele.data.parser.CosplayteleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

object CosplayteleRepo {

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = CosplayteleNetwork.service.get(url)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code()}")
        response.body()?.string().orEmpty()
    }

    /** Any listing page: home, archive, 24 hours, top search, best cosplayer, categories ... */
    suspend fun page(url: String, collectTerms: Boolean): Result<CosplayteleListPage> = runCatching {
        val html = fetchHtml(url)
        withContext(Dispatchers.Default) { CosplayteleParser.parseListPage(html, collectTerms) }
    }

    suspend fun search(query: String): Result<CosplayteleListPage> =
        page(CosplayteleConstants.searchUrl(query), collectTerms = false)

    suspend fun detail(slug: String): Result<CosplayteleDetail> = runCatching {
        val html = fetchHtml(CosplayteleConstants.postUrl(slug))
        withContext(Dispatchers.Default) {
            CosplayteleParser.parseDetail(html, slug) ?: throw IOException("Could not read post")
        }
    }

    /** The site's own live search box (Ajax Search Lite), used for suggestions while typing. */
    suspend fun liveSearch(query: String): Result<List<CosplayteleCard>> = runCatching {
        val body = FormBody.Builder()
            .add("action", "ajaxsearchlite_search")
            .add("aslp", query)
            .add("asid", CosplayteleConstants.AJAX_SEARCH_ID)
            .add("options", "customset[]=post&asl_gen[]=title&qtranslate_lang=0&filters_initial=1&filters_changed=0")
            .add("asl_req_json", "1")
            .build()
        val request = Request.Builder()
            .url("${CosplayteleConstants.BASE_URL}/wp-admin/admin-ajax.php")
            .header("Accept", "text/html")
            .header("Origin", CosplayteleConstants.BASE_URL)
            .header("Referer", "${CosplayteleConstants.BASE_URL}/")
            .post(body)
            .build()
        withContext(Dispatchers.IO) {
            CosplayteleNetwork.sharedOkHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                CosplayteleParser.parseAjaxSearch(resp.body.string())
            }
        }
    }
}
