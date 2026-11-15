package com.yenaly.han1meviewer.ui.player.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Brightness7
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.R

@Composable
fun GestureIndicatorOverlay(
    data: GestureIndicatorData?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = data != null,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.95f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f),
    ) {
        val current = data ?: return@AnimatedVisibility
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .width(148.dp)
                    .clip(RoundedCornerShape(20.dp))

                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = when (current.type) {
                            GestureIndicatorType.Brightness -> Icons.Outlined.Brightness7
                            GestureIndicatorType.Volume -> Icons.AutoMirrored.Outlined.VolumeUp
                            GestureIndicatorType.Progress -> Icons.Outlined.FastForward
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = when (current.type) {
                            GestureIndicatorType.Brightness -> stringResource(R.string.gesture_brightness)
                            GestureIndicatorType.Volume -> stringResource(R.string.gesture_volume)
                            GestureIndicatorType.Progress -> stringResource(R.string.gesture_progress)
                        },
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = current.text,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                    )
                    current.subtitle?.let { subtitle ->
                        Text(
                            text = "/ $subtitle",
                            color = Color.White.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(100))
                        .background(Color.White.copy(alpha = 0.16f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(current.percent.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(100))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    )
                                )
                            ),
                    )
                }
            }
        }
    }
}
