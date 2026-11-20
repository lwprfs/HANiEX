package com.yenaly.han1meviewer.RedGifs.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

data class RedGifsActions(
    val openCreator: (username: String) -> Unit = {},
    val openTag: (tag: String) -> Unit = {},
)

val LocalRedGifsActions = staticCompositionLocalOf { RedGifsActions() }
