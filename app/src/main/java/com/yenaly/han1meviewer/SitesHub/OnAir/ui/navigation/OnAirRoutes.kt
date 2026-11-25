package com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import kotlinx.serialization.Serializable

@Serializable
object OnAirGateRoute : NavKey

@Serializable
object OnAirHomeRoute : NavKey

@Serializable
data class OnAirSearchRoute(
    val query: String? = null,
    val field: String? = null,
    val length: String? = null,
    val date: String? = null,
    val sort: String? = null,
    val tag: String? = null,
) : NavKey

@Serializable
data class OnAirVideoRoute(
    val videoId: String,
    val preferredQuality: String? = null,
) : NavKey

@Serializable
data class OnAirUserRoute(
    val username: String,
) : NavKey

@Serializable
data class OnAirTagRoute(
    val tagName: String,
) : NavKey

@Serializable
object OnAirTagsIndexRoute : NavKey

@Serializable
object OnAirPicturesRoute : NavKey

@Serializable
object OnAirHistoryRoute : NavKey

@Serializable
object OnAirAnnouncementsRoute : NavKey

@Serializable
object OnAirChatRoute : NavKey

@Serializable
object OnAirSettingsRoute : NavKey

@Serializable
object OnAirAccountSettingsRoute : NavKey

@Serializable
object OnAirCookieRoute : NavKey

@Serializable
object OnAirLoginRoute : NavKey

@Serializable
object OnAirRegisterRoute : NavKey
