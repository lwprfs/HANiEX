package com.yenaly.han1meviewer.Buondua.data.model

import com.yenaly.han1meviewer.logic.model.HanimeInfo

/** One page of any album listing (home, hot, collection, tag, search). */
data class BuonduaListPage(
    val items: List<HanimeInfo>,
    /** Absolute URL of the next page, read from the page's own pagination. */
    val nextUrl: String?,
    /** Heading of the listing, e.g. `2,000+ search results for "Hi"`. */
    val heading: String? = null,
)

data class BuonduaLink(val title: String, val url: String)

data class BuonduaPhoto(val url: String, val index: Int)

data class BuonduaVideo(
    /** Direct media URL exactly as published by the site. */
    val url: String,
    /** Name as published (attribute or file name). Never rewritten: the saved file keeps it. */
    val title: String,
    val posterUrl: String?,
    val fileName: String,
)

data class BuonduaAlbumPage(
    val code: String,
    /** Title exactly as the site shows it (only the "( Page x / y )" suffix is removed). */
    val title: String,
    val pageUrl: String,
    val coverUrl: String,
    val photoCount: Int?,
    val videoCount: Int?,
    val page: Int,
    val totalPages: Int,
    val photos: List<BuonduaPhoto>,
    val videos: List<BuonduaVideo>,
    val tags: List<BuonduaLink>,
    val related: List<HanimeInfo>,
)
