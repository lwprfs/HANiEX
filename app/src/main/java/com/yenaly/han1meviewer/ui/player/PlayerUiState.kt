package com.yenaly.han1meviewer.ui.player

enum class PlayerState {

    IDLE,

    PREPARING,

    BUFFERING,

    PLAYING,

    PAUSED,

    COMPLETED,

    ERROR,
}

data class PlayerUiState(

    val state: PlayerState = PlayerState.IDLE,

    val title: String = "",

    val qualities: List<String> = emptyList(),

    val currentQualityIndex: Int = 0,

    val speedIndex: Int = PlayerDefaults.DEF_SPEED_INDEX,

    val isLooping: Boolean = false,

    val isGestureLocked: Boolean = false,

    val superResolutionIndex: Int = PlayerDefaults.DEF_SUPER_RESOLUTION_INDEX,

    val videoWidth: Int = 0,

    val videoHeight: Int = 0,

    val posterUrl: String? = null,

    val hasMedia: Boolean = false,

    /**
     * 总时长（ms），未知为 0。
     *
     * 由预加载在起播前回填，供底栏时长/进度条分母在起播前展示；
     * 起播后以 [PlayerProgress.durationMs] 为准。
     */
    val durationMs: Long = 0L,
)

data class PlayerProgress(

    val positionMs: Long = 0L,

    val durationMs: Long = 0L,

    val bufferedPercent: Int = 0,
)

sealed interface PlayerEvent {

    data object RequestWifiConfirmation : PlayerEvent

    data class ShowResumeButton(val savedPositionMs: Long) : PlayerEvent
}
