package com.yenaly.han1meviewer.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.theme.SpacingLarge
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import com.yenaly.han1meviewer.util.applicationContext

data class SiteSwitchOption(
    val name: String,
    val description: String,
    val siteType: SiteType,
    @DrawableRes val logoRes: Int?,

    val accentColor: Color,
)

val DefaultSiteSwitchOptions: List<SiteSwitchOption> = listOf(
    SiteSwitchOption(
        name = "Hanime",
        description = applicationContext.getString(R.string.site_main_site_with_h_anime),
        siteType = SiteType.HANIME,
        logoRes = R.drawable.s_switch_han1me,
        accentColor = Color(0xFFE53935),
    ),
    SiteSwitchOption(
        name = "MissAV",
        description = applicationContext.getString(R.string.site_jav_streaming_platform),
        siteType = SiteType.MISSAV,
        logoRes = R.drawable.s_switch_missav,
        accentColor = Color(0xFF1E88E5),
    ),
    SiteSwitchOption(
        name = "HentaiMama",
        description = applicationContext.getString(R.string.site_hentai_streaming_site),
        siteType = SiteType.HENTAIMAMA,
        logoRes = R.drawable.s_switch_hentaimama,
        accentColor = Color(0xFF8E24AA),
    ),
    SiteSwitchOption(
        name = "Javchu",
        description = applicationContext.getString(R.string.site_jav_library_fixed_url),
        siteType = SiteType.JAVCHU,
        logoRes = R.drawable.s_switch_javchu,
        accentColor = Color(0xFFFB8C00),
    ),
    SiteSwitchOption(
        name = "OnAir",
        description = applicationContext.getString(R.string.site_self_hosted_onion_video_site),
        siteType = SiteType.ONAIR,
        logoRes = R.drawable.s_switch_onair,
        accentColor = Color(0xFF00897B),
    ),
    SiteSwitchOption(
        name = "FapFolder",
        description = applicationContext.getString(R.string.site_community_feed_albums_and_videos),
        siteType = SiteType.FAPFOLDER,
        logoRes = R.drawable.s_switch_fapfolder,
        accentColor = Color(0xFFD81B60),
    ),
    SiteSwitchOption(
        name = "SupJav",
        description = applicationContext.getString(R.string.site_supjav_description),
        siteType = SiteType.SUPJAV,
        logoRes = R.drawable.s_switch_supjav,
        accentColor = Color(0xFF5E35B1),
    ),
    SiteSwitchOption(
        name = applicationContext.getString(R.string.site_rouvideo_name),
        description = applicationContext.getString(R.string.site_rouvideo_description),
        siteType = SiteType.ROUVIDEO,
        logoRes = R.drawable.s_switch_rouvideo,
        accentColor = Color(0xFFE53935),
    ),
    SiteSwitchOption(
        name = applicationContext.getString(R.string.site_xhamster_name),
        description = applicationContext.getString(R.string.site_xhamster_description),
        siteType = SiteType.XHAMSTER,
        logoRes = R.drawable.s_switch_xhamster,
        accentColor = Color(0xFFEF6C00),
    ),
    SiteSwitchOption(
        name = "Buondua",
        description = applicationContext.getString(R.string.site_buondua_description),
        siteType = SiteType.BUONDUA,
        logoRes = R.drawable.s_switch_buondua,
        accentColor = Color(0xFFFFB300),
    ),
    SiteSwitchOption(
        name = "Cosplaytele",
        description = applicationContext.getString(R.string.site_cosplaytele_description),
        siteType = SiteType.COSPLAYTELE,
        logoRes = R.drawable.s_switch_cosplaytele,
        accentColor = Color(0xFFE91E63),
    ),
)

