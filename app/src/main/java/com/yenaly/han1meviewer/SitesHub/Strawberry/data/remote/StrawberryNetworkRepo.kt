package com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote

import android.util.Log
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberryConstants
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.local.StrawberryProfileCache
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryAnnouncementEntry
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryPageInfo
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryPictureCard
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberrySearchResult
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryTag
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryUserProfile
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryVideoCard
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryVideoDetail
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StreamResult
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.parser.StrawberryListingParser
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.parser.StrawberryWatchParser
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Response

object StrawberryNetworkRepo {

    private const val TAG = "StrawberryNetworkRepo"
    private const val MAX_RETRY_COUNT = 3
    private const val RETRY_DELAY_MS = 2_000L

    private class StrawberryHttpException(
        val httpCode: Int,
        override val message: String,
    ) : RuntimeException(message)

    private suspend fun <T> retryWithBackoff(operation: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                return operation()
            } catch (e: StrawberryHttpException) {
                if (e.httpCode == 404 || e.httpCode == 401 || e.httpCode == 403) {
                    throw e
                }
                lastError = e
            } catch (e: Exception) {
                lastError = e
            }
            Log.w(TAG, "Attempt ${attempt + 1}/$MAX_RETRY_COUNT failed: ${lastError.message}")
            if (attempt < MAX_RETRY_COUNT - 1) {
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("All retries failed")
    }

    private fun Response<ResponseBody>.toHtmlOrThrow(): String {
        val code = code()
        val bodyText = body()?.string().orEmpty()
        when {
            isSuccessful -> return bodyText
            code == 429 -> throw StrawberryHttpException(
                429,
                "Strawberry is rate limiting requests. Wait a minute and try again.",
            )
            code == 401 || code == 403 -> {
                StrawberryCookieManager.clearCookies()
                throw StrawberryHttpException(code, "Session expired (HTTP $code). Log in again.")
            }
            code == 404 -> throw StrawberryHttpException(
                404,
                "Content not found on Strawberry (404). Try a different mirror.",
            )
            code in 500..599 -> throw StrawberryHttpException(
                code,
                "Strawberry server error (HTTP $code). Retry in a moment.",
            )
            else -> throw StrawberryHttpException(code, "Unexpected response (HTTP $code).")
        }
    }

