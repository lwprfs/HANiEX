package com.yenaly.han1meviewer.ui.component.shorts

import androidx.media3.common.Player

internal enum class ShortsPlaybackUi {

    Loading,

    Playing,

    Paused,

    Ended,

    Failed,
}

internal fun resolveShortsPlaybackUi(
    loaded: Boolean,
    failed: Boolean,
    exoState: Int,
    isPlaying: Boolean,
): ShortsPlaybackUi = when {
    failed -> ShortsPlaybackUi.Failed
    !loaded -> ShortsPlaybackUi.Loading
    exoState == Player.STATE_ENDED -> ShortsPlaybackUi.Ended
    exoState == Player.STATE_IDLE || exoState == Player.STATE_BUFFERING -> ShortsPlaybackUi.Loading
    isPlaying -> ShortsPlaybackUi.Playing
    else -> ShortsPlaybackUi.Paused
}

internal const val SHORTS_REBUFFER_SPINNER_DELAY_MS = 350L

internal fun shortsSpinnerDelayMs(firstFrameRendered: Boolean): Long =
    if (firstFrameRendered) SHORTS_REBUFFER_SPINNER_DELAY_MS else 0L

internal fun shouldAutoPlayItem(configAutoPlay: Boolean, userHasSwiped: Boolean): Boolean =
    configAutoPlay || userHasSwiped

internal fun dedupeShorts(items: List<SiteShort>): List<SiteShort> = items.distinctBy { it.id }

internal fun resolveInitialIndex(rawIds: List<String>, initialIndex: Int): Int {
    if (rawIds.isEmpty()) return 0
    val wantedId = rawIds[initialIndex.coerceIn(0, rawIds.lastIndex)]
    return rawIds.distinct().indexOf(wantedId).coerceAtLeast(0)
}

internal fun shouldRequestLoadMore(
    hasMore: Boolean,
    currentPage: Int,
    size: Int,
    lastRequestedSize: Int,
    threshold: Int = 3,
): Boolean = hasMore && size > 0 && currentPage >= size - threshold && lastRequestedSize != size
