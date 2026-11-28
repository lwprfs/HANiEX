package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import kotlinx.serialization.Serializable

@Serializable
object StrawberryGateRoute : NavKey

@Serializable
object StrawberryHomeRoute : NavKey

@Serializable
data class StrawberrySearchRoute(
    val query: String? = null,
    val field: String? = null,
    val length: String? = null,
    val date: String? = null,
    val sort: String? = null,
    val tag: String? = null,
) : NavKey

@Serializable
data class StrawberryVideoRoute(
    val videoId: String,
    val preferredQuality: String? = null,
) : NavKey

@Serializable
data class StrawberryUserRoute(
    val username: String,
) : NavKey

@Serializable
data class StrawberryTagRoute(
    val tagName: String,
) : NavKey

@Serializable
object StrawberryTagsIndexRoute : NavKey

@Serializable
object StrawberryPicturesRoute : NavKey

@Serializable
object StrawberryHistoryRoute : NavKey

@Serializable
object StrawberryAnnouncementsRoute : NavKey

@Serializable
object StrawberryChatRoute : NavKey

@Serializable
object StrawberrySettingsRoute : NavKey

@Serializable
object StrawberryAccountSettingsRoute : NavKey

@Serializable
object StrawberryCookieRoute : NavKey

@Serializable
object StrawberryLoginRoute : NavKey

@Serializable
object StrawberryRegisterRoute : NavKey

@Serializable
object StrawberryGithubDownloaderRoute : NavKey
