package com.yenaly.han1meviewer.ui.screen.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SettingIconBadge
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingTile
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold

/**
 * One tappable row in a site hub page (Library, Subscription, Profile, Customize).
 *
 * The hub pages never own any business logic: every entry just forwards to an existing
 * route of the site, so existing screens, ViewModels and data sources are reused as they are.
 */
data class SiteHubEntry(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val subtitle: String? = null,
)

/** A titled group of [SiteHubEntry]. A `null` [title] renders the group without a header. */
data class SiteHubSection(
    val title: String?,
    val entries: List<SiteHubEntry>,
)

/**
 * State of the account card on My Profile.
 *
 * A site without authentication passes `onClick = null` plus an explanatory [subtitle]; the
 * card is then rendered as a non-interactive "not available" state instead of a fake login.
 * When login support is added to such a site later, only this object needs to be filled in.
 */
data class SiteAccountHeader(
    val siteLabel: String,
    val title: String,
    val subtitle: String,
    val avatarUrl: String? = null,
    val loggedIn: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

private val QuickActionMinWidth = 150.dp
private val QuickActionHeight = 96.dp

@Composable
internal fun SiteHubRow(entry: SiteHubEntry) {
    SettingTile(onClick = entry.onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingIconBadge(imageVector = entry.icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!entry.subtitle.isNullOrBlank()) {
                    Text(
                        text = entry.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Renders [sections] as LazyColumn items. Shared by every hub page. */
internal fun androidx.compose.foundation.lazy.LazyListScope.siteHubSections(
    sections: List<SiteHubSection>,
) {
    sections.forEachIndexed { sectionIndex, section ->
        if (section.entries.isEmpty()) return@forEachIndexed
        if (!section.title.isNullOrBlank()) {
            item(key = "hub_section_${sectionIndex}_title") {
                SettingSectionHeader(title = section.title)
            }
        }
        items(
            items = section.entries,
            key = { "hub_entry_${sectionIndex}_${it.key}" },
        ) { entry -> SiteHubRow(entry) }
    }
}

/**
 * Generic list page used for Library, Subscription and Customize.
 *
 * @param onBack `null` for primary (bottom-bar) destinations, a callback for secondary pages.
 * @param emptyState shown instead of the list when [sections] has no entries, so an unsupported
 *   feature gets an honest "unavailable" explanation rather than an empty screen.
 */
@Composable
fun SiteHubListScreen(
    title: String,
    sections: List<SiteHubSection>,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    emptyState: (@Composable () -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val hasEntries = sections.any { it.entries.isNotEmpty() }
    HanimeScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        if (!hasEntries && emptyState != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { emptyState() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                if (extraContent != null) {
                    item(key = "hub_extra_content") { extraContent() }
                }
                siteHubSections(sections)
            }
        }
    }
}

/** Centered "feature not supported on this site" state. */
@Composable
fun SiteHubUnavailableState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * My Profile for non-Hanime sites, modelled on Hanime's ProfileScreen: account card on top,
 * a grid of quick actions, then grouped lists for everything else.
 */
@Composable
fun SiteProfileScreen(
    account: SiteAccountHeader,
    quickActions: List<SiteHubEntry>,
    sections: List<SiteHubSection>,
    modifier: Modifier = Modifier,
) {
    HanimeScaffold(
        title = stringResource(R.string.my_account),
        onBack = null,
        modifier = modifier,
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val itemSpacing = 12.dp
            val horizontalPadding = 16.dp
            val columns = maxOf(
                2,
                ((maxWidth - horizontalPadding * 2 + itemSpacing) /
                        (QuickActionMinWidth + itemSpacing)).toInt(),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                item(key = "profile_account", span = { GridItemSpan(maxLineSpan) }) {
                    SiteAccountCard(account)
                }
                if (quickActions.isNotEmpty()) {
                    item(key = "profile_quick_title", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.hub_section_quick_actions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(items = quickActions, key = { "quick_${it.key}" }) { entry ->
                        SiteQuickActionTile(entry)
                    }
                }
                sections.forEachIndexed { sectionIndex, section ->
                    if (section.entries.isEmpty()) return@forEachIndexed
                    if (!section.title.isNullOrBlank()) {
                        item(
                            key = "profile_section_${sectionIndex}_title",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                    items(
                        items = section.entries,
                        key = { "profile_entry_${sectionIndex}_${it.key}" },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { entry ->
                        SiteHubRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteAccountCard(account: SiteAccountHeader) {
    val shape = RoundedCornerShape(28.dp)
    val clickable = account.onClick != null
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(64.dp),
            ) {
                if (account.avatarUrl.isNullOrBlank()) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                } else {
                    AsyncImage(
                        model = account.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.bg_default_header),
                        fallback = painterResource(R.drawable.bg_default_header),
                        error = painterResource(R.drawable.bg_default_header),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = account.siteLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = account.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = account.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (clickable) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
    if (account.onClick != null) {
        Surface(
            onClick = account.onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) { content() }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
        ) { content() }
    }
}

@Composable
private fun SiteQuickActionTile(entry: SiteHubEntry) {
    Surface(
        onClick = entry.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QuickActionHeight),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
