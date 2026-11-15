package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.videoUrlRegex

object ClipboardVideoLinkGuard {

    private const val SELF_SHARE_SUFFIX = "- From Han1meViewer -"

    fun findNewVideoCode(): String? {
        val clipText = textFromClipboard?.toString()?.trim().orEmpty()
        if (clipText.isEmpty()) return null

        if (clipText == Preferences.lastHandledClipboard) return null
        Preferences.lastHandledClipboard = clipText

        if (clipText.contains(SELF_SHARE_SUFFIX)) return null

        return videoUrlRegex.find(clipText)?.groupValues?.getOrNull(1)
    }
}
