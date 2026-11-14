package com.yenaly.han1meviewer.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.ui.preview.ComponentPreview
import com.yenaly.han1meviewer.ui.preview.fakeTagList2
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun TagChipGroup(
    tags: List<String>,
    modifier: Modifier = Modifier,
    onTagClick: ((String) -> Unit)? = null,
    collapsible: Boolean = false,
    collapsedMaxLines: Int = 2,
    headerTitle: String? = null,
) {
    require(!collapsible || collapsedMaxLines > 0)

    val tagColors = rememberTagChipColors(tags)
    var expanded by rememberSaveable(tags, collapsible, collapsedMaxLines) { mutableStateOf(false) }
    var chipContentHeightTarget by remember(
        tags,
        collapsible,
        collapsedMaxLines
    ) { mutableIntStateOf(0) }
    // 高度补间用 Animatable 手动驱动：首次出现直接 snap 到位——
    // animateIntAsState 的 0→h 入场动画会让所在列表在数据到达后的每一帧
    // 都重排（播放页进场的连续掉帧来源之一）；仅展开/收起时播放补间。
    val heightAnimatable = remember(tags, collapsible, collapsedMaxLines) {
        Animatable(-1f)
    }
    LaunchedEffect(chipContentHeightTarget) {
        if (chipContentHeightTarget <= 0) return@LaunchedEffect
        if (heightAnimatable.value < 0f) {
            heightAnimatable.snapTo(chipContentHeightTarget.toFloat())
        } else {
            heightAnimatable.animateTo(
                chipContentHeightTarget.toFloat(),
                animationSpec = tween(durationMillis = 240),
            )
        }
    }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "tag-arrow-rotation",
    )

    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val spacingX = 8.dp.roundToPx()
        val spacingY = 8.dp.roundToPx()
        val maxWidth =
            constraints.maxWidth.takeUnless { it == Constraints.Infinity } ?: Int.MAX_VALUE

        val chipPlaceables = subcompose(TagChipSlot.Chips) {
            tags.forEach { tag ->
                TagChip(
                    tag = tag,
                    onTagClick = onTagClick,
                    colors = tagColors[tag] ?: AssistChipDefaults.assistChipColors(),
                )
            }
        }.map { measurable -> measurable.measure(contentConstraints) }

        data class ChipPosition(
            val index: Int,
            val line: Int,
            val x: Int,
            val y: Int,
        )

        data class FlowLayoutResult(
            val positions: List<ChipPosition>,
            val lineHeights: List<Int>,
            val lineTops: List<Int>,
            val contentWidth: Int,
        ) {
            val lineCount: Int
                get() = lineHeights.size

            val height: Int
                get() = if (lineCount > 0) lineTops.last() + lineHeights.last() else 0
        }

        fun calculateFlowLayout(placeables: List<Placeable>): FlowLayoutResult {
            if (placeables.isEmpty()) {
                return FlowLayoutResult(
                    positions = emptyList(),
                    lineHeights = emptyList(),
                    lineTops = emptyList(),
                    contentWidth = 0,
                )
            }

            val positions = mutableListOf<ChipPosition>()
            val lineHeights = mutableListOf(0)
            val lineTops = mutableListOf(0)
            var line = 0
            var x = 0
            var y = 0

            placeables.forEachIndexed { index, placeable ->
                val shouldWrap =
                    x > 0 && maxWidth != Int.MAX_VALUE && x + placeable.width > maxWidth
                if (shouldWrap) {
                    y += lineHeights[line] + spacingY
                    line += 1
                    x = 0
                    lineHeights += 0
                    lineTops += y
                }

                positions += ChipPosition(index = index, line = line, x = x, y = y)
                lineHeights[line] = maxOf(lineHeights[line], placeable.height)
                x += placeable.width + spacingX
            }

            val contentWidth = positions.maxOfOrNull { position ->
                position.x + placeables[position.index].width
            } ?: 0

            return FlowLayoutResult(
                positions = positions,
                lineHeights = lineHeights,
                lineTops = lineTops,
                contentWidth = contentWidth,
            )
        }

        val chipLayout = calculateFlowLayout(chipPlaceables)
        val hasOverflow = collapsible && chipLayout.lineCount > collapsedMaxLines
        val togglePlaceable = if (hasOverflow) {
            subcompose(TagChipSlot.Toggle) {
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = arrowRotation
                        },
                    )

                }
            }.single().measure(contentConstraints)
        } else {
            null
        }

        val contentPlaceables: List<Placeable>
        val contentLayout: FlowLayoutResult

        if (togglePlaceable != null) {
            if (expanded) {
                contentPlaceables = chipPlaceables + togglePlaceable
                contentLayout = calculateFlowLayout(contentPlaceables)
            } else {
                // 二分查找折叠态最多能放下的 chip 数（线性逐个回退为 O(n²)，
                // 且「行数随 chip 数单调不减」，二分安全）
                var lo = 0
                var hi = chipPlaceables.size
                var bestCount = -1
                while (lo <= hi) {
                    val mid = (lo + hi) / 2
                    val candidatePlaceables = chipPlaceables.take(mid) + togglePlaceable
                    val candidateLayout = calculateFlowLayout(candidatePlaceables)
                    if (candidateLayout.lineCount <= collapsedMaxLines) {
                        bestCount = mid
                        lo = mid + 1
                    } else {
                        hi = mid - 1
                    }
                }
                contentPlaceables = if (bestCount >= 0) {
                    chipPlaceables.take(bestCount) + togglePlaceable
                } else {
                    listOf(togglePlaceable)
                }
                contentLayout = calculateFlowLayout(contentPlaceables)
            }
        } else {
            contentPlaceables = chipPlaceables
            contentLayout = chipLayout
        }

        val chipContentHeight = contentLayout.height
        if (chipContentHeightTarget != chipContentHeight) {
            chipContentHeightTarget = chipContentHeight
        }

        // 首次出现（动画尚未就位）直接用目标高度，之后用补间值
        val displayChipHeight = if (heightAnimatable.value >= 0f) {
            heightAnimatable.value.roundToInt()
        } else {
            chipContentHeight
        }

        val headerPlaceables = if (!headerTitle.isNullOrBlank()) {
            subcompose(TagChipSlot.Header) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleLarge,
                )
            }.map { measurable -> measurable.measure(contentConstraints) }
        } else {
            emptyList()
        }

        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val headerSpacing =
            if (headerPlaceables.isNotEmpty() && displayChipHeight > 0) spacingY else 0
        val contentWidth = contentLayout.contentWidth
        val headerWidth = headerPlaceables.maxOfOrNull { it.width } ?: 0
        val layoutWidth = (constraints.maxWidth.takeUnless { it == Constraints.Infinity }
            ?: maxOf(contentWidth, headerWidth))
            .coerceAtLeast(constraints.minWidth)
        val layoutHeight = (headerHeight + headerSpacing + displayChipHeight)
            .coerceAtLeast(constraints.minHeight)

        layout(layoutWidth, layoutHeight) {
            headerPlaceables.forEach { placeable ->
                placeable.placeRelative(0, 0)
            }
            val yOffset = headerHeight + headerSpacing
            contentLayout.positions.forEach { position ->
                contentPlaceables[position.index].placeRelative(position.x, position.y + yOffset)
            }
        }
    }
}

