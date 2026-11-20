package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.RetryableImage

@Composable
fun SiteAvatar(
    url: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader? = null,
) {
    if (!url.isNullOrBlank()) {
        RetryableImage(
            model = url,
            modifier = modifier.size(size).clip(CircleShape),
            placeholder = painterResource(R.drawable.h_chan_loading),
            error = painterResource(R.drawable.h_chan_load_failed),
            contentScale = ContentScale.Crop,
            imageLoader = imageLoader,
        )
    } else {
        Surface(
            modifier = modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
fun SiteVerifiedBadge(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_video_verified_24),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(size),
    )
}

@Composable
fun SiteSubscribeButton(
    subscribed: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(if (subscribed) R.string.site_subscribed else R.string.site_subscribe)
    val icon = painterResource(
        if (subscribed) R.drawable.ic_video_subscribed_24 else R.drawable.ic_video_subscribe_24
    )
    val padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    if (subscribed) {
        FilledTonalButton(onClick = onClick, enabled = !busy, modifier = modifier, contentPadding = padding) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(onClick = onClick, enabled = !busy, modifier = modifier, contentPadding = padding) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
