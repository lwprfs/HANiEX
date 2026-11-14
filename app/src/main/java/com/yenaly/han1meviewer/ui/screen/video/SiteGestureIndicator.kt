package com.yenaly.han1meviewer.ui.screen.video

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Brightness7
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 站点播放器（MissAV / OnAir / FapFolder 等）使用的手势指示浮层。
 *
 * 上游把 Hanime 播放器迁移到 Compose 后删除了 VideoPlayerUi.kt，
 * 其中的该浮层仍被各站点播放器引用，故单独保留在此。
 * （Hanime 播放器自身使用 ui.player.video.GestureIndicatorOverlay。）
 */
enum class GestureIndicatorType {
    Brightness,
    Volume,
    Progress,
}

@Composable
fun GestureIndicatorOverlay(
    visible: Boolean,
    type: GestureIndicatorType,
    percent: Float,
    modifier: Modifier = Modifier,
    text: String = "${(percent * 100).toInt()}%"
) {

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(
            initialScale = 0.92f
        ),
        exit = fadeOut() + scaleOut(
            targetScale = 0.92f
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(
                        width = 170.dp,
                        height = 190.dp
                    )
                    .clip(RoundedCornerShape(36.dp))
            ) {

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.graphicsLayer {
                                    renderEffect =
                                        RenderEffect
                                            .createBlurEffect(
                                                55f,
                                                55f,
                                                Shader.TileMode.CLAMP
                                            )
                                            .asComposeRenderEffect()
                                }
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            Color.Black.copy(alpha = 0.32f)
                        )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.04f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(36.dp)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 22.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Icon(
                        imageVector = when (type) {
                            GestureIndicatorType.Brightness -> Icons.Outlined.Brightness7
                            GestureIndicatorType.Volume -> Icons.AutoMirrored.Outlined.VolumeUp
                            GestureIndicatorType.Progress -> Icons.Outlined.FastForward
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = when (type) {
                            GestureIndicatorType.Brightness -> "亮度"
                            GestureIndicatorType.Volume -> "音量"
                            GestureIndicatorType.Progress -> "进度"
                        },
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    LinearProgressIndicator(
                        progress = { percent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100)),
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}
