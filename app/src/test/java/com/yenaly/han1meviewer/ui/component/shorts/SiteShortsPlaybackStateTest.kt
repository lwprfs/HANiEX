package com.yenaly.han1meviewer.ui.component.shorts

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteShortsPlaybackStateTest {

    private fun ui(
        loaded: Boolean = true,
        failed: Boolean = false,
        state: Int = Player.STATE_READY,
        playing: Boolean = false,
    ) = resolveShortsPlaybackUi(loaded, failed, state, playing)

    @Test
    fun notLoadedIsLoadingNeverPaused() {
        assertEquals(ShortsPlaybackUi.Loading, ui(loaded = false, state = Player.STATE_IDLE))
        assertEquals(ShortsPlaybackUi.Loading, ui(loaded = false, state = Player.STATE_READY))
    }

    @Test
    fun bufferingIsLoadingEvenWhenNotPlaying() {
        assertEquals(ShortsPlaybackUi.Loading, ui(state = Player.STATE_BUFFERING))
        assertEquals(ShortsPlaybackUi.Loading, ui(state = Player.STATE_IDLE))
    }

    @Test
    fun readyAndPlayingIsPlaying() {
        assertEquals(ShortsPlaybackUi.Playing, ui(playing = true))
    }

    @Test
    fun readyAndNotPlayingIsPaused() {
        assertEquals(ShortsPlaybackUi.Paused, ui(playing = false))
    }

    @Test
    fun endedAndFailed() {
        assertEquals(ShortsPlaybackUi.Ended, ui(state = Player.STATE_ENDED))
        assertEquals(ShortsPlaybackUi.Failed, ui(failed = true, playing = true))
        assertEquals(ShortsPlaybackUi.Failed, ui(loaded = false, failed = true))
    }

    @Test
    fun spinnerDelay() {
        assertEquals(0L, shortsSpinnerDelayMs(firstFrameRendered = false))
        assertEquals(SHORTS_REBUFFER_SPINNER_DELAY_MS, shortsSpinnerDelayMs(firstFrameRendered = true))
    }

    @Test
    fun autoplayDecision() {
        assertFalse(shouldAutoPlayItem(configAutoPlay = false, userHasSwiped = false))
        assertTrue(shouldAutoPlayItem(configAutoPlay = false, userHasSwiped = true))
        assertTrue(shouldAutoPlayItem(configAutoPlay = true, userHasSwiped = false))
    }

    @Test
    fun dedupeAndInitialIndex() {
        val ids = listOf("a", "b", "a", "c")
        val items = ids.map { SiteShort(it, it, "", "", "") }
        assertEquals(listOf("a", "b", "c"), dedupeShorts(items).map { it.id })
        assertEquals(0, resolveInitialIndex(ids, 0))
        assertEquals(0, resolveInitialIndex(ids, 2))
        assertEquals(2, resolveInitialIndex(ids, 3))
        assertEquals(2, resolveInitialIndex(ids, 99))
        assertEquals(0, resolveInitialIndex(ids, -5))
        assertEquals(0, resolveInitialIndex(emptyList(), 3))
    }

    @Test
    fun loadMoreFiresOncePerSize() {
        assertTrue(shouldRequestLoadMore(true, 8, 10, -1))
        assertFalse(shouldRequestLoadMore(true, 8, 10, 10))
        assertFalse(shouldRequestLoadMore(true, 2, 10, -1))
        assertFalse(shouldRequestLoadMore(false, 9, 10, -1))
        assertTrue(shouldRequestLoadMore(true, 12, 15, 10))
    }
}