    fun getHomePage(
        page: Int = 1,
        query: String? = null,
        field: String? = null,
        length: String? = null,
        date: String? = null,
        sort: String? = null,
    ) = flow {
        emit(WebsiteState.Loading)
        try {
            val result = retryWithBackoff {
                val response = StrawberryNetwork.service.getHomePage(
                    page = page.takeIf { it > 1 },
                    query = query?.takeIf { it.isNotBlank() },
                    field = field?.takeIf { it.isNotBlank() },
                    length = length?.takeIf { it.isNotBlank() && it != StrawberryConstants.LENGTH_ANY },
                    date = date?.takeIf { it.isNotBlank() && it != StrawberryConstants.DATE_ANY },
                    sort = sort?.takeIf { it.isNotBlank() && it != StrawberryConstants.SORT_NEWEST },
                )
                val html = response.toHtmlOrThrow()
                StrawberryListingParser.parseHomePage(html, StrawberryNetwork.baseUrl)
            }
            result.currentUsername?.let(StrawberryProfileCache::setCurrentUsername)
            emit(WebsiteState.Success(result))
        } catch (e: Exception) {
            Log.e(TAG, "getHomePage failed", e)
            emit(WebsiteState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getHomePagePaged(
        page: Int,
        query: String? = null,
        field: String? = null,
        length: String? = null,
        date: String? = null,
        sort: String? = null,
    ) = flow {
        emit(PageLoadingState.Loading)
        try {
            val result = retryWithBackoff {
                val response = StrawberryNetwork.service.getHomePage(
                    page = page.takeIf { it > 1 },
                    query = query?.takeIf { it.isNotBlank() },
                    field = field?.takeIf { it.isNotBlank() },
                    length = length?.takeIf { it.isNotBlank() && it != StrawberryConstants.LENGTH_ANY },
                    date = date?.takeIf { it.isNotBlank() && it != StrawberryConstants.DATE_ANY },
                    sort = sort?.takeIf { it.isNotBlank() && it != StrawberryConstants.SORT_NEWEST },
                )
                val html = response.toHtmlOrThrow()
                val doc = Jsoup.parse(html, StrawberryNetwork.baseUrl)
                val cards = StrawberryListingParser.parseVideoCards(doc, StrawberryNetwork.baseUrl)
                val pageInfo = StrawberryListingParser.parsePaginator(doc, StrawberryNetwork.baseUrl)
                if (cards.isEmpty()) PageLoadingState.NoMoreData
                else PageLoadingState.Success(cards to pageInfo)
            }
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "getHomePagePaged failed", e)
            emit(PageLoadingState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun searchVideos(
        page: Int,
        query: String,
        field: String,
        length: String,
        date: String,
        sort: String,
    ) = flow {
        emit(PageLoadingState.Loading)
        try {
            val result = retryWithBackoff {
                val response = StrawberryNetwork.service.searchFiles(query, page.takeIf { it > 1 })

                val html = response.toHtmlOrThrow()
                val doc = Jsoup.parse(html, StrawberryNetwork.baseUrl)
                val cards = StrawberryListingParser.parseVideoCards(doc, StrawberryNetwork.baseUrl)
                val pageInfo = StrawberryListingParser.parsePaginator(doc, StrawberryNetwork.baseUrl)
                val heading = StrawberryListingParser.parseHeading(html, StrawberryNetwork.baseUrl)
                if (cards.isEmpty()) PageLoadingState.NoMoreData
                else PageLoadingState.Success(
                    StrawberrySearchResult(
                        query = query,
                        field = field,
                        pageInfo = pageInfo,
                        cards = cards,
                        headingText = heading,
                    )
                )
            }
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "searchVideos failed", e)
            emit(PageLoadingState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getTagPage(
        tagName: String,
        page: Int,
        query: String?,
        field: String?,
        length: String?,
        date: String?,
        sort: String?,
    ) = flow {
        emit(PageLoadingState.Loading)
        try {
            val result = retryWithBackoff {
                val response = StrawberryNetwork.service.getTagPage(
                    tagName = tagName,
                    page = page.takeIf { it > 1 },
                    query = query?.takeIf { it.isNotBlank() },
                    field = field?.takeIf { it.isNotBlank() },
                    length = length?.takeIf { it.isNotBlank() && it != StrawberryConstants.LENGTH_ANY },
                    date = date?.takeIf { it.isNotBlank() && it != StrawberryConstants.DATE_ANY },
                    sort = sort?.takeIf { it.isNotBlank() && it != StrawberryConstants.SORT_NEWEST },
                )
                val html = response.toHtmlOrThrow()
                val doc = Jsoup.parse(html, StrawberryNetwork.baseUrl)
                val cards = StrawberryListingParser.parseVideoCards(doc, StrawberryNetwork.baseUrl)
                val pageInfo = StrawberryListingParser.parsePaginator(doc, StrawberryNetwork.baseUrl)
                val heading = StrawberryListingParser.parseHeading(html, StrawberryNetwork.baseUrl)
                if (cards.isEmpty()) PageLoadingState.NoMoreData
                else PageLoadingState.Success(
                    StrawberrySearchResult(
                        query = query,
                        field = field ?: StrawberryConstants.SEARCH_FIELD_TAG,
                        pageInfo = pageInfo,
                        cards = cards,
                        headingText = heading,
                    )
                )
            }
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "getTagPage failed", e)
            emit(PageLoadingState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getTagsPage(
        page: Int = 1,
        query: String? = null,
        sort: String? = "popular",
    ) = flow {
        emit(PageLoadingState.Loading)
        try {
            val result = retryWithBackoff {
                val response = StrawberryNetwork.service.getTags(
                    page = page.takeIf { it > 1 },
                    query = query?.takeIf { it.isNotBlank() },
                    sort = sort?.takeIf { it.isNotBlank() },
                )
                val html = response.toHtmlOrThrow()
                val doc = Jsoup.parse(html, StrawberryNetwork.baseUrl)
                val tags = StrawberryListingParser.parseTagCloud(doc)
                val pageInfo = StrawberryListingParser.parsePaginator(doc, StrawberryNetwork.baseUrl)
                if (tags.isEmpty()) PageLoadingState.NoMoreData
                else PageLoadingState.Success(tags to pageInfo)
            }
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "getTagsPage failed", e)
            emit(PageLoadingState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getPicturesPage(
        page: Int = 1,
        query: String? = null,
    ) = flow {
        emit(PageLoadingState.Loading)
        try {
            val result = retryWithBackoff {
                val response = StrawberryNetwork.service.getPictures(
                    page = page.takeIf { it > 1 },
                    query = query?.takeIf { it.isNotBlank() },
                )
                val html = response.toHtmlOrThrow()
                val doc = Jsoup.parse(html, StrawberryNetwork.baseUrl)
                val cards: List<StrawberryPictureCard> =
                    StrawberryListingParser.parsePictureCards(doc, StrawberryNetwork.baseUrl)
                val pageInfo = StrawberryListingParser.parsePaginator(doc, StrawberryNetwork.baseUrl)
                if (cards.isEmpty()) PageLoadingState.NoMoreData
                else PageLoadingState.Success(cards to pageInfo)
            }
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "getPicturesPage failed", e)
            emit(PageLoadingState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getAnnouncements() = flow {
        emit(WebsiteState.Loading)
        try {
            val result = retryWithBackoff {
                val response = StrawberryNetwork.service.getAnnouncements()
                val html = response.toHtmlOrThrow()
                val doc = Jsoup.parse(html, StrawberryNetwork.baseUrl)
                StrawberryListingParser.parseAnnouncementEntries(doc)
            }
            emit(WebsiteState.Success(result))
        } catch (e: Exception) {
            Log.e(TAG, "getAnnouncements failed", e)
            emit(WebsiteState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getVideoDetail(videoId: String, quality: String? = null) = flow {
        emit(VideoLoadingState.Loading)
        try {
            val watchUrl = StrawberryNetwork.baseUrl.trimEnd('/') + "/file" + videoId +
                    (quality?.takeIf { it.isNotBlank() }?.let { "?quality=$it" } ?: "")
            val result = retryWithBackoff {
                val firstDetail = runCatching {
                    val response = StrawberryNetwork.service.getWatchPage(
                        videoId = videoId,
                        quality = quality?.takeIf { it.isNotBlank() },
                    )
                    if (response.code() == 404) return@runCatching null
                    val html = response.toHtmlOrThrow()
                    if (StrawberryListingParser.isLoginRequiredPage(html)) {
                        Log.w(TAG, "watch page shows login form (attempt 1)")
                        return@runCatching null
                    }
                    StrawberryWatchParser.parseWatchPage(html, watchUrl)
                }.getOrNull()

                val detail = firstDetail ?: run {
                    Log.w(
                        TAG,
                        "watch parse failed, retrying once with refreshed cookies. " +
                                "jarChars=${StrawberryCookieManager.cookiesRaw.length}, " +
                                "adPass=${StrawberryCookieManager.hasAdPass}",
                    )
                    val retryResponse = StrawberryNetwork.service.getWatchPage(
                        videoId = videoId,
                        quality = quality?.takeIf { it.isNotBlank() },
                    )
                    if (retryResponse.code() == 404) {
                        return@retryWithBackoff VideoLoadingState.NoContent
                    }
                    val retryHtml = retryResponse.toHtmlOrThrow()
                    if (StrawberryListingParser.isLoginRequiredPage(retryHtml)) {
                        return@retryWithBackoff VideoLoadingState.Error(
                            IllegalStateException(
                                "Session expired on this mirror. Sign in again from Settings, " +
                                        "or switch to a different mirror.",
                            ),
                        )
                    }
                    val retryDetail = StrawberryWatchParser.parseWatchPage(retryHtml, watchUrl)
                    if (retryDetail == null) {
                        Log.w(
                            TAG,
                            "watch parse failed twice; html length=${retryHtml.length}",
                        )
                        return@retryWithBackoff VideoLoadingState.Error(
                            IllegalStateException(
                                "This mirror returned a page without the watch layout twice. " +
                                        "Switch mirror from Cookies settings.",
                            ),
                        )
                    }
                    retryDetail
                }

                val normalized = if (detail.videoId.isBlank()) {
                    detail.copy(videoId = videoId)
                } else {
                    detail
                }
                VideoLoadingState.Success(normalized)
            }
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "getVideoDetail failed", e)
            emit(VideoLoadingState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchStreamUrlForQuality(
        videoId: String,
        quality: String,
    ): StreamResult? {
        if (videoId.isBlank()) {
            Log.w(TAG, "fetchStreamUrlForQuality called with blank videoId")
            return null
        }
        return try {
            retryWithBackoff {
                val response = StrawberryNetwork.service.getWatchPage(
                    videoId = videoId,
                    quality = quality,
                )
                if (response.code() == 404) return@retryWithBackoff null
                val html = response.toHtmlOrThrow()
                StrawberryWatchParser.parseStreamFromPage(html, StrawberryNetwork.baseUrl, quality)
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchStreamUrlForQuality failed for $quality", e)
            null
        }
    }

    fun getUserProfile(username: String, page: Int = 1) = flow {
        emit(VideoLoadingState.Loading)
        try {
            val result = retryWithBackoff {
                val response = StrawberryNetwork.service.getUserProfile(
                    username = username,
                    page = page.takeIf { it > 1 },
                )
                if (response.code() == 404) {
                    return@retryWithBackoff VideoLoadingState.NoContent
                }
                val html = response.toHtmlOrThrow()
                val doc = Jsoup.parse(html, StrawberryNetwork.baseUrl)
                val profile = parseUserProfile(doc, StrawberryNetwork.baseUrl, username)
                if (profile == null) {
                    VideoLoadingState.Error(IllegalStateException("Could not parse the user profile."))
                } else {
                    VideoLoadingState.Success(profile)
                }
            }
            if (result is VideoLoadingState.Success && result.info.isCurrentUser) {
                StrawberryProfileCache.put(result.info.username, result.info)
            }
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "getUserProfile failed", e)
            emit(VideoLoadingState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseUserProfile(
        doc: org.jsoup.nodes.Document,
        baseUrl: String,
        requestedUsername: String,
    ): StrawberryUserProfile? {
        val header = doc.selectFirst("div.profile-header") ?: return null
        val pageTitleEl = header.selectFirst("h1.page-title") ?: return null
        val displayName = pageTitleEl.text().trim().removePrefix("@").trim()
        if (displayName.isBlank()) return null

        val currentUsername = StrawberryListingParser.parseCurrentUsername(doc)
            ?: StrawberryProfileCache.getCurrentUsername()
        val isCurrentUser = currentUsername != null &&
                currentUsername.equals(displayName, ignoreCase = true)

        val avatarUrl = StrawberryListingParser.parseUserAvatar(doc)
        val bio = StrawberryListingParser.parseUserBio(doc)
        val followerCount = StrawberryListingParser.parseUserFollowerCount(doc)
        val followingCount = StrawberryListingParser.parseUserFollowingCount(doc)
        val videoCount = StrawberryListingParser.parseUserVideoCount(doc)
        val pictureCount = StrawberryListingParser.parseUserPictureCount(doc)
        val isFollowing = header.selectFirst("form[action*=unfollow]") != null

        val videosRoot = doc.selectFirst("div.video-grid")
        val videos = videosRoot?.let { StrawberryListingParser.parseVideoCards(doc, baseUrl) }.orEmpty()
        val pictures = videosRoot?.let { StrawberryListingParser.parsePictureCards(doc, baseUrl) }.orEmpty()
        val pageInfo = StrawberryListingParser.parsePaginator(doc, baseUrl)

        if (currentUsername == null && isCurrentUser) {
            StrawberryProfileCache.setCurrentUsername(displayName)
        }

        Log.d(
            TAG,
            "parseUserProfile: isCurrentUser=$isCurrentUser videos=${videos.size}",
        )

        return StrawberryUserProfile(
            username = displayName,
            displayName = displayName,
            bio = bio,
            avatarUrl = avatarUrl,
            followerCount = followerCount,
            followingCount = followingCount,
            videoCount = videoCount,
            pictureCount = pictureCount,
            isFollowing = isFollowing,
            isCurrentUser = isCurrentUser,
            videos = videos,
            pictures = pictures,
            pageInfo = pageInfo,
        )
    }
}
