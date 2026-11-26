package com.yenaly.han1meviewer.SitesHub.Wumaobi.common

import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.util.translation.AppTranslation

/**
 * Wumaobi's view of [AppTranslation]. Everything goes through the WUMAOBI site, so it follows
 * the switches and uses the cache of Wumaobi's own translation settings.
 */
object WumaobiTranslation {

    /** Video titles. */
    fun title(text: String): String = AppTranslation.title(text, SiteType.WUMAOBI)

    /** Tags, categories, models, series, channels and networks. */
    fun tag(text: String): String = AppTranslation.tag(text, SiteType.WUMAOBI)
}
