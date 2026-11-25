package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.base.BaseViewModel
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.NetworkRepo
import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.HanimePreview
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.util.TagLocalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PreviewViewModel(application: Application) : BaseViewModel(application) {

    private val previewCache = linkedMapOf<String, WebsiteState<HanimePreview>>()
    private val enrichAttempted = mutableSetOf<String>()
    private val enriching = mutableSetOf<String>()
    private var currentPreviewDate: String? = null

    private val _previewFlow =
        MutableStateFlow<WebsiteState<HanimePreview>>(WebsiteState.Loading)
    val previewFlow = _previewFlow.asStateFlow()

    fun getHanimePreview(date: String) {
        currentPreviewDate = date
        viewModelScope.launch {
            previewCache[date]?.let {
                _previewFlow.value = it
                return@launch
            }
            _previewFlow.value = WebsiteState.Loading
            val preview = loadPreview(date).withLocalizedTags()
            _previewFlow.value = preview
            if (preview is WebsiteState.Success) {
                previewCache[date] = preview
            }
        }
    }

    fun getCachedPreview(date: String): WebsiteState<HanimePreview>? = previewCache[date]

    private suspend fun loadPreview(date: String): WebsiteState<HanimePreview> {
        return if (Preferences.baseUrl == HANIME_URL[3]) {
            NetworkRepo.getHanimePreview(date).first { it !is WebsiteState.Loading }
        } else {
            NetworkRepo.getHanimePreviewFromSearch().first { it !is WebsiteState.Loading }
        }
    }

    fun enrichPreview(date: String, videoCode: String?) {
        if (videoCode.isNullOrBlank()) return
        val cached = previewCache[date] as? WebsiteState.Success ?: return
        val index = cached.info.previewInfo.indexOfFirst { it.videoCode == videoCode }
        if (index < 0) return
        val oldInfo = cached.info.previewInfo[index]

        if (oldInfo.introduction != null || oldInfo.tags.isNotEmpty()) return

        val key = "$date:$videoCode"
        if (key in enrichAttempted || !enriching.add(key)) return
        enrichAttempted += key
        viewModelScope.launch {
            var enriched = false
            try {
                val videoState = NetworkRepo.getHanimeVideo(videoCode)
                    .first { it !is VideoLoadingState.Loading }
                if (videoState !is VideoLoadingState.Success) return@launch

                val current = previewCache[date] as? WebsiteState.Success ?: return@launch
                val currentIndex =
                    current.info.previewInfo.indexOfFirst { it.videoCode == videoCode }
                if (currentIndex < 0) return@launch

                val video = videoState.info
                val currentOldInfo = current.info.previewInfo[currentIndex]
                val newInfo = currentOldInfo.copy(
                    title = video.chineseTitle?.takeIf { it.isNotBlank() } ?: currentOldInfo.title,
                    videoTitle = video.title,
                    introduction = video.introduction,
                    brand = video.artist?.name?.takeIf { it.isNotBlank() },
                    releaseDate = video.uploadTime?.toString() ?: currentOldInfo.releaseDate,
                    tags = TagLocalizer.localizeTags(video.tags),
                )
                val newList = current.info.previewInfo.toMutableList().also {
                    it[currentIndex] = newInfo
                }
                val updated = current.info.copy(
                    headerPicUrl = if (currentIndex == 0 && video.coverUrl.isNotBlank()) {
                        video.coverUrl
                    } else {
                        current.info.headerPicUrl
                    },
                    previewInfo = newList,
                )
                val updatedState = WebsiteState.Success(updated)
                previewCache[date] = updatedState
                enriched = true
                if (currentPreviewDate == date &&
                    _previewFlow.value is WebsiteState.Success
                ) {
                    _previewFlow.value = updatedState
                }
            } catch (_: Exception) {

            } finally {
                enriching.remove(key)

                if (!enriched) enrichAttempted.remove(key)
            }
        }
    }

    private fun WebsiteState<HanimePreview>.withLocalizedTags(): WebsiteState<HanimePreview> {
        return if (this is WebsiteState.Success) {
            WebsiteState.Success(info.withLocalizedTags())
        } else {
            this
        }
    }

    private fun HanimePreview.withLocalizedTags(): HanimePreview {
        return copy(previewInfo = previewInfo.map { info ->
            info.copy(tags = TagLocalizer.localizeTags(info.tags))
        })
    }
}
