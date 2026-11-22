package com.yenaly.han1meviewer.ui.component.shorts

import androidx.media3.common.Player

/**
 * What the center overlay of the active short should represent.
 *
 * This is deliberately derived from *actual* player facts (playback state + isPlaying) and never
 * from `playWhenReady`, which only expresses intent.
 */
internal enum class ShortsPlaybackUi {
    /** Resolving metadata / preparing / buffering. Show the spinner (never the play icon). */
    Loading,

    /** Frames are actually advancing. Show nothing. */
    Playing,

    /** Ready but not playing. Show the play affordance. */
    Paused,

    /** Playback reached the end (only reachable when repeat is off). Show the play affordance. */
    Ended,

    /** Resolution or playback failed. Show retry. */
    Failed,
}

/**
 * Pure mapping from raw facts to [ShortsPlaybackUi].
 *
 * @param loaded true once the player really holds media for the active short.
 * @param failed true when resolution or playback of the active short failed.
 * @param exoState one of `Player.STATE_*`.
 * @param isPlaying `Player.isPlaying` (READY, playWhenReady and not suppressed).
 */
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

/** Genuine first loads show the spinner right away; mid-playback re-buffers wait a moment. */
internal const val SHORTS_REBUFFER_SPINNER_DELAY_MS = 350L

internal fun shortsSpinnerDelayMs(firstFrameRendered: Boolean): Long =
    if (firstFrameRendered) SHORTS_REBUFFER_SPINNER_DELAY_MS else 0L

/** The first item follows the autoplay setting; once the user swiped, new items always play. */
internal fun shouldAutoPlayItem(configAutoPlay: Boolean, userHasSwiped: Boolean): Boolean =
    configAutoPlay || userHasSwiped

/** Duplicate ids would crash pager keys, so only the first occurrence of an id is kept. */
internal fun dedupeShorts(items: List<SiteShort>): List<SiteShort> = items.distinctBy { it.id }

/** Index inside the de-duplicated list that corresponds to [initialIndex] of the raw list. */
internal fun resolveInitialIndex(rawIds: List<String>, initialIndex: Int): Int {
    if (rawIds.isEmpty()) return 0
    val wantedId = rawIds[initialIndex.coerceIn(0, rawIds.lastIndex)]
    return rawIds.distinct().indexOf(wantedId).coerceAtLeast(0)
}

/**
 * Load-more fires once per list size while the user is near the end, so recomposition or an
 * append that keeps the user near the end cannot trigger it repeatedly.
 */
internal fun shouldRequestLoadMore(
    hasMore: Boolean,
    currentPage: Int,
    size: Int,
    lastRequestedSize: Int,
    threshold: Int = 3,
): Boolean = hasMore && size > 0 && currentPage >= size - threshold && lastRequestedSize != size
