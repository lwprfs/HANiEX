package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.translation.AppTranslation

fun translatedTitle(text: String): String = AppTranslation.title(text, Preferences.siteType)

fun translatedDescription(text: String): String =
    if (text.contains("http")) text else AppTranslation.description(text, Preferences.siteType)

fun translatedComment(text: String): String = AppTranslation.comment(text, Preferences.siteType)