private enum class TagChipSlot {
    Header,
    Chips,
    Toggle,
}

@Composable
private fun TagChip(
    tag: String,
    onTagClick: ((String) -> Unit)?,
    colors: ChipColors,
) {
    AssistChip(
        onClick = { onTagClick?.invoke(tag) ?: Unit },
        label = { Text(tag) },
        colors = colors,
    )
}

@Composable
private fun rememberTagChipColors(tags: List<String>): Map<String, ChipColors> {
    val colorScheme = MaterialTheme.colorScheme
    val palette = remember(colorScheme) {
        listOf(
            colorScheme.primaryContainer to colorScheme.onPrimaryContainer,
            colorScheme.secondaryContainer to colorScheme.onSecondaryContainer,
            colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer,
            colorScheme.errorContainer to colorScheme.onErrorContainer,
            colorScheme.surfaceContainer to colorScheme.onSurface,
            colorScheme.surfaceContainerLow to colorScheme.onSurface,
            colorScheme.surfaceContainerHigh to colorScheme.onSurface,
            colorScheme.surfaceContainerHighest to colorScheme.onSurface,
            colorScheme.inversePrimary.copy(alpha = 0.28f) to colorScheme.onSurface,
            colorScheme.primary.copy(alpha = 0.18f) to colorScheme.primary,
            colorScheme.secondary.copy(alpha = 0.18f) to colorScheme.secondary,
            colorScheme.tertiary.copy(alpha = 0.18f) to colorScheme.tertiary,
            colorScheme.error.copy(alpha = 0.16f) to colorScheme.error,
        )
    }
    val assignedPalette = remember(tags, palette) {
        assignPaletteWithoutAdjacentRepeats(tags.distinct(), palette)
    }
    // assistChipColors 是 @Composable 工厂，无法整体 remember；
    // ChipColors 为轻量数据类，逐次创建开销可忽略。
    return assignedPalette.mapValues { (_, pair) ->
        AssistChipDefaults.assistChipColors(
            containerColor = pair.first,
            labelColor = pair.second,
        )
    }
}

private fun assignPaletteWithoutAdjacentRepeats(
    tags: List<String>,
    palette: List<Pair<Color, Color>>,
): Map<String, Pair<Color, Color>> {
    if (tags.isEmpty()) return emptyMap()
    val shuffled = palette.shuffled(Random(tags.joinToString("|").hashCode()))
    val result = LinkedHashMap<String, Pair<Color, Color>>(tags.size)
    var paletteIndex = 0
    var previous: Pair<Color, Color>? = null

    tags.forEach { tag ->
        var chosen = shuffled[paletteIndex % shuffled.size]
        if (chosen == previous && shuffled.size > 1) {
            chosen = shuffled[(paletteIndex + 1) % shuffled.size]
            paletteIndex += 1
        }
        result[tag] = chosen
        previous = chosen
        paletteIndex += 1
    }
    return result
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun TagChipGroupPreview() {
    ComponentPreview {
        TagChipGroup(
            tags = fakeTagList2,
            collapsible = true,
            headerTitle = "TAGs",
        )
    }
}
