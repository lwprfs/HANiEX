package com.yenaly.han1meviewer.ui.player.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import java.util.Locale

@Composable
fun KeyframeCountdownOverlay(
    positionMs: Long,
    keyframes: HKeyframeEntity?,
    remindAheadMs: Int,
    showComment: Boolean,
    modifier: Modifier = Modifier,
) {
    val countdown = remember(positionMs, keyframes, remindAheadMs) {
        keyframes?.keyframes?.forEachIndexed index@{ index, kf ->
            val interval = kf.position - positionMs
            if (interval in 0 until remindAheadMs) {
                val timeText = if (interval / 1_000L >= 1) {
                    (interval / 1_000L + 1).toString()
                } else {
                    String.format(Locale.US, "%.1f", interval / 1_000f)
                }
                return@remember Countdown(
                    kfIndexLabel = "#${index + 1}",
                    prompt = kf.prompt,
                    timeText = timeText,
                )
            }
        }
        null
    }

    AnimatedVisibility(
        visible = countdown != null,
        modifier = modifier,
    ) {
        val current = countdown ?: return@AnimatedVisibility
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showComment) {
                    val label = buildString {
                        append(current.kfIndexLabel)
                        current.prompt?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                    }
                    if (label.isNotBlank()) {
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = current.timeText,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private data class Countdown(
    val kfIndexLabel: String,
    val prompt: String?,
    val timeText: String,
)
