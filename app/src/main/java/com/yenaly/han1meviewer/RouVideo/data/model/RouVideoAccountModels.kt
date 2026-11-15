package com.yenaly.han1meviewer.RouVideo.data.model

/** The signed-in rou.video account, as reported by the site's session. */
data class RouVideoUser(
    val id: String,
    val name: String = "",
    val email: String = "",
) {
    val displayName: String get() = name.ifBlank { email.substringBefore('@') }.ifBlank { id }
    val initial: String get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
}

/** Overall sign-in state shared by every RouVideo screen. */
data class RouVideoAuthState(
    val loggedIn: Boolean = false,
    val user: RouVideoUser? = null,
)

/** What `GET /api/v/{id}` says about the signed-in viewer and one video. */
data class RouVideoViewerState(
    val favorite: Boolean = false,
    /** Saved position in seconds, 0 when none. */
    val positionSec: Double = 0.0,
)

/** A row of "My follows" (`/follows`). */
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
    /** New episodes since the viewer last said "got it". */
    val unseen: Int = 0,
    val lastSeenEpisode: Int? = null,
    /** Video id of the episode to open (resume / next / first), null when unknown. */
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

/** Result of asking whether the stored session is still accepted by the site. */
sealed interface RouVideoSessionCheck {
    data object LoggedOut : RouVideoSessionCheck
    data class LoggedIn(val user: RouVideoUser?) : RouVideoSessionCheck
    data class Unknown(val error: Throwable) : RouVideoSessionCheck
}
