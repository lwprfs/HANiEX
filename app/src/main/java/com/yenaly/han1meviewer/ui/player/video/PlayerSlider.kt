package com.yenaly.han1meviewer.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerSlider(
    value: Float,
    buffered: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val sliderState = remember { SliderState(value.coerceIn(0f, 1f)) }

    LaunchedEffect(value) {
        if (!sliderState.isDragging) {
            sliderState.value = value.coerceIn(0f, 1f)
        }
    }

    Slider(
        state = sliderState,
        onValueChange = { onValueChange(it) },
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,

        thumb = {
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color.White.copy(alpha = 0.22f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White, CircleShape)
                )
            }
        },

        track = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100))
                        .background(Color.White.copy(alpha = 0.14f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(buffered.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(100))
                        .background(Color.White.copy(alpha = 0.32f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(value.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(100))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                                )
                            )
                        )
                )
            }
        }
    )
}
