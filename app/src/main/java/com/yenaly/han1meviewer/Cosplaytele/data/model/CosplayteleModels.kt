package com.yenaly.han1meviewer.Cosplaytele.data.model

data class CosplayteleCard(
    val slug: String,
    val title: String,
    val coverUrl: String,

    val photoCount: Int = 0,
    val videoCount: Int = 0,
)

enum class CosplayteleTermKind { CATEGORY, TAG }

data class CosplayteleTerm(
    val kind: CosplayteleTermKind,
    val slug: String,
    val name: String,
    val thumbUrl: String = "",
    val count: Int = 0,
) {
    val path: String
        get() = if (kind == CosplayteleTermKind.CATEGORY) "/category/$slug/" else "/tag/$slug/"
}

data class CosplayteleListPage(
    val cards: List<CosplayteleCard>,
    val terms: List<CosplayteleTerm>,
    val nextUrl: String?,
) {
    val hasNext: Boolean get() = !nextUrl.isNullOrBlank()
}

data class CosplayteleVideoRef(
    val url: String,
    val isEmbed: Boolean,
    val label: String = "",
)

data class CosplayteleDetail(
    val slug: String,
    val title: String,
    val pageUrl: String,
    val coverUrl: String,
    val photos: List<String>,
    val videos: List<CosplayteleVideoRef>,
    val categories: List<CosplayteleTerm>,
    val tags: List<CosplayteleTerm>,
    val related: List<CosplayteleCard>,
    val postedAt: String = "",
) {
    val hasVideo: Boolean get() = videos.isNotEmpty()
    val hasPhotos: Boolean get() = photos.isNotEmpty()
}

data class CosplayteleStream(
    val url: String,
    val label: String,
    val referer: String,
    val origin: String,
    val height: Int = 0,
)
