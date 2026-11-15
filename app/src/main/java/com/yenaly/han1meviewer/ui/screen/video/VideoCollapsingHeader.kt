package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

private const val FLING_FRICTION_PER_SECOND = 2f

@Composable
fun VideoCollapsingHeader(
    headerHeightPx: Int,
    scrollEnabled: Boolean,
    initialCollapsed: Boolean,
    onCollapseRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {

    var collapsedRatio by remember {
        mutableFloatStateOf(if (initialCollapsed) 1f else 0f)
    }

    val maxCollapse = headerHeightPx.toFloat()

    val lockAnimatable = remember { Animatable(if (initialCollapsed) 1f else 0f) }
    val currentScrollEnabled by rememberUpdatedState(scrollEnabled)
    LaunchedEffect(scrollEnabled) {
        if (!scrollEnabled && collapsedRatio > 0f) {
            lockAnimatable.snapTo(collapsedRatio)
            lockAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 220))

            collapsedRatio = 0f
        }
    }

    val effectiveRatio = remember {
        derivedStateOf {
            if (currentScrollEnabled) {
                collapsedRatio.coerceIn(0f, 1f)
            } else {
                lockAnimatable.value.coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { effectiveRatio.value }.collect { onCollapseRatioChange(it) }
    }

    fun consumeFold(delta: Float): Float {
        val prev = collapsedRatio
        val next = (prev - delta / maxCollapse).coerceIn(0f, 1f)
        if (next != prev) {
            collapsedRatio = next

            return (prev - next) * maxCollapse
        }
        return 0f
    }

    val minFlingVelocityPx = with(LocalDensity.current) { 50.dp.toPx() }

    suspend fun flingCollapse(velocityY: Float): Velocity {
        var velocity = velocityY
        var lastFrame = -1L
        while (abs(velocity) >= minFlingVelocityPx &&
            !((collapsedRatio >= 1f && velocity < 0f) || (collapsedRatio <= 0f && velocity > 0f))
        ) {
            val frame = withFrameNanos { it }
            if (lastFrame >= 0L) {
                val dt = ((frame - lastFrame) / 1_000_000_000f).coerceIn(0.001f, 0.032f)
                val dy = velocity * dt
                val consumed = consumeFold(dy)
                velocity *= exp(-FLING_FRICTION_PER_SECOND * dt)
                if (abs(consumed) < abs(dy) - 0.5f) {

                    velocity *= (1f - abs(consumed) / abs(dy)).coerceIn(0f, 1f)
                    break
                }
            }
            lastFrame = frame
        }
        return Velocity(0f, velocity)
    }

    val nestedScrollConnection = remember(scrollEnabled, maxCollapse) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!scrollEnabled) return Offset.Zero
                val dy = available.y
                return when {

                    dy < 0 -> Offset(0f, consumeFold(dy))

                    dy > 0 && collapsedRatio > 0f -> Offset(0f, consumeFold(dy))

                    else -> Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!scrollEnabled) return Offset.Zero
                val dy = available.y

                return if (dy > 0) Offset(0f, consumeFold(dy)) else Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!scrollEnabled || available.y == 0f) return Velocity.Zero

                val leftover = flingCollapse(available.y)
                return available - leftover
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val ratio = effectiveRatio.value
                    val h = (maxCollapse * (1f - ratio)).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(
                        constraints.copy(minHeight = h, maxHeight = h),
                    )
                    layout(placeable.width, h) { placeable.place(0, 0) }
                }
                .clipToBounds(),
        ) {
            headerContent()
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(nestedScrollConnection),
        ) {
            content()
        }
    }
}
