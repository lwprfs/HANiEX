package com.yenaly.han1meviewer.XXXTube.data.model

import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.logic.model.HanimeInfo

/** One page of video cards. [nextUrl] is the absolute URL of the next page, or null on the last page. */
data class XXXTubeListPage(
    val items: List<HanimeInfo>,
    val nextUrl: String?,
)

/** A model or category card. [path] is the site-relative page path, e.g. `/models/some-slug/`. */
data class XXXTubeEntity(
    val title: String,
    val coverUrl: String,
    val path: String,
    val kind: XXXTubeConstants.EntityKind,
) {
    val listingPath: String
        get() = XXXTubeConstants.listingPathFor(path)
}

data class XXXTubeEntityPage(
    val items: List<XXXTubeEntity>,
    val nextUrl: String?,
)

data class XXXTubeLink(val title: String, val url: String) {

    val path: String
        get() = runCatching { java.net.URI(url).path }.getOrNull().orEmpty().ifBlank { url }

    /** Where tapping this link should go: a video listing. */
    val listingPath: String
        get() = XXXTubeConstants.listingPathFor(path)
}

data class XXXTubeSource(
    val label: String,
    val url: String,
    val height: Int,
    val referer: String = "",
)

data class XXXTubeDetail(
    val code: String,
    val title: String,
    val coverUrl: String,
    val pageUrl: String,
    val date: String,
    val durationSeconds: Long,
    val views: String,
    val models: List<XXXTubeLink>,
    val categories: List<XXXTubeLink>,
    val tags: List<XXXTubeLink>,
    val sources: List<XXXTubeSource>,
    val related: List<HanimeInfo>,
)
