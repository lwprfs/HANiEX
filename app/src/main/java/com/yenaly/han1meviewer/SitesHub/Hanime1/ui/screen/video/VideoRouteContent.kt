package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.video

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_COMMENT_PREFIX
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.bridge.VideoPageHost
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.application
import kotlinx.coroutines.launch

@Composable
fun VideoRouteContent(
    videoCode: String,
    videoState: VideoLoadingState<*>,
    videoViewModel: VideoViewModel,
    commentViewModel: CommentViewModel,
    fromDownload: Boolean,
    pendingDownloadPrompt: DownloadPromptState?,
    onPendingDownloadPromptChange: (DownloadPromptState?) -> Unit,
    onRetry: () -> Unit,
    onOpenVideo: (HanimeInfo) -> Unit,
    onOpenArtist: (com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimeVideo.Artist) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onToggleSubscribe: (com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimeVideo.Artist) -> Unit,
    onToggleFavorite: (com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimeVideo) -> Unit,
    onRateVideo: (com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimeVideo, Boolean) -> Unit,
    onManageMyList: (com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimeVideo.MyList?, List<Boolean>) -> Unit,
    onQuickCheckIn: (com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity) -> Unit,
    onPrepareDownload: (String, com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimeVideo?) -> Unit,
    onConfirmDownloadPrompt: (com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimeVideo?, String?) -> Unit,
    onRequestOpenOfficialDownloadPage: () -> Unit,
    onRequestOpenDownloadPermissionSettings: () -> Unit,
    onOpenWebPage: () -> Unit,
    onOpenOriginalComic: (String) -> Unit,
    onOpenShare: (String, String) -> Unit,
    onCopyText: (String) -> Unit,
    onIntroductionLinkClick: (String) -> Unit,
    stringLongPressShare: String,
    pageHost: VideoPageHost,
    onOpenDownloaded: ((HanimeDownloadEntity) -> Unit)? = null,
    onDownloadsChanged: () -> Unit = {},
) {
    val hostUiState by videoViewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val commentBadgeCount by videoViewModel.commentBadgeCount.collectAsStateWithLifecycle()
    val disableComments = remember {
        PreferenceManager.getDefaultSharedPreferences(application)
            .getBoolean("disable_comments", false)
    }
    val relatedAsTab = !fromDownload && com.yenaly.han1meviewer.Preferences.relatedVideoAsTab
    val showRelatedDownloads = fromDownload && videoCode != "-1" && onOpenDownloaded != null
    val relatedDownloads = if (showRelatedDownloads) {
        rememberDownloadedRelatedState(videoCode)
    } else {
        null
    }
    val tabs = remember(disableComments, commentBadgeCount, fromDownload, relatedAsTab) {
        buildList {
            add(VideoTabItem(VideoTabKind.INTRODUCTION, R.string.introduction))
            if (!fromDownload && !disableComments) {
                add(VideoTabItem(VideoTabKind.COMMENT, R.string.comment, badgeCount = commentBadgeCount))
            }
            if (relatedAsTab) {
                add(VideoTabItem(VideoTabKind.RELATED, R.string.related_video))
            }
        }
    }

    LaunchedEffect(commentViewModel.code) {
        commentViewModel.getCommentIfNeeded(VIDEO_COMMENT_PREFIX, commentViewModel.code)

        launch {
            videoViewModel.hanimeVideoFlow.collect { video ->
                val total = video?.commentCount
                if (total != null && total > 0) {
                    videoViewModel.setCommentBadgeCount(total)
                }
            }
        }
        commentViewModel.videoCommentStateFlow.collect { state ->
            if (state is WebsiteState.Success) {
                commentViewModel.currentUserId = state.info.currentUserId

                val pageTotal = videoViewModel.hanimeVideoFlow.value?.commentCount
                if (pageTotal == null || pageTotal <= 0) {
                    videoViewModel.setCommentBadgeCount(state.info.videoComment.size)
                }
            }
        }
    }

    LaunchedEffect(commentViewModel.code) {
        commentViewModel.postCommentFlow.collect { state ->
            val count = (state as? WebsiteState.Success)?.info?.commentCount
            if (count != null && count > 0) {
                videoViewModel.setCommentBadgeCount(count)
            }
        }
    }

    val introduction: @Composable ((@Composable () -> Unit)?) -> Unit = { footer ->
        RenderVideoIntroductionContent(
            videoCode = videoCode,
            viewModel = videoViewModel,
            pendingDownloadPrompt = pendingDownloadPrompt,
            onPendingDownloadPromptChange = onPendingDownloadPromptChange,
            onOpenVideo = onOpenVideo,
            onOpenArtist = onOpenArtist,
            onNavigateToSearch = onNavigateToSearch,
            onToggleSubscribe = onToggleSubscribe,
            onToggleFavorite = onToggleFavorite,
            onRateVideo = onRateVideo,
            onManageMyList = onManageMyList,
            onQuickCheckIn = onQuickCheckIn,
            onPrepareDownload = onPrepareDownload,
            onConfirmDownloadPrompt = onConfirmDownloadPrompt,
            onRequestOpenOfficialDownloadPage = onRequestOpenOfficialDownloadPage,
            onRequestOpenDownloadPermissionSettings = onRequestOpenDownloadPermissionSettings,
            onOpenWebPage = onOpenWebPage,
            onOpenOriginalComic = onOpenOriginalComic,
            onOpenShare = onOpenShare,
            onCopyText = onCopyText,
            onIntroductionLinkClick = onIntroductionLinkClick,
            stringLongPressShare = stringLongPressShare,
            footer = footer,
        )
    }

    VideoScreen(
        state = videoState,
        onRetry = onRetry,
    ) {
        if (fromDownload) {

            introduction(
                if (relatedDownloads == null) {
                    null
                } else {
                    @Composable {
                        DownloadedRelatedSection(
                            state = relatedDownloads,
                            onOpen = { onOpenDownloaded?.invoke(it) },
                        )
                    }
                },
            )
        } else {
            VideoTabsContent(
                tabs = tabs,
                selectedTabIndex = hostUiState.selectedTabIndex,
                onSelectedTabChange = { videoViewModel.setSelectedTabIndex(videoCode, it) },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (tabs.getOrNull(page)?.kind) {
                    VideoTabKind.INTRODUCTION -> {
                        introduction(null)
                    }

                    VideoTabKind.RELATED -> {
                        RenderVideoRelatedContent(
                            viewModel = videoViewModel,
                            onOpenVideo = onOpenVideo,
                        )
                    }

                    else -> {
                        RenderVideoCommentContent(
                            viewModel = commentViewModel,
                            reportMessages = remember { kotlinx.coroutines.flow.MutableSharedFlow() },
                            getMessageText = { message ->
                                if (message.args.isNotEmpty()) {
                                    application.getString(message.resId, *message.args.toTypedArray())
                                } else {
                                    application.getString(message.resId)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    relatedDownloads?.let { related ->
        DownloadedRelatedDeleteDialog(state = related, onChanged = onDownloadsChanged)
    }
}
