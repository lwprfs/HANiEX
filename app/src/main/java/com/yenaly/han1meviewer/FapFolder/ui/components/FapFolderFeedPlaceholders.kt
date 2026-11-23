package com.yenaly.han1meviewer.FapFolder.ui.components

import com.yenaly.han1meviewer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.FapFolder.data.model.FapFolderGroupRow
import com.yenaly.han1meviewer.FapFolder.data.model.FapFolderUserRow
import com.yenaly.han1meviewer.FapFolder.data.model.GroupVisibility
import com.yenaly.han1meviewer.ui.component.social.SocialChip
import com.yenaly.han1meviewer.ui.component.social.SocialCommunityCard
import com.yenaly.han1meviewer.ui.component.social.SocialTone
import com.yenaly.han1meviewer.ui.component.social.SocialUserCard
import com.yenaly.han1meviewer.ui.component.social.formatSocialCount
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

/** User result card. Built on the shared social kit; shows only what [FapFolderUserRow] provides. */
@Composable
fun FapFolderUserRowCard(
    user: FapFolderUserRow,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val followingLabel = stringResource(R.string.fap_following)
    val friendsLabel = stringResource(R.string.fap_friends)
    val pendingLabel = stringResource(R.string.creator_status_pending)
    val goldLabel = stringResource(R.string.fap_gold)
    val chips = buildList {
        if (user.isGold) add(SocialChip(goldLabel, SocialTone.Warning))
        if (user.isFriend) add(SocialChip(friendsLabel, SocialTone.Positive))
        else if (user.isFriendPending) add(SocialChip(pendingLabel))
        if (user.isFollowed) add(SocialChip(followingLabel, SocialTone.Accent))
    }
    SocialUserCard(
        name = user.name.ifBlank { user.username },
        username = user.username.takeIf { it.isNotBlank() && !it.equals(user.name, true) }
            ?.let { "@$it" },
        avatarUrl = user.avatarUrl,
        bio = user.bio,
        chips = chips,
        onClick = onClick,
        modifier = modifier,
    )
}

/** Community result card. Built on the shared social kit; shows only what [FapFolderGroupRow] provides. */
@Composable
fun FapFolderGroupRowCard(
    group: FapFolderGroupRow,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val privateLabel = stringResource(R.string.fap_group_private)
    val secretLabel = stringResource(R.string.fap_group_secret)
    val memberLabel = stringResource(R.string.site_following)
    val chips = buildList {
        when (group.visibility) {
            GroupVisibility.PRIVATE -> add(SocialChip(privateLabel, SocialTone.Warning))
            GroupVisibility.SECRET -> add(SocialChip(secretLabel, SocialTone.Warning))
            else -> Unit
        }
        if (group.isMember) add(SocialChip(memberLabel, SocialTone.Positive))
    }
    SocialCommunityCard(
        name = group.name,
        avatarUrl = group.avatarUrl,
        meta = group.followerCount?.let { stringResource(R.string.fap_followers, formatSocialCount(it)) },
        chips = chips,
        onClick = onClick,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun FapFolderUserRowPreview() {
    ComponentPreview {
        FapFolderUserRowCard(
            user = FapFolderUserRow(
                uid = 1L,
                username = "sample",
                name = "Sample User",
                avatarUrl = null,
                bio = "A short bio about this user.",
                profileUrl = "https://fapfolder.club/sample",
                isFollowed = true,
                isFriend = false,
                isFriendPending = false,
                isGold = true,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FapFolderGroupRowPreview() {
    ComponentPreview {
        FapFolderGroupRowCard(
            group = FapFolderGroupRow(
                groupId = 1L,
                slug = "sample_group",
                name = "Sample Group",
                avatarUrl = null,
                followerCount = 1234,
                isMember = true,
                visibility = GroupVisibility.PUBLIC,
            ),
        )
    }
}
