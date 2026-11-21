package com.yenaly.han1meviewer.RedGifs.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Navigation callbacks the shared shorts player needs (tap a tag, tap the creator). Provided once by
 * [RedGifsNavHost] so every feed grid and the single-gif screen can use them without extra parameters.
 */
data class RedGifsActions(
    val openCreator: (username: String) -> Unit = {},
    val openTag: (tag: String) -> Unit = {},
)

val LocalRedGifsActions = staticCompositionLocalOf { RedGifsActions() }
