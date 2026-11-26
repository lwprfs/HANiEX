package com.yenaly.han1meviewer.ui.component.social

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
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

object SocialDefaults {
    val CardRadius = 20.dp
    val CardAvatar = 72.dp
    val ProfileAvatar = 92.dp
    val CoverHeight = 150.dp
    val ScreenPadding = 16.dp
    val CardSpacing = 12.dp
    val CommunityAvatarShape: Shape = RoundedCornerShape(20.dp)
}

fun formatSocialCount(value: Long): String = when {
    value >= 1_000_000L -> trimZero(String.format("%.1f", value / 1_000_000.0)) + "M"
    value >= 1_000L -> trimZero(String.format("%.1f", value / 1_000.0)) + "K"
    else -> value.toString()
}

fun formatSocialCount(value: Int): String = formatSocialCount(value.toLong())

private fun trimZero(s: String) = if (s.endsWith(".0")) s.dropLast(2) else s

data class SocialStat(
    val label: String,
    val value: Long?,
    val onClick: (() -> Unit)? = null,
)

enum class SocialTone { Neutral, Accent, Warning, Positive }

data class SocialChip(val text: String, val tone: SocialTone = SocialTone.Neutral)

@Composable
fun SocialAvatar(
    url: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    imageLoader: ImageLoader? = null,
    ringColor: Color? = null,
) {
    val ring = if (ringColor != null) Modifier.border(4.dp, ringColor, shape) else Modifier
    Box(
        modifier = modifier
            .size(size)
            .then(ring)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            RetryableImage(
                model = url,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(R.drawable.h_chan_loading),
                error = painterResource(R.drawable.h_chan_load_failed),
                contentScale = ContentScale.Crop,
                imageLoader = imageLoader,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.trim().take(1).uppercase().ifEmpty { "?" },
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
fun SocialChipPill(chip: SocialChip, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (chip.tone) {
        SocialTone.Neutral -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        SocialTone.Accent -> scheme.primaryContainer to scheme.onPrimaryContainer
        SocialTone.Warning -> scheme.errorContainer to scheme.onErrorContainer
        SocialTone.Positive -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = chip.text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialChipFlow(
    chips: List<SocialChip>,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { chips.forEach { SocialChipPill(it) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialTagFlow(
    tags: List<String>,
    modifier: Modifier = Modifier,
    prefix: String = "",
    onClick: ((String) -> Unit)? = null,
) {
    if (tags.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = if (onClick != null) Modifier.clip(RoundedCornerShape(50))
                    .clickable { onClick(tag) } else Modifier,
            ) {
                Text(
                    text = prefix + tag,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialLinkFlow(
    links: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    if (links.isEmpty()) return
    val uri = LocalUriHandler.current
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        links.forEach { (label, url) ->
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { runCatching { uri.openUri(url) } },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

enum class SocialButtonStyle { Primary, Tonal, Outlined }

@Composable
fun SocialActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SocialButtonStyle = SocialButtonStyle.Primary,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val padding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    val shape = RoundedCornerShape(50)
    val content: @Composable RowScope.() -> Unit = {
        AnimatedContent(
            targetState = loading to label,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "socialActionContent",
        ) { (busy, text) ->
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    val canClick = enabled && !loading
    when (style) {
        SocialButtonStyle.Primary -> Button(
            onClick = onClick, enabled = canClick, modifier = modifier.heightIn(min = 44.dp),
            shape = shape, contentPadding = padding, content = content,
        )

        SocialButtonStyle.Tonal -> FilledTonalButton(
            onClick = onClick, enabled = canClick, modifier = modifier.heightIn(min = 44.dp),
            shape = shape, contentPadding = padding, content = content,
        )

        SocialButtonStyle.Outlined -> OutlinedButton(
            onClick = onClick, enabled = canClick, modifier = modifier.heightIn(min = 44.dp),
            shape = shape, contentPadding = padding,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(),
            content = content,
        )
    }
}

@Composable
fun SocialStatRow(
    stats: List<SocialStat>,
    modifier: Modifier = Modifier,
) {
    val visible = stats.filter { it.value != null }
    if (visible.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            visible.forEach { stat ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (stat.onClick != null) Modifier.clickable(onClick = stat.onClick)
                            else Modifier
                        )
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = formatSocialCount(stat.value ?: 0L),
                        fontSize = 18.sp,
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
}

@Composable
fun SocialTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showSingle: Boolean = false,
    community: Boolean = false,
) {
    if (labels.size < 2 && !showSingle) return
    val cfg = LocalSocialStyle.current
    val headerStyle = if (community) cfg.community else cfg.profile
    if (headerStyle == SocialProfileStyle.CENTERED || headerStyle == SocialProfileStyle.IMMERSIVE) {
        SocialUnderlineTabs(labels, selectedIndex, onSelect, modifier)
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = SocialDefaults.ScreenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "socialTabBg",
            )
            val fg by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "socialTabFg",
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = fg,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun SocialEntityCard(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    avatarShape: Shape = CircleShape,
    username: String? = null,
    description: String? = null,
    meta: String? = null,
    verified: Boolean = false,
    chips: List<SocialChip> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    imageLoader: ImageLoader? = null,
    metaIcon: ImageVector? = null,
    isCommunity: Boolean = false,
) {
    val listStyle = LocalSocialStyle.current.list
    if (listStyle != SocialListStyle.CLASSIC) {
        SocialEntityVariant(
            style = listStyle,
            name = name,
            onClick = onClick,
            modifier = modifier,
            avatarUrl = avatarUrl,
            avatarShape = avatarShape,
            username = username,
            description = description,
            meta = meta,
            metaIcon = metaIcon,
            verified = verified,
            chips = chips,
            trailing = trailing,
            imageLoader = imageLoader,
            isCommunity = isCommunity,
        )
        return
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(SocialDefaults.CardRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(SocialDefaults.CardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SocialAvatar(
                url = avatarUrl,
                name = name,
                size = SocialDefaults.CardAvatar,
                shape = avatarShape,
                imageLoader = imageLoader,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (verified) {
                        Spacer(Modifier.width(4.dp))
                        SiteVerifiedBadge(size = 18.dp)
                    }
                }
                if (!username.isNullOrBlank()) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyMedium,
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!meta.isNullOrBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SocialChipFlow(chips, Modifier.padding(top = 2.dp))
            }
            when {
                trailing != null -> trailing()
                showChevron -> Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun SocialUserCard(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    username: String? = null,
    bio: String? = null,
    meta: String? = null,
    verified: Boolean = false,
    chips: List<SocialChip> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
    imageLoader: ImageLoader? = null,
    metaIcon: ImageVector? = null,
) = SocialEntityCard(
    name = name, onClick = onClick, modifier = modifier, avatarUrl = avatarUrl,
    avatarShape = CircleShape, username = username, description = bio, meta = meta,
    verified = verified, chips = chips, trailing = trailing, imageLoader = imageLoader,
    metaIcon = metaIcon,
)

@Composable
fun SocialCommunityCard(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    description: String? = null,
    meta: String? = null,
    verified: Boolean = false,
    chips: List<SocialChip> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
    imageLoader: ImageLoader? = null,
    metaIcon: ImageVector? = null,
) = SocialEntityCard(
    name = name, onClick = onClick, modifier = modifier, avatarUrl = avatarUrl,
    avatarShape = SocialDefaults.CommunityAvatarShape, description = description, meta = meta,
    verified = verified, chips = chips, trailing = trailing, showChevron = showChevron,
    imageLoader = imageLoader, metaIcon = metaIcon, isCommunity = true,
)

@Composable
fun SocialProfileHeader(
    name: String,
    modifier: Modifier = Modifier,
    coverUrl: String? = null,
    avatarUrl: String? = null,
    avatarShape: Shape = CircleShape,
    username: String? = null,
    verified: Boolean = false,
    chips: List<SocialChip> = emptyList(),
    bio: String? = null,
    metaLines: List<String> = emptyList(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    stats: List<SocialStat> = emptyList(),
    errorText: String? = null,
    extra: (@Composable () -> Unit)? = null,
    imageLoader: ImageLoader? = null,
    isCommunity: Boolean = false,
) {
    val cfg = LocalSocialStyle.current
    val headerStyle = if (isCommunity) cfg.community else cfg.profile
    if (headerStyle != SocialProfileStyle.CLASSIC) {
        SocialProfileVariant(
            style = headerStyle,
            name = name,
            modifier = modifier,
            coverUrl = coverUrl,
            avatarUrl = avatarUrl,
            avatarShape = avatarShape,
            username = username,
            verified = verified,
            chips = chips,
            bio = bio,
            metaLines = metaLines,
            actions = actions,
            stats = stats,
            errorText = errorText,
            extra = extra,
            imageLoader = imageLoader,
        )
        return
    }
    val hasCover = !coverUrl.isNullOrBlank()
    val avatarSize = SocialDefaults.ProfileAvatar
    val pad = SocialDefaults.ScreenPadding

    Column(modifier = modifier.fillMaxWidth()) {
        if (hasCover) {

            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SocialDefaults.CoverHeight)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    RetryableImage(
                        model = coverUrl,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(R.drawable.h_chan_loading),
                        error = painterResource(R.drawable.h_chan_load_failed),
                        contentScale = ContentScale.Crop,
                        imageLoader = imageLoader,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.5f to Color.Transparent,
                                    1f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                )
                            ),
                    )
                }
                SocialAvatar(
                    url = avatarUrl,
                    name = name,
                    size = avatarSize,
                    shape = avatarShape,
                    imageLoader = imageLoader,
                    ringColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = pad)
                        .offset(y = avatarSize / 2),
                )
            }
            Spacer(Modifier.height(avatarSize / 2 + 10.dp))
        } else {

            SocialAvatar(
                url = avatarUrl,
                name = name,
                size = avatarSize,
                shape = avatarShape,
                imageLoader = imageLoader,
                modifier = Modifier.padding(start = pad, top = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pad),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SocialChipFlow(chips)

            if (actions != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SocialExpandableText(bio, modifier = Modifier.padding(top = 4.dp))
            metaLines.filter { it.isNotBlank() }.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (stats.any { it.value != null }) {
                SocialStatRow(stats, Modifier.padding(top = 6.dp))
            }
            extra?.invoke()
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun SocialExpandableText(
    text: String?,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 3,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    if (text.isNullOrBlank()) return
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) {
            Text(
                text = stringResource(if (expanded) R.string.collapse else R.string.expand),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
fun SocialSectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(SocialDefaults.CardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}
