package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.gate

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryProbeFailure

sealed interface StrawberryGateState {

    data object Initializing : StrawberryGateState
    data object CheckingTor : StrawberryGateState
    data object Probing : StrawberryGateState
    data object Passed : StrawberryGateState

    data class TorBlocked(
        val reason: String,
        val torFailure: TorFailureKind? = null,
    ) : StrawberryGateState

    data class ContentError(
        val reason: String,
        val suggestSwitchSite: Boolean = false,
        val suggestWait: Boolean = false,
        val suggestRelogin: Boolean = false,
        val probeFailure: StrawberryProbeFailure? = null,
    ) : StrawberryGateState

    enum class TorFailureKind {
        Timeout,
        NetworkUnreachable,
        NonTorExit,
        Unknown,
    }

    companion object {
        val Saver: Saver<StrawberryGateState, Any> = listSaver(
            save = { state ->
                when (state) {
                    Initializing -> listOf("I")
                    CheckingTor -> listOf("T")
                    Probing -> listOf("P")
                    Passed -> listOf("OK")
                    is TorBlocked -> listOf(
                        "TB",
                        state.reason,
                        state.torFailure?.name ?: "",
                    )
                    is ContentError -> listOf(
                        "CE",
                        state.reason,
                        state.suggestSwitchSite,
                        state.suggestWait,
                        state.suggestRelogin,
                        state.probeFailure?.name ?: "",
                    )
                }
            },
            restore = { list ->
                when (list[0] as String) {
                    "I" -> Initializing
                    "T" -> CheckingTor
                    "P" -> Probing
                    "OK" -> Passed
                    "TB" -> TorBlocked(
                        reason = list[1] as String,
                        torFailure = (list[2] as String).takeIf { it.isNotBlank() }
                            ?.let { runCatching { TorFailureKind.valueOf(it) }.getOrNull() },
                    )
                    "CE" -> ContentError(
                        reason = list[1] as String,
                        suggestSwitchSite = list[2] as Boolean,
                        suggestWait = list[3] as Boolean,
                        suggestRelogin = list[4] as Boolean,
                        probeFailure = (list[5] as String).takeIf { it.isNotBlank() }
                            ?.let { runCatching { StrawberryProbeFailure.valueOf(it) }.getOrNull() },
                    )
                    else -> Initializing
                }
            },
        )
    }
}
