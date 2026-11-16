package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.R

@Composable
internal fun DisclaimerGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun DisclaimerRow(
    icon: ImageVector,
    title: String,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HighlightDisclaimerCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.errorContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.25f)),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AttentionIcon(icon = Icons.Rounded.Warning, tint = scheme.error)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onErrorContainer,
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = scheme.onErrorContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun AttentionIcon(icon: ImageVector, tint: Color) {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pulse.animateTo(1f, tween(durationMillis = 1600, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val scale = 1f + 0.8f * pulse.value
                    scaleX = scale
                    scaleY = scale
                    alpha = 0.35f * (1f - pulse.value)
                }
                .background(tint, CircleShape),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(tint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun AboutSupportNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 3.dp)
                .size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun AboutThanks(text: String) {
    val scheme = MaterialTheme.colorScheme
    val revealState = rememberRevealOnVisible()
    val reveal = revealState.progress

    val lineColor = scheme.outline.copy(alpha = 0.55f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp, bottom = 36.dp)
            .then(revealState.trigger),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(1.dp)
                    .graphicsLayer {
                        scaleX = reveal.value
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    }
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, lineColor))),
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        rotationZ = 45f
                        alpha = reveal.value
                        scaleX = reveal.value
                        scaleY = reveal.value
                    }
                    .background(scheme.primary.copy(alpha = 0.75f)),
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(1.dp)
                    .graphicsLayer {
                        scaleX = reveal.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(Brush.horizontalGradient(listOf(lineColor, Color.Transparent))),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = ((reveal.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
                },
        )
    }
}

@Stable
internal class RevealOnVisible(
    private val played: MutableState<Boolean>,
    val progress: Animatable<Float, AnimationVector1D>,
    private val minVisibleFraction: Float,
) {
    val trigger: Modifier = Modifier.onGloballyPositioned { coordinates ->
        if (!played.value && coordinates.isAttached && coordinates.size.height > 0) {
            val visibleFraction =
                coordinates.boundsInWindow().height / coordinates.size.height
            if (visibleFraction >= minVisibleFraction) played.value = true
        }
    }
}

@Composable
internal fun rememberRevealOnVisible(): RevealOnVisible {
    val played = rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played.value) 1f else 0f) }

    LaunchedEffect(played.value) {
        if (played.value) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1000,
                    delayMillis = 100,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    return remember { RevealOnVisible(played, progress, 0.6f) }
}

@Composable
internal fun AboutHeaderCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_new),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.hanime_app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.about_app_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}
