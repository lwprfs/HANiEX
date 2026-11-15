package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.yenaly.han1meviewer.R

@Composable
fun AccountAvatar(
    avatarUrl: String?,
    name: String?,
    loggedIn: Boolean,
    modifier: Modifier = Modifier,

    signInAvailable: Boolean = true,
) {
    val url = avatarUrl?.takeIf { it.isNotBlank() }
    if (loggedIn && url != null) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            loading = { GeneratedAvatar(name, loggedIn = true) },
            error = { GeneratedAvatar(name, loggedIn = true) },
        )
    } else if (!loggedIn && !signInAvailable) {
        Image(
            painter = painterResource(R.drawable.bg_default_header),
            contentDescription = null,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier.clip(CircleShape)) {
            GeneratedAvatar(name, loggedIn)
        }
    }
}

private val AvatarPalette = listOf(
    Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
    Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4DD0E1), Color(0xFF4DB6AC),
    Color(0xFF81C784), Color(0xFFFFB74D), Color(0xFFFF8A65), Color(0xFFA1887F),
)

@Composable
private fun GeneratedAvatar(name: String?, loggedIn: Boolean) {
    val clean = name?.trim().orEmpty()
    val initial = clean.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
    if (!loggedIn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(0.55f),
            )
        }
        return
    }
    val color = AvatarPalette[Math.floorMod(clean.lowercase().hashCode(), AvatarPalette.size)]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.55f),
            )
        }
    }
}
