package com.yenaly.han1meviewer.SitesHub.RedGifs.ui.shorts

import android.content.Context
import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation.LocalRedGifsActions
import coil3.ImageLoader
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsImageLoader
import com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsSettings
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.model.RedGifsGif
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsAuth
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsNetwork
import com.yenaly.han1meviewer.SitesHub.RedGifs.data.remote.RedGifsRepo
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.shorts.SiteShort
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortMeta
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortOwner
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortTag
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsPlayer
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsSource
import okhttp3.OkHttpClient

internal fun RedGifsGif.toSiteShort(): SiteShort {
    val title = description.ifBlank {
        tags.take(3).joinToString(" · ")
    }.ifBlank { if (userName.isNotBlank()) "@$userName" else id }
    return SiteShort(
        id = id,
        title = title,
        pageUrl = pageUrl,
        videoUrl = if (RedGifsSettings.quality == RedGifsSettings.QUALITY_SD) sdUrl else hdUrl,
        coverUrl = thumbnailUrl.ifBlank { posterUrl },
        views = views,
        durationSeconds = duration.toInt().takeIf { it > 0 },

        isGif = !hasAudio,
    )
}

internal object RedGifsShortsSource : SiteShortsSource {

    override val site: SiteType = SiteType.REDGIFS

    override val streamClient: OkHttpClient?
        get() = RedGifsNetwork.streamClient

    override fun imageLoader(context: Context): ImageLoader? = RedGifsImageLoader.get(context)

    override val isLoggedIn: Boolean
        get() = RedGifsAuth.isLoggedIn

    override val supportsRating: Boolean = true

    override val supportsDislike: Boolean = false

    override suspend fun loadMetas(shorts: List<SiteShort>): Result<Map<String, SiteShortMeta>> =
        runCatching {
            val liked = RedGifsRepo.likedIds()
            val follows = RedGifsRepo.followingNames()
            shorts.mapNotNull { s ->
                val gif = RedGifsRepo.cachedGif(s.id) ?: RedGifsRepo.gif(s.id).getOrNull()
                ?: return@mapNotNull null
                val user = RedGifsRepo.cachedUser(gif.userName)
                val owner = gif.userName.takeIf { it.isNotBlank() }?.let { name ->
                    SiteShortOwner(
                        name = name,
                        avatarUrl = user?.avatarUrl,
                        verified = gif.verified || user?.verified == true,
                        subscribers = user?.followers,
                        canSubscribe = true,
                        subscribed = name.lowercase() in follows,
                        path = name,
                    )
                }
                s.id to SiteShortMeta(
                    likes = gif.likes,
                    liked = s.id in liked,
                    owner = owner,
                    tags = gif.tags.take(8).map { SiteShortTag(name = it, path = it, kind = SiteShortTag.Kind.TAG) },
                )
            }.toMap()
        }

    override val supportsDownload: Boolean = true

    override suspend fun downloadShort(context: Context, short: SiteShort, creator: String?): Result<Unit> =
        com.yenaly.han1meviewer.util.SiteImageSaver.saveGif(
            context = context,
            site = SiteType.REDGIFS,
            url = short.videoUrl,
            creator = creator ?: RedGifsRepo.cachedGif(short.id)?.userName,
            baseName = short.id,
            title = short.title,
            coverUrl = short.coverUrl,
            pageUrl = short.pageUrl.ifBlank { null },
            views = short.views,
            durationSeconds = short.durationSeconds,
            tags = RedGifsRepo.cachedGif(short.id)?.tags.orEmpty().take(12),
            client = RedGifsNetwork.streamClient,
            headers = mapOf(
                "Referer" to com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsConstants.REFERER,
                "Origin" to com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsConstants.ORIGIN,
            ),
        )

    override suspend fun rate(short: SiteShort, like: Boolean): Result<Unit> {
        if (!isLoggedIn) return Result.failure(Exception(SiteShortsSource.LOGIN_REQUIRED))
        if (!like) return SiteShortsSource.unsupported()
        return RedGifsRepo.like(short.id)
    }

    override suspend fun undoRate(short: SiteShort): Result<Unit> {
        if (!isLoggedIn) return Result.failure(Exception(SiteShortsSource.LOGIN_REQUIRED))
        return RedGifsRepo.unlike(short.id)
    }

    override suspend fun setOwnerFollowed(
        short: SiteShort,
        owner: SiteShortOwner,
        follow: Boolean,
    ): Result<Unit> {
        if (!isLoggedIn) return Result.failure(Exception(SiteShortsSource.LOGIN_REQUIRED))
        return RedGifsRepo.setFollowing(owner.name, follow)
    }
}

@Composable
internal fun RedGifsShortsPlayer(
    items: List<SiteShort>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    val actions = LocalRedGifsActions.current
    SiteShortsPlayer(
        source = RedGifsShortsSource,
        items = items,
        initialIndex = initialIndex,
        onDismiss = onDismiss,
        hasMore = hasMore,
        onLoadMore = onLoadMore,
        onOpenTag = { tag -> actions.openTag(tag.name) },
        onOpenOwner = { owner -> actions.openCreator(owner.name) },
    )
}
