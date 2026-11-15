package com.yenaly.han1meviewer.RouVideo.data.model

data class RouVideoUser(
    val id: String,
    val name: String = "",
    val email: String = "",
) {
    val displayName: String get() = name.ifBlank { email.substringBefore('@') }.ifBlank { id }
    val initial: String get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
}

data class RouVideoAuthState(
    val loggedIn: Boolean = false,
    val user: RouVideoUser? = null,
)

data class RouVideoViewerState(
    val favorite: Boolean = false,

    val positionSec: Double = 0.0,
)

data class RouVideoFollowRow(
    val seriesId: String,
    val title: String,
    val titleZh: String? = null,
    val coverUrl: String = "",
    val isPoster: Boolean = false,
    val tags: List<String> = emptyList(),
    val epLabel: String? = null,
    val episodeCount: Int = 0,
    val completed: Boolean = false,

    val unseen: Int = 0,
    val lastSeenEpisode: Int? = null,

    val playVideoId: String? = null,
    val playKind: String? = null,
    val playNumber: Int = 0,
)

data class RouVideoFollowsPage(
    val rows: List<RouVideoFollowRow>,
    val pageNum: Int,
    val totalPage: Int,
    val total: Int,
    val newCount: Int,
    val signedIn: Boolean,
) {
    val hasNext: Boolean get() = pageNum < totalPage
}

sealed interface RouVideoSessionCheck {
    data object LoggedOut : RouVideoSessionCheck
    data class LoggedIn(val user: RouVideoUser?) : RouVideoSessionCheck
    data class Unknown(val error: Throwable) : RouVideoSessionCheck
}
