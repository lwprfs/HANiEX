package com.yenaly.han1meviewer.XHamster.ui.common

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.RetryableImage

@Composable
fun XHamsterCommentCard(
    author: String,
    avatarUrl: String?,
    text: String,
    modifier: Modifier = Modifier,
    createdSeconds: Long? = null,
    dateText: String? = null,
    likes: Long = 0L,
    liked: Boolean = false,
    isReply: Boolean = false,
    replyToName: String? = null,
    badge: String? = null,
    imageLoader: coil3.ImageLoader? = null,
    onAuthorClick: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 24.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val avatarModifier = Modifier
                    .size(if (isReply) 34.dp else 40.dp)
                    .clip(CircleShape)
                if (!avatarUrl.isNullOrBlank() && imageLoader != null) {
                    RetryableImage(
                        model = avatarUrl,
                        modifier = avatarModifier,
                        placeholder = painterResource(R.drawable.h_chan_loading),
                        error = painterResource(R.drawable.h_chan_load_failed),
                        contentScale = ContentScale.Crop,
                        imageLoader = imageLoader,
                    )
                } else {
                    Surface(modifier = avatarModifier, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                author.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = author,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .then(if (onAuthorClick != null) Modifier.clickable(onClick = onAuthorClick) else Modifier),
                        )
                        if (badge != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                Text(
                                    badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    val time = dateText ?: createdSeconds?.let {
                        DateUtils.getRelativeTimeSpanString(
                            it * 1000, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                        ).toString()
                    }
                    if (time != null) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_delete_24),
                            contentDescription = stringResource(R.string.xh_comment_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else if (onReport != null) {
                    IconButton(onClick = onReport, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_report_24),
                            contentDescription = stringResource(R.string.xh_comment_report),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (!replyToName.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video_reply_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.xh_replying_to, replyToName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SelectionContainer {
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }

            if (onLike != null || onReply != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onLike != null) {
                        TextButton(onClick = onLike) {
                            Icon(
                                painter = painterResource(
                                    if (liked) R.drawable.ic_baseline_thumb_up_alt_24
                                    else R.drawable.ic_baseline_thumb_up_off_alt_24,
                                ),
                                contentDescription = stringResource(R.string.xh_comment_like),
                            )
                            if (likes > 0) Text(" $likes")
                        }
                    }
                    if (onReply != null) {
                        TextButton(onClick = onReply) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_reply_24),
                                contentDescription = null,
                            )
                            Text(" " + stringResource(R.string.xh_reply))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun XHamsterCommentComposer(
    replyingTo: String?,
    sending: Boolean,
    enabled: Boolean,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNeedLogin: () -> Unit = {},
) {
    var text by rememberSaveable { mutableStateOf("") }
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 3.dp) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (replyingTo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.xh_replying_to, replyingTo),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(28.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_close_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.xh_comment_hint)) },
                    enabled = enabled && !sending,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .then(if (!enabled) Modifier.clickable(onClick = onNeedLogin) else Modifier),
                )
                if (sending) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(
                        onClick = {
                            val value = text.trim()
                            if (value.isNotEmpty()) {
                                onSend(value)
                                text = ""
                            }
                        },
                        enabled = enabled && text.isNotBlank(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_send_24),
                            contentDescription = stringResource(R.string.xh_comment_send),
                            tint = if (enabled && text.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
