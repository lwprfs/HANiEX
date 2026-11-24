package com.yenaly.han1meviewer.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.component.player.SitePlayerSettings
import com.yenaly.han1meviewer.ui.component.social.LocalSocialStyle
import com.yenaly.han1meviewer.ui.component.social.SiteSocialStyle
import com.yenaly.han1meviewer.ui.component.social.SocialActionButton
import com.yenaly.han1meviewer.ui.component.social.SocialCommunityCard
import com.yenaly.han1meviewer.ui.component.social.SocialDefaults
import com.yenaly.han1meviewer.ui.component.social.SocialListStyle
import com.yenaly.han1meviewer.ui.component.social.SocialProfileHeader
import com.yenaly.han1meviewer.ui.component.social.SocialProfileStyle
import com.yenaly.han1meviewer.ui.component.social.SocialStat
import com.yenaly.han1meviewer.ui.component.social.SocialStyleConfig
import com.yenaly.han1meviewer.ui.component.social.SocialTabRow
import com.yenaly.han1meviewer.ui.component.social.SocialUserCard
import com.yenaly.han1meviewer.ui.component.social.formatSocialCount

@Composable
fun SiteSocialStylePage(site: SiteType, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = {
                    Text("${SitePlayerSettings.label(site)} · ${stringResource(R.string.social_style_title)}")
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        SiteSocialStyleScreen(site = site, modifier = Modifier.padding(padding))
    }
}

@Composable
private fun listStyleLabel(style: SocialListStyle): String = stringResource(
    when (style) {
        SocialListStyle.CLASSIC -> R.string.social_style_classic
        SocialListStyle.SHOWCASE -> R.string.social_list_showcase
        SocialListStyle.CENTERED -> R.string.social_style_centered
        SocialListStyle.COMPACT -> R.string.social_style_compact
        SocialListStyle.FLAT -> R.string.social_list_flat
    }
)

@Composable
private fun profileStyleLabel(style: SocialProfileStyle): String = stringResource(
    when (style) {
        SocialProfileStyle.CLASSIC -> R.string.social_style_classic
        SocialProfileStyle.CENTERED -> R.string.social_style_centered
        SocialProfileStyle.COMPACT -> R.string.social_style_compact
        SocialProfileStyle.IMMERSIVE -> R.string.social_profile_immersive
    }
)

@Composable
fun SiteSocialStyleScreen(site: SiteType, modifier: Modifier = Modifier) {
    @Suppress("UNUSED_VARIABLE") val rev = SiteSocialStyle.revision
    val config = SiteSocialStyle.config(site)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "search_header") {
            SettingSectionHeader(stringResource(R.string.social_style_search_section))
        }
        item(key = "search_chips") {
            StyleChips(
                options = SocialListStyle.entries.map { it to listStyleLabel(it) },
                selected = config.list,
                onSelect = { SiteSocialStyle.setListStyle(site, it) },
            )
        }
        item(key = "search_preview") {
            PreviewFrame(config) {
                SocialUserCard(
                    name = stringResource(R.string.social_style_sample_name),
                    username = "@sample",
                    bio = stringResource(R.string.social_style_sample_bio),
                    meta = "${formatSocialCount(1200)} ${stringResource(R.string.site_followers)}",
                    onClick = {},
                )
                if (SiteSocialStyle.hasCommunityList(site)) {
                    SocialCommunityCard(
                        name = stringResource(R.string.social_style_sample_community),
                        meta = "${formatSocialCount(3400)} ${stringResource(R.string.site_followers)}",
                        metaIcon = Icons.Filled.Groups,
                        onClick = {},
                    )
                }
            }
        }

        item(key = "profile_header") {
            SettingSectionHeader(stringResource(R.string.social_style_profile_section))
        }
        item(key = "profile_chips") {
            StyleChips(
                options = SocialProfileStyle.entries.map { it to profileStyleLabel(it) },
                selected = config.profile,
                onSelect = { SiteSocialStyle.setProfileStyle(site, it) },
            )
        }
        item(key = "profile_preview") {
            PreviewFrame(config) { ProfilePreview(isCommunity = false) }
        }

        if (SiteSocialStyle.hasCommunityPage(site)) {
            item(key = "community_header") {
                SettingSectionHeader(stringResource(R.string.social_style_community_section))
            }
            item(key = "community_chips") {
                StyleChips(
                    options = SocialProfileStyle.entries.map { it to profileStyleLabel(it) },
                    selected = config.community,
                    onSelect = { SiteSocialStyle.setCommunityStyle(site, it) },
                )
            }
            item(key = "community_preview") {
                PreviewFrame(config) { ProfilePreview(isCommunity = true) }
            }
        }

        item(key = "reset") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                TextButton(
                    onClick = { SiteSocialStyle.reset(site) },
                    enabled = config != SocialStyleConfig.Default,
                ) {
                    Text(
                        stringResource(R.string.social_style_reset),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> StyleChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun PreviewFrame(
    config: SocialStyleConfig,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.social_style_preview_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            CompositionLocalProvider(LocalSocialStyle provides config) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { content() }
            }
        }
    }
}

@Composable
private fun ProfilePreview(isCommunity: Boolean) {
    androidx.compose.foundation.layout.Column {
        SocialProfileHeader(
            name = stringResource(
                if (isCommunity) R.string.social_style_sample_community else R.string.social_style_sample_name
            ),
            username = if (isCommunity) null else "@sample",
            avatarShape = if (isCommunity) SocialDefaults.CommunityAvatarShape
            else androidx.compose.foundation.shape.CircleShape,
            isCommunity = isCommunity,
            bio = stringResource(R.string.social_style_sample_bio),
            actions = {
                SocialActionButton(
                    label = stringResource(R.string.fap_follow),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            },
            stats = listOf(
                SocialStat(stringResource(R.string.site_followers), 1200L),
                SocialStat(stringResource(R.string.fap_tab_posts), 48L),
                SocialStat(stringResource(R.string.fap_tab_videos), 12L),
            ),
        )
        SocialTabRow(
            labels = listOf(
                stringResource(R.string.fap_tab_posts),
                stringResource(R.string.fap_tab_photos),
                stringResource(R.string.fap_tab_videos),
            ),
            selectedIndex = 0,
            onSelect = {},
            community = isCommunity,
        )
    }
}
