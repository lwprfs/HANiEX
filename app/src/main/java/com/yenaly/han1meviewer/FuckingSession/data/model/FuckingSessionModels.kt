package com.yenaly.han1meviewer.FuckingSession.data.model

import com.yenaly.han1meviewer.logic.model.HanimeInfo

data class FuckingSessionListPage(
    val items: List<HanimeInfo>,
    val hasNext: Boolean,
)

data class FuckingSessionLink(val title: String, val url: String) {

    /** Path (+ nothing else) of [url], e.g. `/category/anal/`; used as a list route key. */
    val path: String
        get() = runCatching { java.net.URI(url).path }.getOrNull().orEmpty().ifBlank { url }
}

data class FuckingSessionSource(
    val label: String,
    val url: String,
    val height: Int,
    /** Referer the stream host expects (the embed page, not the site itself). */
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
    /** `<iframe>` embeds found in the post body, in page order. */
    val embeds: List<String>,
    /** Third-party mirror / file-host links from the post body (opened in the browser). */
    val mirrors: List<FuckingSessionLink>,
    val sources: List<FuckingSessionSource>,
    val related: List<HanimeInfo>,
)
