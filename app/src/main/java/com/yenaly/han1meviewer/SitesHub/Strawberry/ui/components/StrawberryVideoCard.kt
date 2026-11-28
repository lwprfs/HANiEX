package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberryUrlUtils
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryVideoCard
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork
import com.yenaly.han1meviewer.SitesHub.Strawberry.github.GithubDownloaderStore
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteVideoCard
import com.yenaly.han1meviewer.util.DisplayTextLocalizer
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import androidx.compose.ui.platform.LocalClipboard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StrawberryVideoCardItem(
    card: StrawberryVideoCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: ((StrawberryVideoCard) -> Unit)? = null,
    isWatched: Boolean = false,
) {
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val linkCopiedText = stringResource(R.string.strawberry_video_link_copied)
    val defaultLongClick: (StrawberryVideoCard) -> Unit = { c ->
        val link = StrawberryUrlUtils.normalizeVideoUrl(
            "${StrawberryNetwork.baseUrl.trimEnd('/')}/file${c.videoId}"
        )
        clipboardScope.launch {
                clipboard.setClipEntry(
                    ClipEntry(ClipData.newPlainText("", link))
                )
            }
        GlobalToasts.show(
            linkCopiedText,
            level = GlobalToasts.ToastLevel.SUCCESS,
        )
    }
    val downloadTags by rememberStrawberryDownloadTags()
    val queuedText = stringResource(R.string.strawberry_added_to_github_queue)
    val alreadyText = stringResource(R.string.strawberry_already_in_github_queue)
    val queueLongClick: (StrawberryVideoCard) -> Unit = { c ->
        if (GithubDownloaderStore.repoReady) {
            val added = GithubDownloaderStore.addVideo(c.videoId)
            GlobalToasts.show(
                if (added) queuedText else alreadyText,
                level = if (added) GlobalToasts.ToastLevel.SUCCESS else GlobalToasts.ToastLevel.INFO,
            )
        } else {
            defaultLongClick(c)
        }
    }
    val longClickHandler = onLongClick ?: queueLongClick
    val tagText = when (downloadTags[card.videoId]) {
        StrawberryDownloadTags.Tag.DOWNLOADED -> stringResource(R.string.downloaded)
        StrawberryDownloadTags.Tag.DOWNLOADING -> stringResource(R.string.downloading)
        null -> null
    }

    SiteVideoCard(
        modifier = modifier,
        title = card.title,
        coverUrl = card.thumbUrl,
        views = card.viewsRaw?.let { DisplayTextLocalizer.localizeViews(it) },
        duration = card.duration,
        subtitle = card.uploaderName,
        onClick = onClick,
        onLongClick = { longClickHandler(card) },
        bottomStartBadge = tagText,
        isWatched = isWatched,
    )
}
