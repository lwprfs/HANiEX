package com.yenaly.han1meviewer.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.findActivityOrNull

@Composable
fun SanitizedPredictiveBackOwner(content: @Composable () -> Unit) {
    val activity = LocalContext.current.findActivityOrNull<ComponentActivity>()
    if (activity == null) {

        content()
        return
    }

    val bridge = remember(activity) {
        val decorView = activity.window?.decorView
        val bridge = SanitizingBackBridge(activity)
        // Site switches compose the incoming host before the outgoing one is disposed, so the
        // "previous" owner may itself be a bridge that is about to die. Keep it only as a link
        // in a chain and resolve to a live owner when restoring.
        bridge.previousOwner = decorView?.findViewTreeNavigationEventDispatcherOwner()
        decorView?.setViewTreeNavigationEventDispatcherOwner(bridge)
        bridge.restoreViewTreeOwner = {
            // Only touch the window if it still points at this bridge; otherwise a newer host
            // already took over and overwriting its owner would leave a dangling reference.
            if (decorView != null &&
                decorView.findViewTreeNavigationEventDispatcherOwner() === bridge
            ) {
                decorView.setViewTreeNavigationEventDispatcherOwner(bridge.liveFallbackOwner())
            }
        }
        bridge
    }
    DisposableEffect(activity, bridge) {
        onDispose {
            bridge.restoreViewTreeOwner?.invoke()
            bridge.restoreViewTreeOwner = null
            bridge.dispose()
        }
    }
    content()
}

private class SanitizingBackBridge(
    activity: ComponentActivity,
) : NavigationEventDispatcherOwner {

    var restoreViewTreeOwner: (() -> Unit)? = null

    /** Owner that was installed on the decor view when this bridge was created. */
    var previousOwner: NavigationEventDispatcherOwner? = null

    @Volatile
    var disposed = false
        private set

    /** Nearest earlier owner that has not been disposed (skips dead bridges in the chain). */
    fun liveFallbackOwner(): NavigationEventDispatcherOwner? {
        var candidate = previousOwner
        while (candidate is SanitizingBackBridge && candidate.disposed) {
            candidate = candidate.previousOwner
        }
        return candidate
    }

    val dispatcher = NavigationEventDispatcher()

    private var lastGoodProgress = 0f

    private val input = object : NavigationEventInput() {

        var hostCallback: OnBackPressedCallback? = null

        override fun onHasEnabledBackHandlersChanged(hasEnabledBackHandlers: Boolean) {

            hostCallback?.isEnabled = hasEnabledBackHandlers
        }

        fun backStarted(event: NavigationEvent) = dispatchOnBackStarted(event)

        fun backProgressed(event: NavigationEvent) = dispatchOnBackProgressed(event)

        fun backCompleted() = dispatchOnBackCompleted()

        fun backCancelled() = dispatchOnBackCancelled()
    }

    private val callback = object : OnBackPressedCallback(false) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) {

            if (Preferences.disablePredictiveBack) return
            input.backStarted(toSanitizedNavigationEvent(backEvent))
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            if (Preferences.disablePredictiveBack) return
            input.backProgressed(toSanitizedNavigationEvent(backEvent))
        }

        override fun handleOnBackPressed() {
            lastGoodProgress = 0f
            input.backCompleted()
        }

        override fun handleOnBackCancelled() {
            lastGoodProgress = 0f
            input.backCancelled()
        }
    }

    init {
        input.hostCallback = callback
        dispatcher.addInput(input)
        activity.onBackPressedDispatcher.addCallback(callback)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        callback.remove()
        dispatcher.dispose()
    }

    override val navigationEventDispatcher: NavigationEventDispatcher
        get() = dispatcher

    private fun toSanitizedNavigationEvent(backEvent: BackEventCompat): NavigationEvent {
        val raw = backEvent.progress
        val progress = if (raw.isFinite() && raw >= 0f) {
            raw.coerceAtMost(1f).also { lastGoodProgress = it }
        } else {
            lastGoodProgress
        }
        return NavigationEvent(
            swipeEdge = when (backEvent.swipeEdge) {
                BackEventCompat.EDGE_LEFT -> NavigationEvent.EDGE_LEFT
                BackEventCompat.EDGE_RIGHT -> NavigationEvent.EDGE_RIGHT
                else -> NavigationEvent.EDGE_NONE
            },
            progress = progress,
            touchX = backEvent.touchX,
            touchY = backEvent.touchY,
        )
    }
}
