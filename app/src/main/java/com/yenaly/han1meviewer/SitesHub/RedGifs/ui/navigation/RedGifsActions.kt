package com.yenaly.han1meviewer.SitesHub.RedGifs.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator

import androidx.compose.runtime.staticCompositionLocalOf

data class RedGifsActions(
    val openCreator: (username: String) -> Unit = {},
    val openTag: (tag: String) -> Unit = {},
)

val LocalRedGifsActions = staticCompositionLocalOf { RedGifsActions() }
