package com.yenaly.han1meviewer.ui.component

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.media3.common.VideoSize
import kotlin.math.roundToInt

private const val INLINE_DEFAULT_ASPECT = 16f / 9f
private const val INLINE_MAX_SCREEN_FRACTION = 0.6f

@Composable
fun Modifier.inlinePlayerSize(videoAspect: Float): Modifier {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * INLINE_MAX_SCREEN_FRACTION).dp
    val aspect = videoAspect.takeIf { it.isFinite() && it > 0f } ?: INLINE_DEFAULT_ASPECT
    return layout { measurable, constraints ->
        val width = constraints.maxWidth
        val height = minOf((width / aspect).roundToInt(), maxHeight.roundToPx())
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(width, height) { placeable.place(0, 0) }
    }
}

class VideoFitState {
    var aspect: Float = 0f
    var stretched: Boolean = false

    fun updateFrom(size: VideoSize) {
        aspect = if (size.width > 0 && size.height > 0) {
            size.width * size.pixelWidthHeightRatio / size.height
        } else 0f
    }
}

fun fitVideoSurface(parent: ViewGroup, surface: View, state: VideoFitState) {
    val w = parent.width
    val h = parent.height
    if (w <= 0 || h <= 0) return
    var targetW = w
    var targetH = h
    val aspect = state.aspect
    if (!state.stretched && aspect > 0f) {
        if (w.toFloat() / h > aspect) {
            targetW = (h * aspect).toInt()
        } else {
            targetH = (w / aspect).toInt()
        }
    }
    val lp = (surface.layoutParams as? FrameLayout.LayoutParams)
        ?: FrameLayout.LayoutParams(targetW, targetH)
    if (lp.width != targetW || lp.height != targetH || lp.gravity != Gravity.CENTER) {
        lp.width = targetW
        lp.height = targetH
        lp.gravity = Gravity.CENTER
        surface.layoutParams = lp
    }
}

fun installVideoFit(parent: FrameLayout, surface: View, state: VideoFitState) {
    val settle = Runnable {
        fitVideoSurface(parent, surface, state)
        applyStaleSurfaceLayout(parent, surface)
    }
    parent.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        fitVideoSurface(parent, surface, state)
        parent.removeCallbacks(settle)
        parent.post(settle)
    }
}

private fun applyStaleSurfaceLayout(parent: ViewGroup, surface: View) {
    if (surface.parent !== parent) return
    val lp = surface.layoutParams ?: return
    if (lp.width <= 0 || lp.height <= 0) return
    val left = (parent.width - lp.width) / 2
    val top = (parent.height - lp.height) / 2
    if (surface.width == lp.width && surface.height == lp.height &&
        surface.left == left && surface.top == top
    ) return
    surface.measure(
        View.MeasureSpec.makeMeasureSpec(lp.width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(lp.height, View.MeasureSpec.EXACTLY),
    )
    surface.layout(left, top, left + lp.width, top + lp.height)
}
