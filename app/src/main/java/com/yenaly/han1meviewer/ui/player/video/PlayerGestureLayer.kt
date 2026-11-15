package com.yenaly.han1meviewer.ui.player.video

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.yenaly.han1meviewer.ui.player.PlayerDefaults
import com.yenaly.han1meviewer.ui.player.PlayerUtils
import com.yenaly.han1meviewer.util.appScreenWidth
import com.yenaly.han1meviewer.util.findActivityOrNull
import com.yenaly.han1meviewer.util.statusBarHeight
import kotlin.math.abs

data class GestureIndicatorData(
    val type: GestureIndicatorType,
    val percent: Float,
    val text: String,
    val subtitle: String? = null,
)

enum class GestureIndicatorType { Progress, Volume, Brightness }

@Composable
fun Modifier.playerGestures(
    enabled: Boolean,
    dragEnabled: Boolean,
    seekEnabled: Boolean,
    sensitivity: Int,
    durationMsProvider: () -> Long,
    positionMsProvider: () -> Long,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onIndicator: (GestureIndicatorData?) -> Unit,
): Modifier {
    val view = LocalView.current
    val window = remember(view) {
        view.context.findActivityOrNull<Activity>()?.window
    }
    val audioManager = remember(view) {
        view.context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)
    val currentOnSeekTo by rememberUpdatedState(onSeekTo)
    val currentOnIndicator by rememberUpdatedState(onIndicator)
    val currentDuration by rememberUpdatedState(durationMsProvider)
    val currentPosition by rememberUpdatedState(positionMsProvider)
    val currentSeekEnabled by rememberUpdatedState(seekEnabled)

    return this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onDoubleTap = { currentOnDoubleTap() },
                onTap = { currentOnTap() },
                onLongPress = { currentOnLongPressStart() },
                onPress = {
                    tryAwaitRelease()

                    currentOnLongPressEnd()
                },
            )
        }
        .pointerInput(enabled, dragEnabled, sensitivity) {
            if (!enabled || !dragEnabled) return@pointerInput
            val threshold = PlayerDefaults.GESTURE_THRESHOLD.toFloat()
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                val downX = down.position.x
                val downY = down.position.y

                if (downY < statusBarHeight || downX > appScreenWidth) {
                    return@awaitEachGesture
                }

                var dragging = false
                var horizontal = false
                var changeBrightness = false
                var gestureDownPosition = 0L
                var gestureDownVolume = 0
                var gestureDownBrightness = 0f
                var targetPosition = 0L

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (change.isConsumed) return@awaitEachGesture
                    if (!change.pressed) break

                    val dx = change.position.x - downX
                    val dy = change.position.y - downY

                    if (!dragging) {
                        val absDx = abs(dx)
                        val absDy = abs(dy)
                        if (absDx > threshold || absDy > threshold) {
                            if (absDx >= threshold) {

                                if (!currentSeekEnabled) return@awaitEachGesture
                                horizontal = true
                                dragging = true
                                gestureDownPosition = currentPosition()
                            } else {
                                dragging = true
                                if (downX < appScreenWidth * 0.5f) {

                                    changeBrightness = true
                                    gestureDownBrightness = readCurrentBrightness(window)
                                } else {

                                    gestureDownVolume =
                                        audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                }
                            }
                        }
                    }

                    if (dragging) {
                        if (horizontal) {
                            val duration = currentDuration()
                            val delta = dx * duration / (size.width * sensitivity)
                            targetPosition = (gestureDownPosition + delta.toLong())
                                .coerceIn(0L, duration)
                            currentOnIndicator(
                                GestureIndicatorData(
                                    type = GestureIndicatorType.Progress,
                                    percent = if (duration > 0) {
                                        targetPosition.toFloat() / duration
                                    } else {
                                        0f
                                    },
                                    text = PlayerUtils.stringForTime(targetPosition),
                                    subtitle = PlayerUtils.stringForTime(duration),
                                )
                            )
                        } else {
                            val deltaY = -dy
                            if (changeBrightness) {
                                val deltaV = 255f * deltaY * 3f / size.height
                                val newBrightness =
                                    ((gestureDownBrightness + deltaV) / 255f).coerceIn(0.01f, 1f)
                                window?.let { w ->
                                    w.attributes = w.attributes.apply {
                                        screenBrightness = newBrightness
                                    }
                                }
                                val percent =
                                    ((gestureDownBrightness + deltaV) / 255f).coerceIn(0f, 1f)
                                currentOnIndicator(
                                    GestureIndicatorData(
                                        type = GestureIndicatorType.Brightness,
                                        percent = percent,
                                        text = "${(percent * 100).toInt()}%",
                                    )
                                )
                            } else {
                                val max =
                                    audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        ?: break
                                val deltaV = (max * deltaY * 3f / size.height).toInt()
                                val newVolume = (gestureDownVolume + deltaV).coerceIn(0, max)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                val percent =
                                    ((gestureDownVolume + deltaV).coerceIn(0, max)).toFloat() / max
                                currentOnIndicator(
                                    GestureIndicatorData(
                                        type = GestureIndicatorType.Volume,
                                        percent = percent,
                                        text = "${(percent * 100).toInt()}%",
                                    )
                                )
                            }
                        }
                        change.consume()
                    }
                }

                if (dragging) {
                    if (horizontal) {
                        currentOnSeekTo(targetPosition)
                    }
                    currentOnIndicator(null)
                }
            }
        }
}

private fun readCurrentBrightness(window: android.view.Window?): Float {
    val lp = window?.attributes
    if (lp != null && lp.screenBrightness >= 0) {
        return lp.screenBrightness * 255f
    }
    return try {
        Settings.System.getInt(
            window?.context?.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
        ).toFloat()
    } catch (_: Settings.SettingNotFoundException) {
        128f
    }
}
