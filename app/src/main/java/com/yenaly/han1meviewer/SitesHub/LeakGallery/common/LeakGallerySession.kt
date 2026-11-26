package com.yenaly.han1meviewer.SitesHub.LeakGallery.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Bumped whenever the LeakGallery login state changes so screens can recompose. */
object LeakGallerySession {

    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> get() = _epoch

    fun bump() {
        _epoch.value = _epoch.value + 1
    }
}
