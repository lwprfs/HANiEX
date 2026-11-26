package com.yenaly.han1meviewer.ui.component.social

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Profile / community / channel layout whose [header] scrolls away together with the
 * list in [content] instead of staying pinned at the top.
 *
 * - [header]: big profile block. Collapses while the list below is scrolled up and
 *   expands again once the list is back at its first item.
 * - [pinned]: small bar (usually the tab row). It stays visible under the top bar.
 * - [content]: any lazy list / grid. It must scroll on its own.
 */
@Composable
fun CollapsingProfileLayout(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    pinned: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var offsetPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(headerHeightPx) {
        offsetPx = offsetPx.coerceIn(-headerHeightPx.toFloat(), 0f)
    }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy >= 0f) return Offset.Zero
                val target = (offsetPx + dy).coerceIn(-headerHeightPx.toFloat(), 0f)
                val used = target - offsetPx
                offsetPx = target
                return Offset(0f, used)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val dy = available.y
                if (dy <= 0f) return Offset.Zero
                val target = (offsetPx + dy).coerceIn(-headerHeightPx.toFloat(), 0f)
                val used = target - offsetPx
                offsetPx = target
                return Offset(0f, used)
            }
        }
    }

    Layout(
        modifier = modifier
            .clipToBounds()
            .nestedScroll(connection),
        content = {
            Box(Modifier.onSizeChanged { headerHeightPx = it.height }) { header() }
            Box { pinned() }
            Box { content() }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val width = constraints.maxWidth
        val fixedWidth = loose.copy(minWidth = width, maxWidth = width)
        val headerP = measurables[0].measure(fixedWidth)
        val pinnedP = measurables[1].measure(fixedWidth)
        val offset = offsetPx.roundToInt().coerceIn(-headerP.height, 0)
        val contentTop = headerP.height + offset + pinnedP.height
        val height = constraints.maxHeight
        val contentHeight = (height - contentTop).coerceAtLeast(0)
        val contentP = measurables[2].measure(
            Constraints(
                minWidth = width,
                maxWidth = width,
                minHeight = contentHeight,
                maxHeight = contentHeight,
            )
        )
        layout(width, height) {
            headerP.placeRelative(0, offset)
            pinnedP.placeRelative(0, headerP.height + offset)
            contentP.placeRelative(0, contentTop)
        }
    }
}
