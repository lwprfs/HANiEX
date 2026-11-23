package com.yenaly.han1meviewer.FapFolder.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import kotlinx.serialization.Serializable

@Serializable
object FapFolderHomeRoute : NavKey

@Serializable
data class FapFolderPostRoute(
    val slugOrId: String,
    val autoPlay: Boolean = false,
) : NavKey

@Serializable
data class FapFolderSearchRoute(
    val query: String? = null,
    val isHashtag: Boolean = false,
    val tab: String = "posts",
) : NavKey

@Serializable
data class FapFolderHashtagRoute(
    val tag: String,
) : NavKey

@Serializable
data class FapFolderGroupRoute(
    val slug: String,
    val tab: String = "posts",
) : NavKey

@Serializable
data class FapFolderProfileRoute(
    val username: String,
    val tab: String = "posts",
) : NavKey

@Serializable
data class FapFolderAlbumRoute(
    val albumId: String,
) : NavKey

@Serializable
object FapFolderHistoryRoute : NavKey

@Serializable
object FapFolderSearchHistoryRoute : NavKey

@Serializable
object FapFolderSettingsRoute : NavKey

@Serializable
object FapFolderCookiesRoute : NavKey

@Serializable
object FapFolderImageFolderRoute : NavKey

@Serializable
object FapFolderLoginRoute : NavKey

@Serializable
object FapFolderRegisterRoute : NavKey

@Serializable
object FapFolderAccountRoute : NavKey

@Serializable
object FapFolderAccountSettingsRoute : NavKey

@Serializable
object FapFolderPrivacyRoute : NavKey

@Serializable
object FapFolderGateRoute : NavKey

@Serializable
object FapFolderFollowedCommunitiesRoute : NavKey

@Serializable
object FapFolderSavedRoute : NavKey
