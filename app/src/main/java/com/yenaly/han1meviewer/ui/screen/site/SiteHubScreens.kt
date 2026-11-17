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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.AccountAvatar
import com.yenaly.han1meviewer.ui.component.ExpressiveEntryCard
import com.yenaly.han1meviewer.ui.component.SettingIconBadge
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingTile
import com.yenaly.han1meviewer.ui.component.appbar.HanimeScaffold

data class SiteHubEntry(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    /** Login-only entry shown while signed out: rendered with a lock and its [onClick] opens sign-in. */
    val locked: Boolean = false,
)

/**
 * Marks a login-only entry. It stays visible while signed out (like Hanime's library cards),
 * but is locked and sends the user to [onLogin] instead of opening the page.
 */
fun SiteHubEntry.gated(loggedIn: Boolean, onLogin: () -> Unit): SiteHubEntry =
    if (loggedIn) this else copy(locked = true, onClick = onLogin)

data class SiteHubSection(
    val title: String?,
    val entries: List<SiteHubEntry>,
)

data class SiteAccountHeader(
    val siteLabel: String,
    val title: String,
    val subtitle: String,
    val avatarUrl: String? = null,
    val loggedIn: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

private val CardMinWidth = 160.dp
private val CardHeight = 168.dp
private val CardGridHorizontalPadding = 20.dp
private val CardGridSpacing = 12.dp

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
                imageVector = if (entry.locked) {
                    Icons.Filled.Lock
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = if (entry.locked) stringResource(R.string.login_first) else null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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

@Composable
fun SiteHubListScreen(
    title: String,
    sections: List<SiteHubSection>,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    extraContent: (@Composable () -> Unit)? = null,
) {
    HanimeScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        if (onBack == null) {
            SiteHubCardGrid(
                sections = sections,
                padding = padding,
                extraContent = extraContent,
            )
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

@Composable
private fun SiteHubCardGrid(
    sections: List<SiteHubSection>,
    padding: PaddingValues,
    extraContent: (@Composable () -> Unit)?,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = hubCardColumns(maxWidth)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = CardGridHorizontalPadding,
                end = CardGridHorizontalPadding,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(CardGridSpacing),
            verticalArrangement = Arrangement.spacedBy(CardGridSpacing),
        ) {
            if (extraContent != null) {
                item(key = "hub_extra_content", span = { GridItemSpan(maxLineSpan) }) {
                    extraContent()
                }
            }
            siteHubCardSections(sections)
        }
    }
}

private fun hubCardColumns(maxWidth: androidx.compose.ui.unit.Dp): Int = maxOf(
    2,
    ((maxWidth - CardGridHorizontalPadding * 2 + CardGridSpacing) /
            (CardMinWidth + CardGridSpacing)).toInt(),
)

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.siteHubCardSections(
    sections: List<SiteHubSection>,
) {
    sections.forEachIndexed { sectionIndex, section ->
        if (section.entries.isEmpty()) return@forEachIndexed
        if (!section.title.isNullOrBlank()) {
            item(
                key = "hub_section_${sectionIndex}_title",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = if (sectionIndex == 0) 0.dp else 12.dp),
                )
            }
        }
        items(
            items = section.entries,
            key = { "hub_entry_${sectionIndex}_${it.key}" },
            span = { GridItemSpan(1) },
        ) { entry -> SiteHubCard(entry) }
    }
}

@Composable
private fun SiteHubCard(entry: SiteHubEntry) {
    ExpressiveEntryCard(
        icon = entry.icon,
        title = entry.title,
        subtitle = entry.subtitle,

        subtitleMinLines = 2,
        modifier = Modifier.height(CardHeight),
        locked = entry.locked,
        onClick = entry.onClick,
    )
}

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
            val columns = hubCardColumns(maxWidth)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = CardGridHorizontalPadding,
                    end = CardGridHorizontalPadding,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(CardGridSpacing),
                verticalArrangement = Arrangement.spacedBy(CardGridSpacing),
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
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    items(
                        items = quickActions,
                        key = { "quick_${it.key}" },
                        span = { GridItemSpan(1) },
                    ) { entry -> SiteHubCard(entry) }
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
                                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                            )
                        }
                    }
                    items(
                        items = section.entries,
                        key = { "profile_entry_${sectionIndex}_${it.key}" },
                        span = { GridItemSpan(1) },
                    ) { entry -> SiteHubCard(entry) }
                }
            }
        }
    }
}

@Composable
private fun SiteAccountCard(account: SiteAccountHeader) {
    val shape = RoundedCornerShape(32.dp)
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(76.dp),
            ) {
                AccountAvatar(
                    avatarUrl = account.avatarUrl,
                    name = account.title,
                    loggedIn = account.loggedIn,
                    signInAvailable = account.loggedIn || account.onClick != null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_profile),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = account.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (account.subtitle.isNotBlank()) {
                    Text(
                        text = account.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = account.siteLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (account.onClick != null) {
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
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) { content() }
    }
}
