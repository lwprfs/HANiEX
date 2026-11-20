package com.yenaly.han1meviewer.ui.component.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteAvatar
import com.yenaly.han1meviewer.ui.component.SiteCommentCard
import com.yenaly.han1meviewer.ui.component.SiteCommentComposer
import com.yenaly.han1meviewer.ui.component.SiteSubscribeButton
import com.yenaly.han1meviewer.ui.component.SiteVerifiedBadge
import com.yenaly.han1meviewer.util.compactCount

internal data class ShortsCommentsState(
    val items: List<SiteShortComment> = emptyList(),
    val cursor: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val loaded: Boolean = false,
    val total: Long? = null,
)

@Composable
internal fun ShortsCircleButton(
    onClick: () -> Unit,
    scrim: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp).background(scrim, CircleShape),
    ) { content() }
}

@Composable
private fun ShortsRailButton(
    onClick: () -> Unit,
    label: String?,
    scrim: Color,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ShortsCircleButton(onClick = onClick, scrim = scrim, content = content)
        if (!label.isNullOrEmpty()) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun ShortsOwnerRow(
    owner: SiteShortOwner,
    imageLoader: ImageLoader?,
    scrim: Color,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = scrim) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SiteAvatar(owner.avatarUrl, owner.name, size = 40.dp, imageLoader = imageLoader)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = owner.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (owner.verified) SiteVerifiedBadge()
                }
                val subs = owner.subscribers.compactCount()
                val kind = stringResource(
                    if (owner.isChannel) R.string.site_channel else R.string.site_uploader
                )
                Text(
                    text = if (subs != null) {
                        "$kind  ·  " + stringResource(R.string.site_n_subscribers, subs)
                    } else kind,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (owner.canSubscribe) {
                SiteSubscribeButton(owner.subscribed, owner.subscribeBusy, onSubscribe)
            }
        }
    }
}

@Composable
internal fun ShortsActionRail(
    meta: SiteShortMeta?,
    canRate: Boolean,
    canFavorite: Boolean,
    canComment: Boolean,
    canShare: Boolean,
    scrim: Color,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onFavorite: () -> Unit,
    onComments: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (canRate) {
            ShortsRailButton(onClick = onLike, label = meta?.likes.compactCount(), scrim = scrim) {
                Icon(
                    painter = painterResource(
                        if (meta?.liked == true) R.drawable.ic_baseline_thumb_up_alt_24
                        else R.drawable.ic_baseline_thumb_up_off_alt_24
                    ),
                    contentDescription = stringResource(R.string.site_shorts_like),
                    tint = if (meta?.liked == true) active else Color.White,
                )
            }
            ShortsRailButton(onClick = onDislike, label = null, scrim = scrim) {
                Icon(
                    painter = painterResource(
                        if (meta?.disliked == true) R.drawable.ic_baseline_thumb_down_alt_24
                        else R.drawable.ic_baseline_thumb_down_off_alt_24
                    ),
                    contentDescription = stringResource(R.string.site_shorts_dislike),
                    tint = if (meta?.disliked == true) active else Color.White,
                )
            }
        }
        if (canFavorite) {
            ShortsRailButton(onClick = onFavorite, label = null, scrim = scrim) {
                Icon(
                    painter = painterResource(
                        if (meta?.favorite == true) R.drawable.ic_baseline_favorite_24
                        else R.drawable.ic_baseline_favorite_border_24
                    ),
                    contentDescription = stringResource(R.string.site_shorts_favorite),
                    tint = if (meta?.favorite == true) active else Color.White,
                )
            }
        }
        if (canComment) {
            ShortsRailButton(
                onClick = onComments,
                label = meta?.commentsCount?.takeIf { it > 0 }.compactCount(),
                scrim = scrim,
            ) {
                Icon(
                    painterResource(R.drawable.ic_baseline_comment_24),
                    stringResource(R.string.xh_comments),
                    tint = Color.White,
                )
            }
        }
        if (canShare) {
            ShortsRailButton(onClick = onShare, label = null, scrim = scrim) {
                Icon(
                    painterResource(R.drawable.ic_baseline_share_24),
                    stringResource(R.string.share),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
internal fun ShortsCommentsSheet(
    state: ShortsCommentsState,
    fallbackCount: Long,
    commentsDisabled: Boolean,
    canPost: Boolean,
    replyingTo: String?,
    sending: Boolean,
    imageLoader: ImageLoader?,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onReply: (SiteShortComment) -> Unit,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit,
    onLikeComment: (SiteShortComment) -> Unit,
    onDeleteComment: (SiteShortComment) -> Unit,
    onReportComment: () -> Unit,
    onNeedLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.items.size, state.cursor) {
        if (nearEnd && state.cursor != null && !state.loading) onLoadMore()
    }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClose,
            ),
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.74f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.navigationBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.xh_comments) + "  " + (state.total ?: fallbackCount),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_baseline_close_24), stringResource(R.string.close))
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.loading && state.items.isEmpty() ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }

                        state.error != null && state.items.isEmpty() -> Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        }

                        state.items.isEmpty() ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(
                                        if (commentsDisabled) R.string.xh_comments_disabled else R.string.xh_no_comments
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.items, key = { it.id }) { c ->
                                SiteCommentCard(
                                    author = c.author,
                                    avatarUrl = c.avatarUrl,
                                    text = c.text,
                                    createdSeconds = c.createdSeconds,
                                    likes = c.likes,
                                    liked = c.likedByMe,
                                    isReply = c.isReply,
                                    replyToName = c.replyToName,
                                    imageLoader = imageLoader,
                                    onReply = { onReply(c) },
                                    onLike = { onLikeComment(c) },
                                    onDelete = if (c.editable) ({ onDeleteComment(c) }) else null,
                                    onReport = if (c.editable) null else onReportComment,
                                )
                            }
                            if (state.loading) item {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
                if (!commentsDisabled) {
                    SiteCommentComposer(
                        replyingTo = replyingTo,
                        sending = sending,
                        enabled = canPost,
                        onCancelReply = onCancelReply,
                        onSend = onSend,
                        onNeedLogin = onNeedLogin,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShortsSubtitleDialog(
    tracks: List<SiteShortSubtitle>,
    selectedLang: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.site_shorts_subtitles)) },
        text = {
            Column {
                SubtitleOption(stringResource(R.string.site_shorts_subtitles_off), selectedLang == null) { onSelect(null) }
                tracks.forEach { t -> SubtitleOption(t.label, selectedLang == t.lang) { onSelect(t.lang) } }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun SubtitleOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
internal fun ShortsReportDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.site_shorts_report_title)) },
        text = { Text(stringResource(R.string.site_shorts_report_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.site_shorts_report_open)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
