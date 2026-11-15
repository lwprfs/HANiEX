package com.yenaly.han1meviewer.MissAV.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Logged-in MissAV account. [name] is what the site reports, [login] is what the user typed. */
data class MissAvUser(
    val name: String?,
    val login: String?,
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: login?.takeIf { it.isNotBlank() } ?: ""
}

data class MissAvActressLink(
    val name: String,
    /** Site-relative path, e.g. `/dm289/en/actresses/name`. */
    val path: String,
)

data class MissAvActress(
    val id: String?,
    val name: String,
    val avatarUrl: String,
    /** Site-relative path used to open the actress page. */
    val path: String,
)

data class MissAvPlaylist(
    val key: String,
    val name: String,
    val coverUrl: String?,
    val info: String?,
)

/** One row of the "add to playlist" sheet (`GET /api/playlists/{dvdId}`). */
data class MissAvPlaylistOption(
    val key: String,
    val name: String,
    val isAdded: Boolean,
)

data class MissAvListPage<T>(
    val items: List<T>,
    val hasNext: Boolean,
)

/** Result of `GET /api/items/{key}/view` and `GET /api/actresses/{id}/view`. */
data class MissAvSavedState(
    val loggedIn: Boolean,
    val saved: Boolean,
)

@Serializable
data class MissAvUserDto(
    val name: String? = null,
)

@Serializable
data class MissAvLoginResponse(
    val user: MissAvUserDto? = null,
)

@Serializable
data class MissAvViewResponse(
    val result: String? = null,
    val user: MissAvUserDto? = null,
    val saved: Boolean = false,
)

@Serializable
data class MissAvPlaylistOptionDto(
    @SerialName("is_added") val isAdded: Boolean = false,
    val key: String,
    val name: String = "",
)

@Serializable
data class MissAvPlaylistOptionsResponse(
    val data: List<MissAvPlaylistOptionDto> = emptyList(),
)

/** Header block of an actress page (avatar, name, measurements, birthday). */
data class MissAvActressProfile(
    val name: String?,
    val avatarUrl: String?,
    val details: List<String>,
)

/** One post of an actress' news feed (`GET /en/api/actresses/{id}/tweets`). */
data class MissAvTweet(
    val id: String,
    val text: String,
    val createdAt: String,
    val createdAtHuman: String,
    val media: List<String>,
)

data class MissAvTweetsPage(
    val items: List<MissAvTweet>,
    val nextCursor: String?,
)
