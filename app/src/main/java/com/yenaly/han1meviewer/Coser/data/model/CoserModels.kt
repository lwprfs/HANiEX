package com.yenaly.han1meviewer.Coser.data.model

data class CoserCard(
    val slug: String,
    val title: String,
    val coverUrl: String,
    val seriesName: String = "",
    val quality: String = "",
    val duration: String = "",
    val extraItems: Int = 0,
    val views: String = "",
    val date: String = "",
)

enum class CoserTermKind { SERIES, TAG, SEARCH }

data class CoserTerm(
    val kind: CoserTermKind,
    val slug: String,
    val name: String,
    val thumbUrl: String = "",
    val count: Int = 0,
) {
    val path: String
        get() = when (kind) {
            CoserTermKind.SERIES -> "/series/$slug/"
            CoserTermKind.TAG -> "/tag/$slug/"
            CoserTermKind.SEARCH -> "/search/?q=" + java.net.URLEncoder.encode(slug, "UTF-8")
        }
}

data class CoserListPage(
    val cards: List<CoserCard>,
    val terms: List<CoserTerm>,
    val nextUrl: String?,
) {
    val hasNext: Boolean get() = !nextUrl.isNullOrBlank()
}

data class CoserMirror(
    val provider: String,
    val embed: String,
    val direct: String = "",
)

data class CoserVideoRef(
    val url: String,
    val isEmbed: Boolean,
    val label: String = "",
    val mirrors: List<CoserMirror> = emptyList(),
    val poster: String = "",
    val durationSec: Int = 0,
)

data class CoserDetail(
    val slug: String,
    val title: String,
    val pageUrl: String,
    val coverUrl: String,
    val photos: List<String>,
    val videos: List<CoserVideoRef>,
    val categories: List<CoserTerm>,
    val tags: List<CoserTerm>,
    val related: List<CoserCard>,
    val postedAt: String = "",
    val views: String = "",
    val duration: String = "",
    val resolution: String = "",
) {
    val hasVideo: Boolean get() = videos.isNotEmpty()
    val hasPhotos: Boolean get() = photos.isNotEmpty()
}

data class CoserStream(
    val url: String,
    val label: String,
    val referer: String,
    val origin: String,
    val height: Int = 0,
)
