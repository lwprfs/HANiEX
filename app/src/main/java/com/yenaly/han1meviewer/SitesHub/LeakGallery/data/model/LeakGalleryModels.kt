package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model

/** A media tile (video or photo). [slug] is the numeric media id. */
data class LeakGalleryCard(
    val slug: String,
    val title: String,
    val coverUrl: String,
    val username: String = "",
    val isVideo: Boolean = true,
    val durationSec: Int = 0,
) {
    val creatorName: String get() = username
}

/** A creator profile. [slug] is the username. */
data class LeakGalleryTerm(
    val slug: String,
    val name: String,
    val thumbUrl: String = "",
    /** Overrides the creator profile path (used for tag chips). */
    val customPath: String? = null,
) {
    val path: String
        get() = customPath
            ?: com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants.profilePath(slug)
}

data class LeakGalleryListPage(
    val cards: List<LeakGalleryCard>,
    val terms: List<LeakGalleryTerm>,
) {
    val hasNext: Boolean get() = cards.isNotEmpty() || terms.isNotEmpty()
}

data class LeakGalleryDetail(
    val slug: String,
    val title: String,
    val pageUrl: String,
    val coverUrl: String,
    val mediaUrl: String,
    val isVideo: Boolean,
    val durationSec: Int,
    val creator: LeakGalleryTerm?,
    val tags: List<String>,
    val similar: List<LeakGalleryCard>,
    val similarCreators: List<LeakGalleryTerm>,
    val caption: String = "",
    val postedAt: String = "",
    val views: Long = 0,
    val likes: Long = 0,
) {
    val hasVideo: Boolean get() = isVideo && mediaUrl.isNotBlank()
    val photos: List<String> get() = if (!isVideo && mediaUrl.isNotBlank()) listOf(mediaUrl) else emptyList()
    val hasPhotos: Boolean get() = photos.isNotEmpty()
}
