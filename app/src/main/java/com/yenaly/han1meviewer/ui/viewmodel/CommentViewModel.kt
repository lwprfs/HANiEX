package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.base.BaseViewModel
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.CommentPlace
import com.yenaly.han1meviewer.logic.model.PostCommentResult
import com.yenaly.han1meviewer.logic.model.ReportReason
import com.yenaly.han1meviewer.logic.model.VideoCommentArgs
import com.yenaly.han1meviewer.logic.model.VideoComments
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.screen.video.CommentSortType
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import com.yenaly.han1meviewer.util.HAnalytics
import com.yenaly.han1meviewer.util.applicationContext
import com.yenaly.han1meviewer.util.loadAssetAs
import com.yenaly.han1meviewer.util.unsafeLazy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommentViewModel(application: Application) : BaseViewModel(application) {

    data class CommentUiState(
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0,
        val childCommentId: String? = null,
    )

    lateinit var code: String

    private val commentUiStateMap = mutableMapOf<String, CommentUiState>()

    var currentUserId: String? = null

    private val _reportMessage = MutableSharedFlow<Message>()
    val reportMessage = _reportMessage.asSharedFlow()
    data class Message(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    )

    private val _videoCommentStateFlow =
        MutableStateFlow<WebsiteState<VideoComments>>(WebsiteState.Loading)
    val videoCommentStateFlow = _videoCommentStateFlow.asStateFlow()

    private val _videoReplyStateFlow =
        MutableStateFlow<WebsiteState<VideoComments>>(WebsiteState.Loading)
    val videoReplyStateFlow = _videoReplyStateFlow.asStateFlow()

    private val _videoCommentFlow = MutableStateFlow(emptyList<VideoComments.VideoComment>())
    val videoCommentFlow = _videoCommentFlow.asStateFlow()

    private var commentRequestJob: Job? = null

    private val _hasMoreComments = MutableStateFlow(false)
    val hasMoreCommentsFlow = _hasMoreComments.asStateFlow()

    private var nextCommentOffset: Int? = null
    private var currentCommentType: String? = null
    private var loadingMoreComments = false
    private var loadMoreJob: Job? = null

    var lastCommentCode: String? = null
        private set

    private val _videoReplyFlow = MutableStateFlow(emptyList<VideoComments.VideoComment>())
    val videoReplyFlow = _videoReplyFlow.asStateFlow()

    private val _postCommentFlow =
        MutableSharedFlow<WebsiteState<PostCommentResult>>(replay = 0)
    val postCommentFlow = _postCommentFlow.asSharedFlow()

    private val _postReplyFlow =
        MutableSharedFlow<WebsiteState<PostCommentResult>>(replay = 0)
    val postReplyFlow = _postReplyFlow.asSharedFlow()

    private val _commentLikeFlow =
        MutableSharedFlow<WebsiteState<VideoCommentArgs>>(replay = 0)
    val commentLikeFlow = _commentLikeFlow.asSharedFlow()
    val reportReason by unsafeLazy {
        loadAssetAs<List<ReportReason>>("report_reason.json").orEmpty()
    }

    private val _currentSortType = MutableStateFlow(CommentSortType.LATEST)
    val currentSortType = _currentSortType.asStateFlow()
    fun setSortType(type: CommentSortType) {
        _currentSortType.value = type
    }

    fun getCommentUiState(code: String): CommentUiState {
        return commentUiStateMap[code] ?: CommentUiState()
    }

    fun setCommentScrollState(
        code: String,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ) {
        val current = commentUiStateMap[code] ?: CommentUiState()
        commentUiStateMap[code] = current.copy(
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
        )
    }

    fun setChildCommentId(code: String, childCommentId: String?) {
        val current = commentUiStateMap[code] ?: CommentUiState()
        commentUiStateMap[code] = current.copy(childCommentId = childCommentId)
    }

    fun clearCommentData(){
        commentRequestJob?.cancel()
        commentRequestJob = null
        loadMoreJob?.cancel()
        loadMoreJob = null
        _hasMoreComments.value = false
        nextCommentOffset = null
        _videoCommentFlow.value = emptyList()
    }
    fun getComment(type: String, code: String) {
        lastCommentCode = code
        currentCommentType = type
        _hasMoreComments.value = false
        nextCommentOffset = null
        commentRequestJob?.cancel()
        loadMoreJob?.cancel()
        commentRequestJob = viewModelScope.launch {
            _videoCommentStateFlow.value = WebsiteState.Loading
            NetworkRepo.getComments(type, code).collect { state ->
                _videoCommentStateFlow.value = state
                if (state is WebsiteState.Success) {
                    _hasMoreComments.value = state.info.hasMoreComments
                    nextCommentOffset = state.info.nextCommentOffset
                }
                _videoCommentFlow.update { prevList ->
                    when (state) {
                        is WebsiteState.Success -> state.info.videoComment
                        is WebsiteState.Loading -> emptyList()
                        else -> prevList
                    }
                }
            }
        }
    }

    fun loadMoreComments() {
        val type = currentCommentType ?: return
        val code = lastCommentCode ?: return
        if (!_hasMoreComments.value || loadingMoreComments) return
        val offset = nextCommentOffset ?: return
        loadingMoreComments = true
        loadMoreJob = viewModelScope.launch {
            try {
                NetworkRepo.getComments(type, code, offset).collect { state ->
                    if (state is WebsiteState.Success && lastCommentCode == code) {
                        _videoCommentFlow.update { prevList ->
                            val prevIds = prevList.mapNotNull { it.id }.toSet()
                            prevList + state.info.videoComment.filter {
                                it.id == null || it.id !in prevIds
                            }
                        }
                        _hasMoreComments.value = state.info.hasMoreComments
                        nextCommentOffset = state.info.nextCommentOffset
                    }
                }
            } finally {
                loadingMoreComments = false
            }
        }
    }

    fun getCommentIfNeeded(type: String, code: String) {
        if (code == lastCommentCode && _videoCommentStateFlow.value is WebsiteState.Success) return
        getComment(type, code)
    }

    fun updateComments(comments: List<VideoComments.VideoComment>) {
        _videoCommentFlow.update { comments }
    }

    fun getCommentReply(commentId: String) {
        viewModelScope.launch {

            _videoReplyStateFlow.value = WebsiteState.Loading
            NetworkRepo.getCommentReply(commentId).collect { state ->
                _videoReplyStateFlow.value = state
                _videoReplyFlow.update { prevList ->
                    when (state) {
                        is WebsiteState.Success -> state.info.videoComment
                        is WebsiteState.Loading -> emptyList()
                        else -> prevList
                    }
                }
            }
        }
    }

    fun postComment(
        currentUserId: String,
        targetUserId: String,
        type: String,
        text: String,
    ) {
        viewModelScope.launch {
            NetworkRepo.postComment(csrfToken, currentUserId, targetUserId, type, text)
                .collect { state ->
                    _postCommentFlow.emit(state)
                    logCommentPostEvent(type = type, isReply = false, state = state)

                    val newComment = (state as? WebsiteState.Success)?.info?.comment
                    if (newComment != null) {
                        _videoCommentFlow.update { prev ->
                            val key = newComment.id
                            if (key != null && prev.any { it.id == key }) {
                                prev
                            } else {
                                listOf(newComment) + prev
                            }
                        }
                    }
                }
        }
    }

    fun postReply(
        replyCommentId: String,
        text: String,
    ) {
        viewModelScope.launch {
            NetworkRepo.postCommentReply(csrfToken, replyCommentId, text)
                .collect { state ->
                    _postReplyFlow.emit(state)
                    logCommentPostEvent(type = null, isReply = true, state = state)
                    val result = (state as? WebsiteState.Success)?.info ?: return@collect

                    result.comment?.let { newReply ->
                        _videoReplyFlow.update { prev -> prev + newReply }
                    }

                    result.parentCommentId?.let { parentId ->
                        _videoCommentFlow.update { prev ->
                            prev.map { comment ->
                                if (comment.replyTargetIdOrNull == parentId) {
                                    comment.copy(
                                        hasMoreReplies = true,
                                        replyCount = (comment.replyCount ?: 0) + 1,
                                    )
                                } else {
                                    comment
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun logCommentPostEvent(
        type: String?,
        isReply: Boolean,
        state: WebsiteState<PostCommentResult>,
    ) {
        when (state) {
            is WebsiteState.Success -> HAnalytics.logEvent(
                FirebaseConstants.EVENT_COMMENT_POST,
                FirebaseConstants.PARAM_METHOD to (type ?: "reply"),
                FirebaseConstants.PARAM_RESULT to "success",
            )

            is WebsiteState.Error -> HAnalytics.logEvent(
                FirebaseConstants.EVENT_COMMENT_POST,
                FirebaseConstants.PARAM_METHOD to (type ?: "reply"),
                FirebaseConstants.PARAM_RESULT to "failure",
                FirebaseConstants.PARAM_REASON to state.throwable.message,
            )

            WebsiteState.Loading -> Unit
        }
    }

    fun likeComment(
        isPositive: Boolean, commentPosition: Int,
        comment: VideoComments.VideoComment, likeCommentStatus: Boolean = false,
        unlikeCommentStatus: Boolean = false,
    ) = likeCommentInternal(
        CommentPlace.COMMENT, isPositive, commentPosition,
        comment, likeCommentStatus, unlikeCommentStatus
    )

    fun likeChildComment(
        isPositive: Boolean, commentPosition: Int,
        comment: VideoComments.VideoComment, likeCommentStatus: Boolean = false,
        unlikeCommentStatus: Boolean = false,
    ) = likeCommentInternal(
        CommentPlace.CHILD_COMMENT, isPositive, commentPosition,
        comment, likeCommentStatus, unlikeCommentStatus
    )

    private fun likeCommentInternal(
        commentPlace: CommentPlace,
        isPositive: Boolean,
        commentPosition: Int,
        comment: VideoComments.VideoComment,
        likeCommentStatus: Boolean = false,
        unlikeCommentStatus: Boolean = false,
    ) {
        viewModelScope.launch {
            NetworkRepo.likeComment(
                csrfToken,
                commentPlace,
                comment.post.foreignId,
                isPositive,
                comment.post.commentLikesCount ?: 0,
                comment.post.commentLikesSum ?: 0,
                likeCommentStatus,
                unlikeCommentStatus,
                commentPosition, comment
            ).collect { argState ->
                _commentLikeFlow.emit(argState)
                if (argState is WebsiteState.Success) {
                    when (commentPlace) {
                        CommentPlace.COMMENT -> _videoCommentFlow.update { prevList ->
                            prevList.map { item ->
                                if (item.reportableId == comment.reportableId){
                                    item.handleCommentLike(argState.info)
                                } else {
                                    item
                                }
                            }

                        }

                        CommentPlace.CHILD_COMMENT -> _videoReplyFlow.update { prevList ->
                            prevList.map { item ->
                                if (item.reportableId == comment.reportableId){
                                    item.handleCommentLike(argState.info)
                                } else {
                                    item
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private fun VideoComments.VideoComment.handleCommentLike(
        args: VideoCommentArgs,
    ) = if (args.isPositive) {
        this.incLikesCount(cancel = post.likeCommentStatus)
    } else {
        this.decLikesCount(cancel = post.unlikeCommentStatus)
    }

    fun handleCommentLike(args: VideoCommentArgs) {
        if (args.isPositive) {
            if (args.comment.post.likeCommentStatus) {
                GlobalToasts.show(applicationContext.getString(R.string.cancel_thumb_up_success), level = GlobalToasts.ToastLevel.SUCCESS)
            } else {
                GlobalToasts.show(applicationContext.getString(R.string.thumb_up_success), level = GlobalToasts.ToastLevel.SUCCESS)
            }
        } else {
            if (args.comment.post.unlikeCommentStatus) {
                GlobalToasts.show(applicationContext.getString(R.string.cancel_thumb_down_success), level = GlobalToasts.ToastLevel.SUCCESS)
            } else {
                GlobalToasts.show(applicationContext.getString(R.string.thumb_down_success), level = GlobalToasts.ToastLevel.SUCCESS)
            }
        }
    }

    fun reportComment(
        reason: String,
        currentUserId: String?,
        redirectUrl: String,
        reportableType: String?,
        reportableId: String?
    ){
        viewModelScope.launch {
            Log.i("ReportComment", "reporting comment")
            NetworkRepo.reportComment(
                csrfToken = csrfToken,
                reason = reason,
                currentUserId = currentUserId,
                redirectUrl = redirectUrl,
                reportableType = reportableType,
                reportableId = reportableId
            ).collect { state ->
                when(state){
                    is WebsiteState.Error -> {
                        _reportMessage.emit(
                            Message(
                                R.string.report_failed,
                                listOf(state.throwable.message ?: "unknown")
                            )
                        )
                    }
                    WebsiteState.Loading -> {

                    }
                    is WebsiteState.Success<*> -> {
                        _reportMessage.emit(Message(R.string.report_success))
                    }
                }
            }
        }
    }
    fun clearVideoReplyList() { _videoReplyFlow.value = emptyList() }
}
