package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.shorts

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryImageLoader
import com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGallerySession
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryShort
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryAuth
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryNetwork
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryRepo
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.content.LoginLockedScreen
import com.yenaly.han1meviewer.ui.component.shorts.SiteShort
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortMeta
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortOwner
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsPlayer
import com.yenaly.han1meviewer.ui.component.shorts.SiteShortsSource
import com.yenaly.han1meviewer.util.SiteImageSaver
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

internal fun LeakGalleryShort.toSiteShort(): SiteShort = SiteShort(
    id = id,
    title = title,
    pageUrl = LeakGalleryConstants.webUrl(username, id),
    videoUrl = videoUrl,
    coverUrl = coverUrl,
    views = views.takeIf { it > 0 },
    durationSeconds = durationSec.takeIf { it > 0 },
)

internal object LeakGalleryShortsSource : SiteShortsSource {

    override val site: SiteType = SiteType.LEAKGALLERY

    override val streamClient: OkHttpClient?
        get() = LeakGalleryNetwork.streamClient

    override fun imageLoader(context: Context): ImageLoader? = LeakGalleryImageLoader.get(context)

    override val isLoggedIn: Boolean
        get() = LeakGalleryAuth.isLoggedIn

    override val supportsRating: Boolean = true

    override val supportsDislike: Boolean = false

    override suspend fun loadMetas(shorts: List<SiteShort>): Result<Map<String, SiteShortMeta>> =
        runCatching {
            shorts.mapNotNull { s ->
                val item = LeakGalleryRepo.cachedShort(s.id) ?: return@mapNotNull null
                val owner = item.username.takeIf { it.isNotBlank() }?.let { name ->
                    SiteShortOwner(
                        name = name,
                        avatarUrl = item.avatarUrl.ifBlank { null },
                        canSubscribe = true,
                        subscribed = item.subscribed,
                        path = name,
                    )
                }
                s.id to SiteShortMeta(likes = item.likes, liked = item.liked, owner = owner)
            }.toMap()
        }

    override val supportsDownload: Boolean = true

    override suspend fun downloadShort(context: Context, short: SiteShort, creator: String?): Result<Unit> =
        SiteImageSaver.saveGif(
            context = context,
            site = SiteType.LEAKGALLERY,
            url = short.videoUrl,
            creator = creator ?: LeakGalleryRepo.cachedShort(short.id)?.username,
            baseName = short.id,
            title = short.title,
            coverUrl = short.coverUrl,
            pageUrl = short.pageUrl.ifBlank { null },
            views = short.views,
            durationSeconds = short.durationSeconds,
            client = LeakGalleryNetwork.streamClient,
            headers = mapOf(
                "Referer" to "${LeakGalleryConstants.BASE_URL}/",
                "Origin" to LeakGalleryConstants.BASE_URL,
            ),
        )

    override suspend fun rate(short: SiteShort, like: Boolean): Result<Unit> = setLiked(short, like)

    override suspend fun undoRate(short: SiteShort): Result<Unit> = setLiked(short, false)

    private suspend fun setLiked(short: SiteShort, like: Boolean): Result<Unit> {
        if (!isLoggedIn) return Result.failure(Exception(SiteShortsSource.LOGIN_REQUIRED))
        return LeakGalleryRepo.setLiked(short.id, like).map { count ->
            LeakGalleryRepo.updateShort(short.id) {
                it.copy(liked = like, likes = count ?: (it.likes + if (like) 1 else -1).coerceAtLeast(0))
            }
        }
    }

    override suspend fun setOwnerFollowed(
        short: SiteShort,
        owner: SiteShortOwner,
        follow: Boolean,
    ): Result<Unit> {
        if (!isLoggedIn) return Result.failure(Exception(SiteShortsSource.LOGIN_REQUIRED))
        val cached = LeakGalleryRepo.cachedShort(short.id)
        val profileId = cached?.profileId?.takeIf { it > 0L }
            ?: LeakGalleryRepo.profileInfo(owner.name).getOrNull()?.id
            ?: return Result.failure(Exception("Profile not found"))
        return LeakGalleryRepo.setFollowing(profileId, follow).onSuccess {
            LeakGalleryRepo.markFollowing(owner.name, follow)
        }
    }
}

@Composable
fun LeakGalleryShortsScreen(
    mode: String,
    startId: String?,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenCreator: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val epoch by LeakGallerySession.epoch.collectAsStateWithLifecycle()
    val loggedIn = remember(epoch) { LeakGalleryAuth.isLoggedIn }
    val blocked = mode == LeakGalleryConstants.SHORTS_FOLLOWED && !loggedIn

    var shorts by remember(mode, startId) { mutableStateOf<List<LeakGalleryShort>>(emptyList()) }
    var nextPage by remember(mode, startId) { mutableIntStateOf(0) }
    var hasNext by remember(mode, startId) { mutableStateOf(false) }
    var loading by remember(mode, startId) { mutableStateOf(true) }
    var loadingMore by remember(mode, startId) { mutableStateOf(false) }
    var error by remember(mode, startId) { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val perPageFixed = mode != LeakGalleryConstants.SHORTS_FOR_YOU

    LaunchedEffect(mode, startId, reload, loggedIn) {
        if (blocked) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        LeakGalleryRepo.shorts(mode, 0)
            .onSuccess { page ->
                var list = page.shorts
                if (startId != null) {
                    val at = list.indexOfFirst { it.id == startId }
                    list = when {
                        at > 0 -> listOf(list[at]) + list.filterIndexed { i, _ -> i != at }
                        at == 0 -> list
                        else -> LeakGalleryRepo.shortById(startId).getOrNull()
                            ?.let { first -> listOf(first) + list.filter { it.id != first.id } }
                            ?: list
                    }
                }
                shorts = list
                nextPage = 1
                hasNext = page.hasNext
            }
            .onFailure {

                val only = startId?.let { id -> LeakGalleryRepo.shortById(id).getOrNull() }
                if (only != null) {
                    shorts = listOf(only)
                    hasNext = false
                } else {
                    shorts = emptyList()
                    hasNext = false
                    error = it.message ?: it.javaClass.simpleName
                }
            }
        loading = false
    }

    fun loadMore() {
        if (!hasNext || loadingMore || loading) return
        loadingMore = true
        scope.launch {
            LeakGalleryRepo.shorts(mode, nextPage)
                .onSuccess { page ->
                    val known = shorts.map { it.id }.toHashSet()
                    val fresh = page.shorts.filter { it.id !in known }
                    shorts = shorts + fresh
                    if (perPageFixed) nextPage += 1
                    hasNext = page.hasNext && fresh.isNotEmpty()
                }
                .onFailure { hasNext = false }
            loadingMore = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            blocked -> LoginLockedScreen(
                title = stringResource(R.string.leakgallery_shorts_following),
                onBack = onBack,
                onSignIn = onLogin,
            )
            loading && shorts.isEmpty() -> SiteLoadingContent()
            shorts.isEmpty() && error != null ->
                SiteErrorContent(message = error, onRetry = { reload++ })
            shorts.isEmpty() ->
                SiteEmptyContent(hint = stringResource(R.string.leakgallery_shorts_empty))
            else -> {
                val items = remember(shorts) { shorts.map { it.toSiteShort() } }
                SiteShortsPlayer(
                    source = LeakGalleryShortsSource,
                    items = items,
                    initialIndex = 0,
                    onDismiss = onBack,
                    hasMore = hasNext,
                    onLoadMore = { loadMore() },
                    onOpenOwner = { owner -> onOpenCreator(owner.name) },
                )
            }
        }
    }
}
