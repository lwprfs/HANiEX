package com.yenaly.han1meviewer.RedGifs.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object RedGifsSession {

    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> get() = _epoch

    fun bump() {
        _epoch.value = _epoch.value + 1
    }
}
