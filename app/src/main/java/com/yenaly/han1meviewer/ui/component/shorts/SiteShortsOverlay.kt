package com.yenaly.han1meviewer.ui.component.shorts

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortsRailButton(
    onClick: () -> Unit,
    label: String?,
    scrim: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scrim)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { content() }
        if (!label.isNullOrEmpty()) {
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortsRatingPill(
    likes: String?,
    liked: Boolean,
    disliked: Boolean,
    scrim: Color,
    onLike: () -> Unit,
    onDislike: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val likeTint by animateColorAsState(if (liked) scheme.primary else Color.White, label = "ShortsLikeTint")
    val dislikeTint by animateColorAsState(if (disliked) scheme.error else Color.White, label = "ShortsDislikeTint")
    val likeBg by animateColorAsState(
        if (liked) scheme.primaryContainer.copy(alpha = 0.55f) else scrim, label = "ShortsLikeBg",
    )
    val dislikeBg by animateColorAsState(
        if (disliked) scheme.errorContainer.copy(alpha = 0.55f) else scrim, label = "ShortsDislikeBg",
    )
    Column(
        modifier = Modifier.widthIn(min = 52.dp).clip(RoundedCornerShape(20.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(likeBg)
                .combinedClickable(onClick = onLike)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = stringResource(R.string.site_shorts_like),
                modifier = Modifier.size(24.dp),
                tint = likeTint,
            )
            if (!likes.isNullOrEmpty()) {
                Text(likes, color = likeTint, style = MaterialTheme.typography.labelMedium)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.22f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(dislikeBg)
                .combinedClickable(onClick = onDislike),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (disliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                contentDescription = stringResource(R.string.site_shorts_dislike),
                modifier = Modifier.size(24.dp),
                tint = dislikeTint,
            )
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (canRate) {
            ShortsRatingPill(
                likes = meta?.likes.compactCount(),
                liked = meta?.liked == true,
                disliked = meta?.disliked == true,
                scrim = scrim,
                onLike = onLike,
                onDislike = onDislike,
            )
        }
        if (canFavorite) {
            ShortsRailButton(onClick = onFavorite, label = stringResource(R.string.site_shorts_favorite), scrim = scrim) {
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
                label = meta?.commentsCount?.takeIf { it > 0 }.compactCount()
                    ?: stringResource(R.string.xh_comments),
                scrim = scrim,
            ) {
                Icon(
                    painterResource(R.drawable.ic_shorts_comment_outline_24),
                    stringResource(R.string.xh_comments),
                    tint = Color.White,
                )
            }
        }
        if (canShare) {
            ShortsRailButton(onClick = onShare, label = stringResource(R.string.share), scrim = scrim) {
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
