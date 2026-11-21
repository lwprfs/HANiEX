package com.yenaly.han1meviewer.ui.screen.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.ui.graphics.vector.ImageVector
import com.yenaly.han1meviewer.R

enum class ContentLibraryEntry(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val requiresLogin: Boolean = false,
) {
    Favorite(
        icon = Icons.Filled.Favorite,
        titleRes = R.string.fav_video,
        descriptionRes = R.string.content_library_favorite_desc,
        requiresLogin = true,
    ),
    WatchLater(
        icon = Icons.Filled.WatchLater,
        titleRes = R.string.watch_later,
        descriptionRes = R.string.content_library_watch_later_desc,
        requiresLogin = true,
    ),
    Playlist(
        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
        titleRes = R.string.play_list,
        descriptionRes = R.string.content_library_playlist_desc,
        requiresLogin = true,
    ),
    History(
        icon = Icons.Filled.History,
        titleRes = R.string.watch_history,
        descriptionRes = R.string.content_library_history_desc,
    ),
    Download(
        icon = Icons.Filled.Download,
        titleRes = R.string.download,
        descriptionRes = R.string.content_library_download_desc,
    ),
    PhotoDownloads(
        icon = Icons.Filled.PhotoLibrary,
        titleRes = R.string.hub_image_downloads,
        descriptionRes = R.string.hub_image_downloads_desc,
    ),
    ShortDownloads(
        icon = Icons.Filled.PlayCircle,
        titleRes = R.string.hub_gif_downloads,
        descriptionRes = R.string.hub_gif_downloads_desc,
    ),
}
