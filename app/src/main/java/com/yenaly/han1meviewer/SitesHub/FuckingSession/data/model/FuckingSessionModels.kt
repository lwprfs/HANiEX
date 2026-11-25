package com.yenaly.han1meviewer.SitesHub.FuckingSession.data.model

import com.yenaly.han1meviewer.logic.model.HanimeInfo

data class FuckingSessionListPage(
    val items: List<HanimeInfo>,
    val hasNext: Boolean,
)

data class FuckingSessionLink(val title: String, val url: String) {

    val path: String
        get() = runCatching { java.net.URI(url).path }.getOrNull().orEmpty().ifBlank { url }
}

data class FuckingSessionSource(
    val label: String,
    val url: String,
    val height: Int,

    val referer: String = "",
)

data class FuckingSessionDetail(
    val code: String,
    val title: String,
    val coverUrl: String,
    val pageUrl: String,
    val date: String,
    val durationSeconds: Long,
    val categories: List<FuckingSessionLink>,
    val tags: List<FuckingSessionLink>,

    val embeds: List<String>,

    val mirrors: List<FuckingSessionLink>,
    val sources: List<FuckingSessionSource>,
    val related: List<HanimeInfo>,
)
