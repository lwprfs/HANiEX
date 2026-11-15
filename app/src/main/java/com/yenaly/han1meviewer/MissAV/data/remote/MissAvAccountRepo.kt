package com.yenaly.han1meviewer.MissAV.data.remote

import com.yenaly.han1meviewer.MissAV.data.model.MissAvActress
import com.yenaly.han1meviewer.MissAV.data.model.MissAvActressProfile
import com.yenaly.han1meviewer.MissAV.data.model.MissAvTweetsPage
import com.yenaly.han1meviewer.MissAV.data.model.MissAvListPage
import com.yenaly.han1meviewer.MissAV.data.model.MissAvPlaylist
import com.yenaly.han1meviewer.MissAV.data.model.MissAvPlaylistOption
import com.yenaly.han1meviewer.MissAV.data.model.MissAvSavedState
import com.yenaly.han1meviewer.MissAV.data.parser.MissAvAccountParser
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import okhttp3.ResponseBody

object MissAvAccountRepo {

    private val service get() = MissAvNetwork.missAvService
    private fun base() = Preferences.missAvBaseUrl.trimEnd('/')
    private fun host() = base().toHttpUrl().host
    private fun xsrf() = MissAvCookieJar.xsrfToken(host())
    private val emptyJson: RequestBody = "{}".toRequestBody("application/json".toMediaType())

    private fun url(path: String, page: Int? = null, build: HttpUrl.Builder.() -> Unit = {}): String {
        val b = base().toHttpUrl().newBuilder()
        path.trim('/').split('/').filter { it.isNotEmpty() }.forEach { b.addPathSegment(it) }
        if (page != null && page > 1) b.addQueryParameter("page", page.toString())
        return b.apply(build).build().toString()
    }

    private fun Response<ResponseBody>.checked(requireAuth: Boolean = false): String {
        val text = (if (isSuccessful) body() else errorBody())?.string().orEmpty()
        if (code() == 401 || code() == 419) {
            if (requireAuth) MissAvAuth.onSessionExpired()
            throw MissAvAuthException("Please log in again")
        }
        if (!isSuccessful) throw MissAvAuthException("Request failed (${code()})")
        return text
    }

    private suspend fun page(path: String, page: Int, requireAuth: Boolean): String = withContext(Dispatchers.IO) {
        val r = service.getPageUrl(url(path, page))
        r.checked(requireAuth)
    }

    suspend fun history(page: Int): MissAvListPage<HanimeInfo> =
        MissAvAccountParser.parseVideoList(page("/en/history", page, true))

    suspend fun saved(page: Int): MissAvListPage<HanimeInfo> =
        MissAvAccountParser.parseVideoList(page("/en/saved", page, true))

    suspend fun savedActresses(page: Int): MissAvListPage<MissAvActress> =
        MissAvAccountParser.parseActresses(page("/en/saved/actresses", page, true))

    suspend fun playlists(): List<MissAvPlaylist> =
        MissAvAccountParser.parsePlaylists(page("/en/playlists", 1, true))

    suspend fun playlistVideos(key: String, page: Int): MissAvListPage<HanimeInfo> =
        MissAvAccountParser.parseVideoList(page("/en/playlists/$key", page, true))

    suspend fun actresses(path: String, page: Int): MissAvListPage<MissAvActress> =
        MissAvAccountParser.parseActresses(page(path, page, false))

    data class ActressPage(
        val id: String?,
        val profile: MissAvActressProfile?,
        val videos: MissAvListPage<HanimeInfo>,
    )

    suspend fun actressPage(path: String, page: Int): ActressPage {
        val body = page(path, page, false)
        return ActressPage(
            id = MissAvAccountParser.extractActressId(body),
            profile = MissAvAccountParser.parseActressProfile(body),
            videos = MissAvAccountParser.parseVideoList(body),
        )
    }

    suspend fun actressTweets(actressId: String, cursor: String?): MissAvTweetsPage = withContext(Dispatchers.IO) {
        val u = base().toHttpUrl().newBuilder()
            .addPathSegments("en/api/actresses/$actressId/tweets")
            .apply { if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor) }
            .build().toString()
        MissAvAccountParser.parseTweets(service.getJson(u, xsrf = xsrf()).checked())
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        service.deleteJson("${base()}/api/history", emptyJson, xsrf = xsrf()).checked(true)
        Unit
    }

    private suspend fun viewState(kind: String, id: String): MissAvSavedState = withContext(Dispatchers.IO) {
        val text = service.getJson("${base()}/api/$kind/$id/view", xsrf = xsrf()).checked()
        MissAvAccountParser.parseViewState(text).also { MissAvAuth.onServerUserSeen(it.loggedIn) }
    }

    suspend fun itemState(itemKey: String) = viewState("items", itemKey)
    suspend fun actressState(id: String) = viewState("actresses", id)

    private suspend fun setSaved(kind: String, id: String, save: Boolean): Boolean = withContext(Dispatchers.IO) {
        val u = "${base()}/api/$kind/$id/save"
        if (save) {
            service.postJson(u, emptyJson, xsrf = xsrf()).checked(true)
            return@withContext viewState(kind, id).saved
        }
        runCatching { service.deleteJson(u, emptyJson, xsrf = xsrf()).checked(true) }
        if (!viewState(kind, id).saved) return@withContext false
        runCatching { service.postJson(u, emptyJson, xsrf = xsrf()).checked(true) }
        viewState(kind, id).saved
    }

    suspend fun setItemSaved(itemKey: String, save: Boolean) = setSaved("items", itemKey, save)
    suspend fun setActressSaved(id: String, save: Boolean) = setSaved("actresses", id, save)

    suspend fun playlistOptions(dvdId: String): List<MissAvPlaylistOption> = withContext(Dispatchers.IO) {
        MissAvAccountParser.parsePlaylistOptions(
            service.getJson("${base()}/api/playlists/$dvdId", xsrf = xsrf()).checked(true)
        )
    }

    private fun playlistBody(dvdId: String, key: String): RequestBody =
        JSONObject().put("dvdId", dvdId).put("key", key).toString()
            .toRequestBody("application/json".toMediaType())

    suspend fun addToPlaylist(dvdId: String, key: String): Boolean = withContext(Dispatchers.IO) {
        service.postJson("${base()}/api/playlists/add", playlistBody(dvdId, key), xsrf = xsrf()).checked(true)
        playlistOptions(dvdId).firstOrNull { it.key == key }?.isAdded == true
    }

    suspend fun removeFromPlaylist(dvdId: String, key: String): Boolean = withContext(Dispatchers.IO) {
        suspend fun added() = playlistOptions(dvdId).firstOrNull { it.key == key }?.isAdded == true
        runCatching { service.postJson("${base()}/api/playlists/remove", playlistBody(dvdId, key), xsrf = xsrf()).checked(true) }
        if (!added()) return@withContext false
        runCatching { service.deleteJson("${base()}/api/playlists/add", playlistBody(dvdId, key), xsrf = xsrf()).checked(true) }
        added()
    }

    suspend fun createPlaylist(name: String): List<MissAvPlaylist> = withContext(Dispatchers.IO) {
        val createUrl = url("/en/playlists/create")
        val html = service.getPageUrl(createUrl).checked(true)
        val spec = MissAvAccountParser.parseCreatePlaylistForm(html, createUrl)
            ?: throw MissAvAuthException("Create form not available")
        val fields = spec.fields.toMutableMap().apply { put(spec.nameField, name) }
        val r = service.postForm(spec.action, fields)
        r.checked(true)
        MissAvAccountParser.parsePlaylists(service.getPageUrl(url("/en/playlists")).checked(true))
    }
}
