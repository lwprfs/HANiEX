package com.yenaly.han1meviewer.ui.component.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteVerifiedBadge
import com.yenaly.han1meviewer.ui.screen.RetryableImage

@Composable
internal fun SocialEntityVariant(
    style: SocialListStyle,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    description: String?,
    meta: String?,
    metaIcon: ImageVector?,
    verified: Boolean,
    chips: List<SocialChip>,
    trailing: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
    isCommunity: Boolean,
) {
    when (style) {
        SocialListStyle.SHOWCASE -> ShowcaseEntity(
            name, onClick, modifier, avatarUrl, avatarShape, username, description, meta,
            metaIcon, verified, chips, trailing, imageLoader, isCommunity,
        )

        SocialListStyle.CENTERED -> CenteredEntity(
            name, onClick, modifier, avatarUrl, avatarShape, username, description, meta,
            metaIcon, verified, chips, trailing, imageLoader,
        )

        SocialListStyle.COMPACT -> CompactEntity(
            name, onClick, modifier, avatarUrl, avatarShape, username, meta, verified,
            trailing, imageLoader,
        )

        SocialListStyle.FLAT -> FlatEntity(
            name, onClick, modifier, avatarUrl, avatarShape, username, description, meta,
            verified, trailing, imageLoader,
        )

        SocialListStyle.CLASSIC -> Unit
    }
}

@Composable
private fun EntityNameLine(
    name: String,
    verified: Boolean,
    fontSize: Int,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
    ) {
        Text(
            text = name,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (verified) {
            Spacer(Modifier.width(4.dp))
            SiteVerifiedBadge(size = (fontSize - 1).dp)
        }
    }
}

@Composable
private fun EntityMetaLine(
    meta: String?,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
) {
    if (meta.isNullOrBlank()) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = meta,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun RingedAvatar(
    url: String?,
    name: String,
    size: Dp,
    shape: Shape,
    imageLoader: ImageLoader?,
    ringWidth: Dp = 2.dp,
) {
    Box(
        modifier = Modifier
            .border(ringWidth, MaterialTheme.colorScheme.primary, shape)
            .padding(ringWidth + 1.dp),
    ) {
        SocialAvatar(
            url = url,
            name = name,
            size = size,
            shape = shape,
            imageLoader = imageLoader,
        )
    }
}

@Composable
private fun ShowcaseEntity(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    description: String?,
    meta: String?,
    metaIcon: ImageVector?,
    verified: Boolean,
    chips: List<SocialChip>,
    trailing: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
    isCommunity: Boolean,
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box {
                    RingedAvatar(avatarUrl, name, 84.dp, avatarShape, imageLoader)
                    if (isCommunity) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    EntityNameLine(name, verified, fontSize = 22)
                    if (!username.isNullOrBlank()) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    EntityMetaLine(meta, metaIcon)
                }
                trailing?.invoke()
            }
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CenteredChips(chips)
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isCommunity) R.string.social_view_community
                        else R.string.social_view_profile
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CenteredChips(chips: List<SocialChip>, modifier: Modifier = Modifier) {
    if (chips.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { chips.forEach { SocialChipPill(it) } }
}

@Composable
private fun CenteredEntity(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    description: String?,
    meta: String?,
    metaIcon: ImageVector?,
    verified: Boolean,
    chips: List<SocialChip>,
    trailing: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
) {
    val shape = RoundedCornerShape(SocialDefaults.CardRadius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SocialAvatar(avatarUrl, name, 88.dp, shape = avatarShape, imageLoader = imageLoader)
            Spacer(Modifier.height(4.dp))
            EntityNameLine(name, verified, fontSize = 20, centered = true)
            if (!username.isNullOrBlank()) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            EntityMetaLine(meta, metaIcon, centered = true)
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CenteredChips(chips, Modifier.padding(top = 2.dp))
            trailing?.invoke()
        }
    }
}

