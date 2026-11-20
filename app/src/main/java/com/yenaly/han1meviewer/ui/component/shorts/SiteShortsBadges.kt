package com.yenaly.han1meviewer.ui.component.shorts

import androidx.compose.foundation.background
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

internal object ShortsBadgeDefaults {

    val Glass = Color.Black.copy(alpha = 0.42f)
    val Shape = RoundedCornerShape(50)
    val IconSize = 11.dp
    val HorizontalPadding = 7.dp
    val VerticalPadding = 2.5.dp
    val IconTextGap = 3.dp

    val TextShadow = Shadow(
        color = Color.Black.copy(alpha = 0.55f),
        offset = Offset(0f, 1f),
        blurRadius = 6f,
    )
}

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

@Composable
internal fun ShortsDurationBadge(seconds: Int?, modifier: Modifier = Modifier) {
    if (seconds == null || seconds <= 0) return
    val text = "%d:%02d".format(seconds / 60, seconds % 60)
    ShortsBadge(modifier) { ShortsBadgeText(text) }
}

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
