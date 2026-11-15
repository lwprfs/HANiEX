package com.yenaly.han1meviewer.ui.player

import androidx.annotation.IntRange
import com.yenaly.han1meviewer.ui.player.PlayerDefaults.speedArray

object PlayerDefaults {

    const val GESTURE_THRESHOLD = 10

    const val DEF_PROGRESS_SLIDE_SENSITIVITY = 5

    const val DEF_COUNTDOWN_SEC = 10

    const val DEF_SPEED = 1.0f

    const val DEF_SPEED_INDEX = 4

    const val DEF_LONG_PRESS_SPEED_TIMES = 2.5f

    const val DEF_SUPER_RESOLUTION_INDEX = 0

    val speedArray = floatArrayOf(
        0.1f, 0.3f, 0.5f, 0.75f,
        1.0f, 1.25f, 1.5f, 1.75f,
        2.0f, 2.25f, 2.5f, 2.75f,
        3.0f,
    )

    val speedStringArray = Array(speedArray.size) { "${speedArray[it]}x" }

    fun toRealSensitivity(@IntRange(from = 1, to = 9) sensitivity: Int): Int {
        return when (sensitivity) {
            1, 2, 3, 4, 5 -> sensitivity
            6 -> 7
            7 -> 10
            8 -> 20
            9 -> 40
            else -> DEF_PROGRESS_SLIDE_SENSITIVITY
        }
    }
}