@Composable
private fun CompactEntity(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    meta: String?,
    verified: Boolean,
    trailing: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SocialAvatar(avatarUrl, name, 48.dp, shape = avatarShape, imageLoader = imageLoader)
            Column(modifier = Modifier.weight(1f)) {
                EntityNameLine(name, verified, fontSize = 16)
                val second = listOfNotNull(
                    username?.takeIf { it.isNotBlank() },
                    meta?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (second.isNotEmpty()) {
                    Text(
                        text = second,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun FlatEntity(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    description: String?,
    meta: String?,
    verified: Boolean,
    trailing: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SocialAvatar(avatarUrl, name, 56.dp, shape = avatarShape, imageLoader = imageLoader)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                EntityNameLine(name, verified, fontSize = 17)
                if (!username.isNullOrBlank()) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!meta.isNullOrBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.invoke()
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 86.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
internal fun SocialProfileVariant(
    style: SocialProfileStyle,
    name: String,
    modifier: Modifier,
    coverUrl: String?,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    verified: Boolean,
    chips: List<SocialChip>,
    bio: String?,
    metaLines: List<String>,
    actions: (@Composable RowScope.() -> Unit)?,
    stats: List<SocialStat>,
    errorText: String?,
    extra: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
) {
    when (style) {
        SocialProfileStyle.CENTERED -> CenteredProfile(
            name, modifier, coverUrl, avatarUrl, avatarShape, username, verified, chips, bio,
            metaLines, actions, stats, errorText, extra, imageLoader,
        )

        SocialProfileStyle.COMPACT -> CompactProfile(
            name, modifier, coverUrl, avatarUrl, avatarShape, username, verified, chips, bio,
            metaLines, actions, stats, errorText, extra, imageLoader,
        )

        SocialProfileStyle.IMMERSIVE -> ImmersiveProfile(
            name, modifier, coverUrl, avatarUrl, avatarShape, username, verified, chips, bio,
            metaLines, actions, stats, errorText, extra, imageLoader,
        )

        SocialProfileStyle.CLASSIC -> Unit
    }
}

@Composable
private fun CoverImage(
    coverUrl: String,
    imageLoader: ImageLoader?,
    modifier: Modifier = Modifier,
) {
    RetryableImage(
        model = coverUrl,
        modifier = modifier,
        placeholder = painterResource(R.drawable.h_chan_loading),
        error = painterResource(R.drawable.h_chan_load_failed),
        contentScale = ContentScale.Crop,
        imageLoader = imageLoader,
    )
}

@Composable
private fun StatPill(
    stat: SocialStat,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    val value = stat.value ?: return
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(if (stat.onClick != null) Modifier.clickable(onClick = stat.onClick) else Modifier),
        shape = RoundedCornerShape(50),
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = formatSocialCount(value),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1,
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatPills(
    stats: List<SocialStat>,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    val visible = stats.filter { it.value != null }
    if (visible.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { visible.forEach { StatPill(it, container, content) } }
}

@Composable
private fun DividedStatRow(stats: List<SocialStat>, modifier: Modifier = Modifier) {
    val visible = stats.filter { it.value != null }
    if (visible.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEachIndexed { index, stat ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (stat.onClick != null) Modifier.clickable(onClick = stat.onClick)
                        else Modifier
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = formatSocialCount(stat.value ?: 0L),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ActionsRow(
    actions: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (actions == null) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = actions,
    )
}

@Composable
private fun ErrorLine(errorText: String?) {
    if (errorText.isNullOrBlank()) return
    Text(
        text = errorText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun CenteredProfile(
    name: String,
    modifier: Modifier,
    coverUrl: String?,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    verified: Boolean,
    chips: List<SocialChip>,
    bio: String?,
    metaLines: List<String>,
    actions: (@Composable RowScope.() -> Unit)?,
    stats: List<SocialStat>,
    errorText: String?,
    extra: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
) {
    val avatarSize = 104.dp
    val pad = SocialDefaults.ScreenPadding
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (coverUrl.isNullOrBlank()) 96.dp else 160.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to scheme.primaryContainer.copy(alpha = 0.55f),
                            1f to scheme.background,
                        )
                    ),
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    CoverImage(coverUrl, imageLoader, Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.45f to Color.Transparent,
                                    1f to scheme.background.copy(alpha = 0.9f),
                                )
                            ),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = avatarSize / 2)
                    .background(scheme.background, avatarShape)
                    .border(3.dp, scheme.primary, avatarShape)
                    .padding(4.dp),
            ) {
                SocialAvatar(avatarUrl, name, avatarSize, shape = avatarShape, imageLoader = imageLoader)
            }
        }
        Spacer(Modifier.height(avatarSize / 2 + 12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pad),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EntityNameLine(name, verified, fontSize = 26, centered = true)
            if (!username.isNullOrBlank()) {
                Text(
                    text = username,
                    fontSize = 15.sp,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DividedStatRow(stats, Modifier.padding(top = 6.dp))
            SocialExpandableText(
                bio,
                modifier = Modifier.padding(top = 2.dp),
                textAlign = TextAlign.Center,
            )
            metaLines.filter { it.isNotBlank() }.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            CenteredChips(chips)
            ActionsRow(actions, Modifier.padding(top = 4.dp))
            ErrorLine(errorText)
            extra?.invoke()
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CompactProfile(
    name: String,
    modifier: Modifier,
    coverUrl: String?,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    verified: Boolean,
    chips: List<SocialChip>,
    bio: String?,
    metaLines: List<String>,
    actions: (@Composable RowScope.() -> Unit)?,
    stats: List<SocialStat>,
    errorText: String?,
    extra: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
) {
    val pad = SocialDefaults.ScreenPadding
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        if (!coverUrl.isNullOrBlank()) {
            CoverImage(
                coverUrl = coverUrl,
                imageLoader = imageLoader,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(scheme.surfaceContainerHigh),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pad, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SocialAvatar(avatarUrl, name, 76.dp, shape = avatarShape, imageLoader = imageLoader)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    EntityNameLine(name, verified, fontSize = 21)
                    if (!username.isNullOrBlank()) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    SocialChipFlow(chips, Modifier.padding(top = 2.dp))
                }
            }
            StatPills(stats, scheme.surfaceContainerHighest, scheme.onSurface)
            SocialExpandableText(bio)
            metaLines.filter { it.isNotBlank() }.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
            ActionsRow(actions)
            ErrorLine(errorText)
            extra?.invoke()
        }
    }
}

@Composable
private fun ImmersiveProfile(
    name: String,
    modifier: Modifier,
    coverUrl: String?,
    avatarUrl: String?,
    avatarShape: Shape,
    username: String?,
    verified: Boolean,
    chips: List<SocialChip>,
    bio: String?,
    metaLines: List<String>,
    actions: (@Composable RowScope.() -> Unit)?,
    stats: List<SocialStat>,
    errorText: String?,
    extra: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?,
) {
    val pad = SocialDefaults.ScreenPadding
    val scheme = MaterialTheme.colorScheme
    val hasCover = !coverUrl.isNullOrBlank()
    val textColor = if (hasCover) Color.White else scheme.onSurface
    val subColor = if (hasCover) Color.White.copy(alpha = 0.78f) else scheme.onSurfaceVariant
    Box(modifier = modifier.fillMaxWidth()) {
        ImmersiveBackdrop(coverUrl, imageLoader)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = pad,
                    end = pad,
                    top = if (hasCover) 150.dp else 28.dp,
                    bottom = 14.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RingedAvatar(avatarUrl, name, 84.dp, avatarShape, imageLoader, ringWidth = 3.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (verified) {
                            Spacer(Modifier.width(6.dp))
                            SiteVerifiedBadge(size = 22.dp)
                        }
                    }
                    if (!username.isNullOrBlank()) {
                        Text(
                            text = username,
                            fontSize = 15.sp,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            SocialChipFlow(chips)
            if (!bio.isNullOrBlank()) {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.92f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            metaLines.filter { it.isNotBlank() }.forEach {
                Text(text = it, style = MaterialTheme.typography.labelLarge, color = subColor)
            }
            StatPills(
                stats = stats,
                container = if (hasCover) Color.White.copy(alpha = 0.18f) else scheme.surfaceContainerHighest,
                content = textColor,
            )
            ActionsRow(actions)
            ErrorLine(errorText)
            extra?.invoke()
        }
    }
}

@Composable
private fun BoxScope.ImmersiveBackdrop(coverUrl: String?, imageLoader: ImageLoader?) {
    val scheme = MaterialTheme.colorScheme
    if (!coverUrl.isNullOrBlank()) {
        CoverImage(coverUrl, imageLoader, Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.88f),
                    )
                ),
        )
    } else {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to scheme.primaryContainer.copy(alpha = 0.6f),
                        1f to scheme.background,
                    )
                ),
        )
    }
}

@Composable
internal fun SocialUnderlineTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val fill = labels.size <= 3
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fill) Modifier else Modifier.horizontalScroll(rememberScrollState())),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .then(if (fill) Modifier.weight(1f) else Modifier)
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (selected) scheme.primary else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
    }
}
