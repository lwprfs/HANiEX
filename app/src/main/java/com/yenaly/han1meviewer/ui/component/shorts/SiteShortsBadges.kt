package com.yenaly.han1meviewer.ui.component.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.R

/**
 * Shared floating-badge system for the Shorts / GIF experience.
 *
 * Every badge is the same compact translucent pill: no border, no shadow, white content, so it
 * stays readable on both bright and dark thumbnails and looks identical in light and dark themes
 * (the content underneath is video, not a themed surface).
 */
internal object ShortsBadgeDefaults {
    /** Glass fill used on top of thumbnails / video. */
    val Glass = Color.Black.copy(alpha = 0.42f)
    val Shape = RoundedCornerShape(50)
    val IconSize = 11.dp
    val HorizontalPadding = 7.dp
    val VerticalPadding = 2.5.dp
    val IconTextGap = 3.dp

    /** Soft text shadow for text that floats without a pill (e.g. counts in the action rail). */
    val TextShadow = Shadow(
        color = Color.Black.copy(alpha = 0.55f),
        offset = Offset(0f, 1f),
        blurRadius = 6f,
    )
}

/** Base pill. All other badges are built on this so size, padding and radius stay consistent. */
@Composable
internal fun ShortsBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = ShortsBadgeDefaults.Glass,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(ShortsBadgeDefaults.Shape)
            .background(containerColor)
            .padding(
                horizontal = ShortsBadgeDefaults.HorizontalPadding,
                vertical = ShortsBadgeDefaults.VerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ShortsBadgeDefaults.IconTextGap),
        content = content,
    )
}

@Composable
private fun ShortsBadgeText(text: String) {
    Text(
        text = text,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

/** Icon + compact value pill, e.g. ▶ 1.2M. */
@Composable
internal fun ShortsStatBadge(
    icon: Painter,
    value: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    ShortsBadge(
        modifier = if (description != null) {
            modifier.semantics { contentDescription = description }
        } else modifier,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(ShortsBadgeDefaults.IconSize),
        )
        ShortsBadgeText(value)
    }
}

/** ▶ 12K — view count. [views] must already be compacted; pass null to render nothing. */
@Composable
internal fun ShortsViewsBadge(views: String?, modifier: Modifier = Modifier) {
    if (views.isNullOrEmpty()) return
    ShortsStatBadge(
        icon = painterResource(R.drawable.ic_baseline_play_arrow_24),
        value = views,
        modifier = modifier,
        description = stringResource(R.string.site_shorts_n_views, views),
    )
}

/** ♥ 3.4K — like count. Renders nothing when the source gave no number. */
@Composable
internal fun ShortsLikesBadge(likes: String?, modifier: Modifier = Modifier) {
    if (likes.isNullOrEmpty()) return
    ShortsStatBadge(
        icon = painterResource(R.drawable.ic_baseline_favorite_24),
        value = likes,
        modifier = modifier,
        description = stringResource(R.string.site_shorts_like) + " " + likes,
    )
}

/** 💬 214 — comment count. Renders nothing when the source gave no number. */
@Composable
internal fun ShortsCommentsBadge(comments: String?, modifier: Modifier = Modifier) {
    if (comments.isNullOrEmpty()) return
    ShortsStatBadge(
        icon = painterResource(R.drawable.ic_shorts_comment_outline_24),
        value = comments,
        modifier = modifier,
        description = stringResource(R.string.xh_comments) + " " + comments,
    )
}

/** 0:07 — duration. Only shown when the source actually provides it. */
@Composable
internal fun ShortsDurationBadge(seconds: Int?, modifier: Modifier = Modifier) {
    if (seconds == null || seconds <= 0) return
    val text = "%d:%02d".format(seconds / 60, seconds % 60)
    ShortsBadge(modifier) { ShortsBadgeText(text) }
}

/**
 * GIF indicator, shared between grid thumbnails and the player so they always match.
 * Uses the theme's primary colour so it is the one badge that reads instantly as a "type" tag.
 */
@Composable
internal fun ShortsGifBadge(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(ShortsBadgeDefaults.Shape)
            .background(scheme.primary.copy(alpha = 0.92f))
            .padding(horizontal = 8.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.site_shorts_gif_badge),
            color = scheme.onPrimary,
            maxLines = 1,
            style = LocalTextStyle.current.merge(
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
            ),
        )
    }
}

/**
 * Tappable tag chip for the player. Same glass look as the badges so it sits naturally on the
 * video; slightly taller so it is easy to hit. Tags and categories get a "#" prefix.
 */
@Composable
internal fun ShortsTagChip(tag: SiteShortTag, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = when (tag.kind) {
        SiteShortTag.Kind.TAG, SiteShortTag.Kind.CATEGORY -> "#" + tag.name
        else -> tag.name
    }
    Text(
        text = label,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            shadow = ShortsBadgeDefaults.TextShadow,
        ),
        modifier = modifier
            .clip(ShortsBadgeDefaults.Shape)
            .background(ShortsBadgeDefaults.Glass)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}
