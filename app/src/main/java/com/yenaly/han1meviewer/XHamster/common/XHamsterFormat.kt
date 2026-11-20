package com.yenaly.han1meviewer.XHamster.common

import com.yenaly.han1meviewer.util.compactCount

object XHamsterFormat {

    fun duration(seconds: Long?): String? {
        if (seconds == null || seconds <= 0) return null
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun count(value: Long?): String? = value.compactCount()
}
