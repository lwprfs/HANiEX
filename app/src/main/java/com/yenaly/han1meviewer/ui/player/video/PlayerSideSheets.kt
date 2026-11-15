package com.yenaly.han1meviewer.ui.player.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.player.PlayerDefaults
import com.yenaly.han1meviewer.ui.player.PlayerUtils
import kotlin.math.roundToInt

enum class PlayerSheetType {

    Speed,

    Quality,

    SuperResolution,

    Keyframes,
}

@Composable
private fun BubbleOptionItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.White.copy(alpha = 0.88f)
            },
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun PlayerBubblePopup(
    anchorX: Float,
    anchorEdgeY: Float,
    preferAbove: Boolean,
    playerWidthPx: Int,
    itemCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxVisibleItems: Int = 6,
    content: @Composable () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bubbleWidth = 172.dp
    val arrowSize = 10.dp
    val margin = 8.dp
    val verticalPad = 10.dp

    with(density) {
        val widthPx = bubbleWidth.toPx()
        val arrowPx = arrowSize.toPx()
        val marginPx = margin.toPx()
        val visibleRows = itemCount.coerceAtMost(maxVisibleItems)
        val heightPx = visibleRows * ITEM_HEIGHT.toPx() + verticalPad.toPx() * 2

        val leftPx = (anchorX - widthPx / 2)
            .coerceIn(marginPx, (playerWidthPx - widthPx - marginPx).coerceAtLeast(marginPx))

        val topPx = if (preferAbove) {
            anchorEdgeY - arrowPx - heightPx
        } else {
            anchorEdgeY + arrowPx
        }

        val triangleCenterPx = (anchorX - leftPx)
            .coerceIn(arrowPx + 6f, widthPx - arrowPx - 6f)

        Box(modifier = modifier) {
            Surface(
                modifier = Modifier
                    .offsetPx(IntOffset(leftPx.roundToInt(), topPx.roundToInt()))
                    .width(bubbleWidth),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xF0101014),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color.White.copy(alpha = 0.08f),
                ),
            ) {
                Column(modifier = Modifier.padding(vertical = verticalPad)) {

                    Box(
                        modifier = Modifier
                            .heightIn(max = ITEM_HEIGHT * maxVisibleItems)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Column {
                            content()
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offsetPx(
                        IntOffset(
                            (leftPx + triangleCenterPx - arrowPx).roundToInt(),
                            if (preferAbove) {
                                (topPx + heightPx).roundToInt()
                            } else {
                                (topPx - arrowPx).roundToInt()
                            },
                        )
                    )
                    .size(arrowSize)
                    .rotate(45f)
                    .background(Color(0xF0101014)),
            )
        }
    }
}

internal val ITEM_HEIGHT = 38.dp

private fun Modifier.offsetPx(position: IntOffset): Modifier =
    this.then(Modifier.offset { position })

@Composable
fun BubbleDismissScrim(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            },
    )
}

@Composable
internal fun SpeedBubble(
    currentSpeedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    PlayerDefaults.speedStringArray.forEachIndexed { index, item ->
        BubbleOptionItem(
            text = item,
            selected = index == currentSpeedIndex,
            onClick = { onSelect(index) },
        )
    }
}

@Composable
internal fun QualityBubble(
    qualities: List<String>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
) {
    qualities.forEachIndexed { index, item ->
        BubbleOptionItem(
            text = item,
            selected = index == currentIndex,
            onClick = { onSelect(index) },
        )
    }
}

@Composable
internal fun SuperResolutionBubble(
    currentIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val options = listOf(
        stringResource(R.string.super_resolution_off),
        stringResource(R.string.super_resolution_performance),
        stringResource(R.string.super_resolution_quality),
    )
    options.forEachIndexed { index, item ->
        BubbleOptionItem(
            text = item,
            selected = index == currentIndex,
            onClick = { onSelect(index) },
        )
    }
}

@Composable
fun PlayerSideSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            AnimatedVisibility(
                visible = true,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                Surface(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight(),
                    color = Color.Black.copy(alpha = 0.72f),
                ) {
                    Box(modifier = Modifier.padding(vertical = 48.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
internal fun KeyframesSheet(
    keyframes: HKeyframeEntity?,
    isLocalKeyframes: Boolean,
    onSeek: (HKeyframeEntity.Keyframe) -> Unit,
    onEdit: (HKeyframeEntity.Keyframe) -> Unit,
    onDelete: (HKeyframeEntity.Keyframe) -> Unit,
) {
    val list = keyframes?.keyframes.orEmpty()
    if (list.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.here_is_empty),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.long_press_to_add_h_keyframe),
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }

    LazyColumn {
        itemsIndexed(list, key = { _, item -> item.position }) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(item) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = PlayerUtils.stringForTime(item.position),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "#${index + 1}",
                            color = Color.White.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    item.prompt?.takeIf { it.isNotBlank() }?.let { prompt ->
                        Text(
                            text = "➥ $prompt",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (isLocalKeyframes) {
                    IconButton(onClick = { onEdit(item) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.modify_h_keyframe),
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.sure_to_delete),
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
