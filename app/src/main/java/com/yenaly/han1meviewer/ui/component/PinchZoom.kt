package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

/**
 * Pinch-to-zoom + pan for fullscreen image viewers.
 *
 * The handler is always attached, so a pinch works from normal size (previously it only
 * attached once the image was already zoomed, which made pinching impossible). With one finger at
 * 1x nothing is consumed, so a surrounding pager can still swipe between images.
 */
fun Modifier.pinchZoomGestures(
    key: Any?,
    scale: () -> Float,
    offset: () -> Offset,
    maxScale: Float = 5f,
    onChange: (scale: Float, offset: Offset) -> Unit,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val multiTouch = event.changes.count { it.pressed } > 1
            if (multiTouch || scale() > 1.01f) {
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                val newScale = (scale() * zoom).coerceIn(1f, maxScale)
                val newOffset = if (newScale <= 1.01f) {
                    Offset.Zero
                } else {
                    val maxX = size.width * (newScale - 1f) / 2f
                    val maxY = size.height * (newScale - 1f) / 2f
                    Offset(
                        (offset().x + pan.x).coerceIn(-maxX, maxX),
                        (offset().y + pan.y).coerceIn(-maxY, maxY),
                    )
                }
                onChange(if (newScale <= 1.01f) 1f else newScale, newOffset)
                event.changes.forEach { c: PointerInputChange -> if (c.positionChanged()) c.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}
