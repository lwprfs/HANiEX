package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.player.LocalPlayerCompact
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val PARALLAX_FACTOR = 0.5f
private const val SCRIM_MAX_ALPHA = 0.65f
private const val SNAP_VELOCITY = 400f

private class CollapseState(private val scope: CoroutineScope) {
    var heightPx by mutableFloatStateOf(0f)
    var offsetPx by mutableFloatStateOf(0f)
    private var job: Job? = null

    fun cancelAnimation() {
        job?.cancel()
    }

    fun consume(dy: Float): Float {
        if (heightPx <= 0f) return 0f
        val old = offsetPx
        offsetPx = (old + dy).coerceIn(-heightPx, 0f)
        return offsetPx - old
    }

    fun animateTo(target: Float, velocity: Float = 0f) {
        job?.cancel()
        job = scope.launch {
            animate(
                initialValue = offsetPx,
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) { value, _ ->
                offsetPx = value.coerceIn(-heightPx, 0f)
            }
        }
    }

    fun settle(vy: Float): Boolean {
        val h = heightPx
        if (h <= 0f) return false
        val off = offsetPx
        if (vy < -SNAP_VELOCITY && off > -h) {
            animateTo(-h, vy)
            return true
        }
        if (off < 0f && off > -h) {
            val target = when {
                vy > SNAP_VELOCITY -> 0f
                off < -h / 2f -> -h
                else -> 0f
            }
            animateTo(target, vy)
            return true
        }
        return false
    }
}

@Composable
fun CollapsingPlayerLayout(
    modifier: Modifier = Modifier,
    pip: Boolean = false,
    pinEnabled: Boolean = false,
    pinActive: Boolean = false,
    onPinClose: () -> Unit = {},
    player: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = remember { CollapseState(scope) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var pageResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            pageResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var dismissed by remember { mutableStateOf(false) }
    LaunchedEffect(pinActive) {
        if (!pinActive) dismissed = false
    }
    val collapsed by remember {
        derivedStateOf { state.heightPx > 0f && state.offsetPx <= -state.heightPx + 1f }
    }
    val showMini = pinEnabled && pinActive && collapsed && !dismissed && !pip && pageResumed

    val connection = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy != 0f) state.cancelAnimation()

                if (dy < 0f) return Offset(0f, state.consume(dy))
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {

                if (available.y > 0f) return Offset(0f, state.consume(available.y))
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (state.settle(available.y)) Velocity(0f, available.y) else Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0f && state.offsetPx < 0f) {
                    state.animateTo(0f, available.y)
                    return Velocity(0f, available.y)
                }
                return Velocity.Zero
            }
        }
    }

    val dragState = rememberDraggableState { delta ->
        state.cancelAnimation()
        state.consume(delta)
    }

    Layout(
        modifier = modifier
            .then(if (pip) Modifier.background(Color.Black) else Modifier)
            .clipToBounds()
            .nestedScroll(connection),
        content = {
            Box(
                modifier = Modifier.draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    enabled = !showMini && !pip,
                    onDragStopped = { velocity -> state.settle(velocity) },
                ),
            ) {
                CompositionLocalProvider(LocalPlayerCompact provides showMini) { player() }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val h = state.heightPx
                        val fraction = if (h > 0f) (-state.offsetPx / h).coerceIn(0f, 1f) else 0f
                        alpha = fraction * SCRIM_MAX_ALPHA
                    }
                    .background(Color.Black),
            )
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                content()
            }
            Box {
                if (showMini) {
                    val closeDescription = stringResource(R.string.player_pin_close)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                dismissed = true
                                onPinClose()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = closeDescription,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        if (pip) {

            val pipPlayer = measurables[0].measure(
                Constraints(minWidth = width, maxWidth = width, minHeight = 0, maxHeight = height),
            )
            val zero = Constraints.fixed(0, 0)
            val pipScrim = measurables[1].measure(zero)
            val pipList = measurables[2].measure(zero)
            val pipClose = measurables[3].measure(zero)
            return@Layout layout(width, height) {
                pipList.place(0, 0)
                pipScrim.place(0, 0)
                pipClose.place(0, 0)
                pipPlayer.place(0, ((height - pipPlayer.height) / 2).coerceAtLeast(0))
            }
        }

        if (showMini) {
            val margin = 8.dp.roundToPx()
            val miniWidth = (width * 0.5f).roundToInt().coerceAtLeast(1)
            val miniHeight = (miniWidth * 9f / 16f).roundToInt().coerceAtLeast(1)
            val miniPlayer = measurables[0].measure(
                Constraints(
                    minWidth = miniWidth,
                    maxWidth = miniWidth,
                    minHeight = 0,
                    maxHeight = miniHeight,
                ),
            )
            val miniScrim = measurables[1].measure(Constraints.fixed(0, 0))
            val miniList = measurables[2].measure(
                Constraints(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height),
            )
            val miniClose = measurables[3].measure(Constraints())
            val playerX = (width - miniPlayer.width - margin).coerceAtLeast(0)
            return@Layout layout(width, height) {
                miniList.place(0, 0, 0f)
                miniScrim.place(0, 0, 0f)
                miniPlayer.place(playerX, margin, 1f)
                miniClose.place(
                    (playerX + miniPlayer.width - miniClose.width - margin / 2).coerceAtLeast(0),
                    margin + margin / 2,
                    2f,
                )
            }
        }

        val playerPlaceable = measurables[0].measure(
            Constraints(minWidth = width, maxWidth = width, minHeight = 0, maxHeight = height),
        )

        val ph = playerPlaceable.height
        state.heightPx = ph.toFloat()
        val offset = state.offsetPx.coerceIn(-ph.toFloat(), 0f).roundToInt()
        val visiblePlayer = (ph + offset).coerceAtLeast(0)
        val listHeight = (height - visiblePlayer).coerceAtLeast(0)

        val scrimPlaceable = measurables[1].measure(Constraints.fixed(width, ph))
        val closePlaceable = measurables[3].measure(Constraints.fixed(0, 0))

        val listPlaceable = measurables[2].measure(
            Constraints(
                minWidth = width,
                maxWidth = width,
                minHeight = listHeight,
                maxHeight = listHeight,
            ),
        )

        layout(width, height) {

            val parallaxY = (offset * PARALLAX_FACTOR).roundToInt()
            playerPlaceable.place(0, parallaxY)
            scrimPlaceable.place(0, parallaxY)
            listPlaceable.place(0, visiblePlayer)
            closePlaceable.place(0, 0)
        }
    }
}
