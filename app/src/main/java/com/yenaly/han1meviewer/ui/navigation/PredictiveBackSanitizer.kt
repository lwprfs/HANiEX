package com.yenaly.han1meviewer.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.findActivityOrNull

@Composable
fun SanitizedPredictiveBackOwner(content: @Composable () -> Unit) {
    val activity = LocalContext.current.findActivityOrNull<ComponentActivity>()
    if (activity == null) {

        content()
        return
    }
    val bridge = remember(activity) { SanitizingBackBridge(activity) }
    DisposableEffect(activity, bridge) {
        onDispose { bridge.dispose() }
    }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides bridge, content = content)
}

private class SanitizingBackBridge(
    activity: ComponentActivity,
) : NavigationEventDispatcherOwner {

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
