package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model

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

data class LeakGalleryTerm(
    val slug: String,
    val name: String,
    val thumbUrl: String = "",

    val customPath: String? = null,
    /** Numeric profile id (needed to follow / unfollow); 0 when the API did not give one. */
    val id: Long = 0L,
    val subscribed: Boolean = false,
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
    val liked: Boolean = false,
) {
    val hasVideo: Boolean get() = isVideo && mediaUrl.isNotBlank()
    val photos: List<String> get() = if (!isVideo && mediaUrl.isNotBlank()) listOf(mediaUrl) else emptyList()
    val hasPhotos: Boolean get() = photos.isNotEmpty()
}
