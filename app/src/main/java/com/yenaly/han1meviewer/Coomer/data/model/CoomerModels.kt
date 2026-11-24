package com.yenaly.han1meviewer.Coomer.data.model

/** A post tile (post search results, creator page). */
data class CoomerCard(
    val slug: String,
    val title: String,
    val coverUrl: String,
    val creatorName: String = "",
    val date: String = "",
)

/** A creator (OnlyFans / Fansly / ... account). [slug] is `service/id[/name]`. */
data class CoomerTerm(
    val slug: String,
    val name: String,
    val thumbUrl: String = "",
    val service: String = "",
) {
    val path: String
        get() = "/u/" + slug.trim('/')
}

data class CoomerListPage(
    val cards: List<CoomerCard>,
    val terms: List<CoomerTerm>,
    val nextUrl: String?,
) {
    val hasNext: Boolean get() = !nextUrl.isNullOrBlank()
}

data class CoomerDetail(
    val slug: String,
    val title: String,
    val pageUrl: String,
    val coverUrl: String,
    val photos: List<String>,
    val videos: List<String>,
    val creator: CoomerTerm?,
    val related: List<CoomerTerm>,
    val caption: String = "",
    val postedAt: String = "",
) {
    val hasVideo: Boolean get() = videos.isNotEmpty()
    val hasPhotos: Boolean get() = photos.isNotEmpty()
}
