package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.homepage

import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.Announcement

sealed interface HomeUiEvent {
    data object NavigateToPreview : HomeUiEvent
    data class OpenSearchPage(val query: String = "") : HomeUiEvent
    data class NavigateToSearchAdvanced(val params: Map<String, String>) : HomeUiEvent
    data class OpenVideo(val videoCode: String) : HomeUiEvent
    data class LongPressVideoCopy(val videoCode: String, val videoTitle: String) : HomeUiEvent
    data object ShowExitDialog : HomeUiEvent
    data class ShowAnnouncementDialog(val announcement: Announcement) : HomeUiEvent
}
