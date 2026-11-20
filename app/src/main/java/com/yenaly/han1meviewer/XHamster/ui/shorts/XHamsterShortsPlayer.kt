package com.yenaly.han1meviewer.XHamster.ui.shorts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.XHamster.common.XHamsterImageLoader
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterCommentResult
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShort
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortComment
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortMeta
import com.yenaly.han1meviewer.XHamster.data.model.XHamsterShortTag
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterCategoryRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterProfileRoute
import com.yenaly.han1meviewer.XHamster.ui.navigation.XHamsterTagShortsRoute
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterMomentsRepo
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterNetwork
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterRepo
import com.yenaly.han1meviewer.ui.component.shorts.SiteShort
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortComment
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortCommentsPage
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortMeta
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortOwner
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortPostResult
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortSubtitle
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortTag
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsPlayer
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsSource
import okhttp3.OkHttpClient
import java.util.UUID

internal fun XHamsterShort.toSiteShort(gif: Boolean = false) = SiteShort(
    id = id.toString(),
    title = title,
    pageUrl = pageUrl,
    videoUrl = videoUrl,
    coverUrl = coverUrl,
    views = views,
    isGif = gif || pageUrl.contains("/gifs/"),
)

internal fun XHamsterShortMeta.toSiteMeta() = SiteShortMeta(
    streamUrl = streamUrl,
    likes = likes,
    liked = liked,
    dislikes = dislikes,
    disliked = disliked,
    favorite = favorite,
    commentsCount = commentsCount,
    commentsDisabled = commentsDisabled,
    subtitles = subtitles.map { SiteShortSubtitle(it.label, it.lang, it.url) },
    owner = ownerName?.takeIf { it.isNotBlank() }
        ?.let { SiteShortOwner(name = it, avatarUrl = ownerAvatar, path = ownerPath) },
    tags = tags.map {
        SiteShortTag(
            name = it.name,
            path = it.path,
            kind = when (it.kind) {
                XHamsterShortTag.Kind.CATEGORY -> SiteShortTag.Kind.CATEGORY
                XHamsterShortTag.Kind.TAG -> SiteShortTag.Kind.TAG
                XHamsterShortTag.Kind.PERSON -> SiteShortTag.Kind.PERSON
                XHamsterShortTag.Kind.CHANNEL -> SiteShortTag.Kind.CHANNEL
            },
        )
    },
)

private fun XHamsterShortComment.toSiteComment() = SiteShortComment(
    id = id,
    author = author,
    avatarUrl = avatarUrl,
    text = text,
    likes = likes,
    createdSeconds = createdSeconds,
    isReply = isReply,
    replyToName = replyToName,
    editable = editable,
    likedByMe = likedByMe,
)

internal class XHamsterShortsSource(private val referer: String) : SiteShortsSource {
    override val site = SiteType.XHAMSTER
    override val streamClient: OkHttpClient get() = XHamsterNetwork.streamClient
    override fun imageLoader(context: Context): ImageLoader = XHamsterImageLoader.get(context)

    override val isLoggedIn: Boolean get() = XHamsterNetwork.hasAuthCookies()
    override val supportsRating = true
    override val supportsFavorite = true
    override val supportsComments = true

    private fun SiteShort.ref() = pageUrl.ifBlank { referer }
    private fun SiteShort.videoId() = id.toLong()

    override suspend fun loadMetas(shorts: List<SiteShort>): Result<Map<String, SiteShortMeta>> =
        XHamsterRepo.shortMetas(shorts.mapNotNull { it.id.toLongOrNull() }, referer)
            .map { metas -> metas.entries.associate { it.key.toString() to it.value.toSiteMeta() } }

    override suspend fun rate(short: SiteShort, like: Boolean) =
        XHamsterMomentsRepo.rate(short.videoId(), like, short.ref())

    override suspend fun undoRate(short: SiteShort) =
        XHamsterMomentsRepo.undoRate(short.videoId(), short.ref())

    override suspend fun setFavorite(short: SiteShort, favorite: Boolean) =
        XHamsterMomentsRepo.setFavorite(short.videoId(), favorite, short.ref())

    override suspend fun comments(short: SiteShort, cursor: String?): Result<SiteShortCommentsPage> =
        XHamsterMomentsRepo.comments(short.videoId(), cursor, short.ref()).map { page ->
            SiteShortCommentsPage(
                items = page.items.map { it.toSiteComment() },
                nextCursor = page.nextCursor,
                total = page.total,
            )
        }

    override suspend fun postComment(
        short: SiteShort,
        text: String,
        replyTo: SiteShortComment?,
    ): SiteShortPostResult = when (
        val result = XHamsterMomentsRepo.postComment(
            videoId = short.videoId(),
            text = text,
            replyToId = replyTo?.id,
            replyToName = replyTo?.author,
            watchId = UUID.randomUUID().toString(),
            referer = short.ref(),
        )
    ) {
        XHamsterCommentResult.Posted -> SiteShortPostResult.Posted
        XHamsterCommentResult.LoginRequired -> SiteShortPostResult.LoginRequired
        XHamsterCommentResult.CaptchaRequired -> SiteShortPostResult.CaptchaRequired
        is XHamsterCommentResult.Rejected -> SiteShortPostResult.Rejected(result.message)
    }

    override suspend fun likeComment(short: SiteShort, comment: SiteShortComment) =
        XHamsterMomentsRepo.likeComment(short.videoId(), comment.id, short.ref())

    override suspend fun deleteComment(short: SiteShort, comment: SiteShortComment) =
        XHamsterMomentsRepo.deleteComment(short.videoId(), comment.id, short.ref())

    override fun reportUrl(short: SiteShort): String = XHamsterMomentsRepo.reportUrl(short.ref())
}

@Composable
fun XHamsterShortsPlayer(
    items: List<SiteShort>,
    initialIndex: Int,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    referer: String,
    owner: SiteShortOwner? = null,
    onToggleSubscribe: () -> Unit = {},
    initialMetas: Map<String, SiteShortMeta> = emptyMap(),
) {
    val source = remember(referer) { XHamsterShortsSource(referer) }

    val openTag: (SiteShortTag) -> Unit = { tag ->
        onDismiss()
        NavigationManager.getNavController()?.let { nav ->
            when (tag.kind) {
                SiteShortTag.Kind.PERSON, SiteShortTag.Kind.CHANNEL ->
                    nav.navigateSafely(XHamsterProfileRoute(tag.path, tag.name))
                else -> nav.navigateSafely(XHamsterTagShortsRoute(tag.path, tag.name))
            }
        }
    }

    val openOwner: (SiteShortOwner) -> Unit = { owner ->
        val path = owner.path
        if (path != null) {
            onDismiss()
            NavigationManager.getNavController()?.let { nav ->
                val isProfile = path.startsWith("/users/") || path.startsWith("/creators/") ||
                        path.startsWith("/pornstars/") || path.startsWith("/channels/")
                if (isProfile) nav.navigateSafely(XHamsterProfileRoute(path, owner.name))
                else nav.navigateSafely(XHamsterCategoryRoute(path, owner.name))
            }
        }
    }
    SiteShortsPlayer(
        source = source,
        items = items,
        initialIndex = initialIndex,
        onDismiss = onDismiss,
        hasMore = hasMore,
        onLoadMore = onLoadMore,
        owner = owner,
        onToggleSubscribe = onToggleSubscribe,
        initialMetas = initialMetas,
        onOpenTag = openTag,
        onOpenOwner = openOwner,
    )
}
