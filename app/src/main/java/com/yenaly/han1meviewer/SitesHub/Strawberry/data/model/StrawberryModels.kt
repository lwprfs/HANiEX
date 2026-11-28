package com.yenaly.han1meviewer.SitesHub.Strawberry.data.model

import com.yenaly.han1meviewer.logic.model.HanimeInfo

data class StrawberryHomePage(
    val videos: List<StrawberryVideoCard>,
    val totalVideos: Int?,
    val totalPictures: Int?,
    val totalMembers: Int?,
    val announcement: StrawberryAnnouncement?,
    val pageInfo: StrawberryPageInfo?,
    val currentUsername: String? = null,
)

data class StrawberryVideoCard(
    val videoId: String,
    val title: String,
    val thumbUrl: String,
    val previewUrl: String?,
    val duration: String?,
    val uploaderName: String?,
    val viewsRaw: String?,
    val views: Long?,
) {
    fun toHanimeInfo(): HanimeInfo = HanimeInfo(
        title = title,
        coverUrl = thumbUrl,
        videoCode = videoId,
        duration = duration,
        views = viewsRaw,
        uploadTime = null,
        currentArtist = uploaderName,
        itemType = HanimeInfo.NORMAL,
    )
}

data class StrawberryPageInfo(
    val current: Int,
    val total: Int,
    val nextUrl: String?,
    val prevUrl: String?,
    val lastUrl: String? = null,
)

data class StrawberryAnnouncement(
    val title: String,
    val url: String,
)

data class StrawberryAnnouncementEntry(
    val title: String,
    val author: String?,
    val body: String,
    val isPinned: Boolean,
)

data class StrawberryChatMessage(
    val id: String,
    val author: String?,
    val authorUrl: String?,
    val timestampLabel: String?,
    val timestampRaw: String?,
    val body: String,
)

data class StrawberryChatPage(
    val messages: List<StrawberryChatMessage>,
    val csrfToken: String,
    val latestId: String?,
    val onLastPage: Boolean,
    val formError: String?,
)

data class StrawberryTag(
    val name: String,
    val url: String,
    val count: Int?,
)

data class StrawberryPictureCard(
    val pictureId: String,
    val title: String,
    val thumbUrl: String,
    val uploaderName: String?,
    val viewsRaw: String?,
    val views: Long?,
)

data class StreamResult(
    val quality: String,
    val url: String,
    val mediaType: String?,
    val isHls: Boolean,
)

data class StrawberryVideoDetail(
    val videoId: String,
    val title: String,
    val uploaderName: String?,
    val uploaderUrl: String?,
    val views: Long?,
    val viewsRaw: String?,
    val duration: String?,
    val fileSize: String?,
    val favoriteCount: Int?,
    val isFavorited: Boolean,
    val tags: List<StrawberryVideoTag>,
    val qualityLinks: List<StrawberryQualityLink>,
    val qualityLinksByQuality: Map<String, String>,
    val streamUrlByQuality: Map<String, String>,
    val currentQuality: String,
    val currentMediaType: String?,
    val recommendedVideos: List<StrawberryVideoCard>,

    val coverUrl: String = "",
)

data class StrawberryVideoTag(
    val tagId: String,
    val name: String,
    val score: Int,
)

data class StrawberryQualityLink(
    val quality: String,
    val label: String,
    val url: String,
    val sizeLabel: String?,
    val isActive: Boolean,
)

data class StrawberrySearchResult(
    val query: String?,
    val field: String,
    val pageInfo: StrawberryPageInfo?,
    val cards: List<StrawberryVideoCard>,
    val headingText: String?,
)

data class StrawberryUserProfile(
    val username: String,
    val displayName: String,
    val bio: String?,
    val avatarUrl: String?,
    val followerCount: Int?,
    val followingCount: Int?,
    val videoCount: Int?,
    val pictureCount: Int?,
    val isFollowing: Boolean,
    val isCurrentUser: Boolean = false,
    val videos: List<StrawberryVideoCard>,
    val pictures: List<StrawberryPictureCard>,
    val pageInfo: StrawberryPageInfo?,
)
