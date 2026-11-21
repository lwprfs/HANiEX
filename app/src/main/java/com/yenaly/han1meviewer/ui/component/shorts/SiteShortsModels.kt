package com.yenaly.han1meviewer.ui.component.shorts

import android.content.Context
import coil3.ImageLoader
import com.yenaly.han1meviewer.SiteType
import okhttp3.OkHttpClient

data class SiteShort(
    val id: String,
    val title: String,
    val pageUrl: String,
    val videoUrl: String,
    val coverUrl: String,
    val views: Long? = null,

    val isGif: Boolean = false,
    /** Optional. Only set when the source really provides it; the UI hides the badge otherwise. */
    val durationSeconds: Int? = null,
)

data class SiteShortSubtitle(
    val label: String,
    val lang: String,
    val url: String,
)

data class SiteShortOwner(
    val name: String,
    val avatarUrl: String? = null,
    val verified: Boolean = false,
    val subscribers: Long? = null,
    val isChannel: Boolean = false,
    val canSubscribe: Boolean = false,
    val subscribed: Boolean = false,
    val subscribeBusy: Boolean = false,
    /** Source-specific profile path. When set, tapping the owner in the player can open their profile. */
    val path: String? = null,
)

data class SiteShortMeta(
    val streamUrl: String? = null,
    val likes: Long = 0L,
    val liked: Boolean = false,
    val dislikes: Long = 0L,
    val disliked: Boolean = false,
    val favorite: Boolean = false,
    val commentsCount: Long = 0L,
    val commentsDisabled: Boolean = false,
    val subtitles: List<SiteShortSubtitle> = emptyList(),
    val owner: SiteShortOwner? = null,
    val tags: List<SiteShortTag> = emptyList(),
)

/** A tappable tag / category / performer / channel shown under a short. [path] is source-specific. */
data class SiteShortTag(
    val name: String,
    val path: String,
    val kind: Kind = Kind.TAG,
) {
    enum class Kind { CATEGORY, TAG, PERSON, CHANNEL }
}

data class SiteShortComment(
    val id: String,
    val author: String,
    val avatarUrl: String?,
    val text: String,
    val likes: Long,
    val createdSeconds: Long?,
    val isReply: Boolean,
    val replyToName: String? = null,
    val editable: Boolean = false,
    val likedByMe: Boolean = false,
)

data class SiteShortCommentsPage(
    val items: List<SiteShortComment>,
    val nextCursor: String?,
    val total: Long = items.size.toLong(),
)

sealed interface SiteShortPostResult {
    data object Posted : SiteShortPostResult
    data object LoginRequired : SiteShortPostResult
    data object CaptchaRequired : SiteShortPostResult
    data class Rejected(val message: String?) : SiteShortPostResult
}

interface SiteShortsSource {
    val site: SiteType

    val streamClient: OkHttpClient? get() = null

    fun imageLoader(context: Context): ImageLoader? = null

    val isLoggedIn: Boolean get() = false
    val supportsRating: Boolean get() = false
    val supportsFavorite: Boolean get() = false
    val supportsComments: Boolean get() = false
    val supportsDislike: Boolean get() = true

    suspend fun loadMetas(shorts: List<SiteShort>): Result<Map<String, SiteShortMeta>> =
        Result.success(emptyMap())

    suspend fun rate(short: SiteShort, like: Boolean): Result<Unit> = unsupported()
    suspend fun undoRate(short: SiteShort): Result<Unit> = unsupported()
    suspend fun setFavorite(short: SiteShort, favorite: Boolean): Result<Unit> = unsupported()

    suspend fun comments(short: SiteShort, cursor: String?): Result<SiteShortCommentsPage> = unsupported()
    suspend fun postComment(
        short: SiteShort,
        text: String,
        replyTo: SiteShortComment?,
    ): SiteShortPostResult = SiteShortPostResult.Rejected(null)

    suspend fun likeComment(short: SiteShort, comment: SiteShortComment): Result<Unit> = unsupported()
    suspend fun deleteComment(short: SiteShort, comment: SiteShortComment): Result<Unit> = unsupported()

    suspend fun setOwnerFollowed(
        short: SiteShort,
        owner: SiteShortOwner,
        follow: Boolean,
    ): Result<Unit> = unsupported()

    fun reportUrl(short: SiteShort): String? = null

    companion object {

        const val LOGIN_REQUIRED = "LOGIN_REQUIRED"

        internal fun <T> unsupported(): Result<T> =
            Result.failure(UnsupportedOperationException())
    }
}