private const val SitesPerPage = 6
private val TileHeight = 108.dp
private val TileGap = 10.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSwitchDialog(
    currentSite: SiteType,
    onDismiss: () -> Unit,
    onSiteSelected: (SiteType) -> Unit,
    modifier: Modifier = Modifier,
    options: List<SiteSwitchOption> = DefaultSiteSwitchOptions,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var disabled by remember { mutableStateOf(Preferences.disabledSites) }
    var pinned by remember { mutableStateOf(Preferences.pinnedSites) }
    var menuFor by remember { mutableStateOf<SiteSwitchOption?>(null) }
    val scope = rememberCoroutineScope()
    val currentSiteMessage = stringResource(R.string.site_visibility_current_site)

    val visible = options.filter { it.siteType == currentSite || it.siteType !in disabled }
    val ordered = pinned.mapNotNull { p -> visible.firstOrNull { it.siteType == p } } +
            visible.filter { it.siteType !in pinned }
    val pages = ordered.chunked(SitesPerPage)
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = SpacingLarge)
                .padding(bottom = SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                )
                            )
                        ),
                )
                Spacer(Modifier.width(SpacingNormal))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.site_switch_site),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.site_switch_hold_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = "${ordered.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TileHeight * 3 + TileGap * 2),
                    pageSpacing = 12.dp,
                ) { page ->
                    val items = pages.getOrNull(page).orEmpty()
                    Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(TileGap),
                            ) {
                                for (col in 0 until 2) {
                                    val option = items.getOrNull(row * 2 + col)
                                    if (option == null) {
                                        Spacer(Modifier.weight(1f).height(TileHeight))
                                    } else {
                                        SiteTile(
                                            option = option,
                                            isCurrent = option.siteType == currentSite,
                                            isPinned = option.siteType in pinned,
                                            modifier = Modifier.weight(1f).height(TileHeight),
                                            onClick = {
                                                if (option.siteType == currentSite) {
                                                    onDismiss()
                                                } else {
                                                    onSiteSelected(option.siteType)
                                                    onDismiss()
                                                }
                                            },
                                            onLongClick = { menuFor = option },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (pagerState.currentPage < pages.lastIndex) {
                    PageArrow(
                        forward = true,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                    )
                }
                if (pagerState.currentPage > 0) {
                    PageArrow(
                        forward = false,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                    )
                }
            }

            if (pages.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(pages.size) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == pagerState.currentPage) 18.dp else 6.dp, 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                        )
                    }
                }
            }
        }
    }

    menuFor?.let { option ->
        val isPinned = option.siteType in pinned
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(option.name) },
            text = {
                Column {
                    SiteMenuRow(
                        icon = Icons.Filled.PushPin,
                        label = stringResource(
                            if (isPinned) R.string.site_switch_unpin else R.string.site_switch_pin
                        ),
                        onClick = {
                            if (isPinned) Preferences.unpinSite(option.siteType)
                            else Preferences.pinSite(option.siteType)
                            pinned = Preferences.pinnedSites
                            menuFor = null
                            scope.launch { pagerState.scrollToPage(0) }
                        },
                    )
                    SiteMenuRow(
                        icon = Icons.Filled.VisibilityOff,
                        label = stringResource(R.string.site_switch_hide),
                        onClick = {
                            menuFor = null
                            if (Preferences.setSiteEnabled(option.siteType, false)) {
                                disabled = Preferences.disabledSites
                                pinned = Preferences.pinnedSites
                            } else {
                                GlobalToasts.show(
                                    currentSiteMessage,
                                    level = GlobalToasts.ToastLevel.WARNING,
                                )
                            }
                        },
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { menuFor = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SiteMenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PageArrow(forward: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
        shadowElevation = 3.dp,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight
                else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SiteTile(
    option: SiteSwitchOption,
    isCurrent: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shape,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (isCurrent) BorderStroke(1.5.dp, option.accentColor.copy(alpha = 0.7f)) else null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SiteLogo(option, boxSize = 52.dp, logoSize = 40.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            if (isPinned) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp),
                )
            }
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.site_current_site),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SiteLogo(option: SiteSwitchOption, boxSize: Dp, logoSize: Dp) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (option.logoRes != null) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (option.logoRes != null) {
            Image(
                painter = painterResource(option.logoRes),
                contentDescription = option.name,
                modifier = Modifier
                    .size(logoSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = option.name.take(1),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
fun SiteSwitchConfirmDialog(
    title: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
    )
}
