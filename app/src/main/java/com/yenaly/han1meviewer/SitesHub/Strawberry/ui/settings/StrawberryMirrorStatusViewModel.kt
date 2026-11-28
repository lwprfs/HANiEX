package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberryConstants
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberrySitePreferences
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryMirrorStatus
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryMirrorStatusRepo
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StrawberryMirrorStatusViewModel : ViewModel() {

    val statuses = mutableStateMapOf<String, StrawberryMirrorStatus>()

    var checkingAll by mutableStateOf(false)
        private set

    private var bulkJob: Job? = null

    val selectedHost: String
        get() = StrawberrySitePreferences.currentHostname()

    fun checkOne(host: String) {
        if (statuses[host] is StrawberryMirrorStatus.Checking) return
        viewModelScope.launch {
            statuses[host] = StrawberryMirrorStatus.Checking(host)
            statuses[host] = StrawberryMirrorStatusRepo.check(host)
        }
    }

    fun checkAll() {
        if (checkingAll) return
        bulkJob?.cancel()
        checkingAll = true
        bulkJob = viewModelScope.launch {
            try {
                StrawberryMirrorStatusRepo.checkAll(StrawberryConstants.STRAWBERRY_HOSTNAME.toList()) { result ->
                    statuses[result.host] = result
                }
            } finally {
                checkingAll = false
            }
        }
    }

    fun switchTo(host: String) {
        if (host == StrawberrySitePreferences.currentHostname()) return
        val idx = StrawberryConstants.STRAWBERRY_HOSTNAME.indexOfFirst {
            it.equals(host, ignoreCase = true)
        }
        if (idx < 0) return
        StrawberrySitePreferences.setMirrorIndex(idx)
        StrawberryNetwork.rebuildNetwork()
        statuses.clear()
        checkAll()
    }

    override fun onCleared() {
        super.onCleared()
        bulkJob?.cancel()
    }
}
