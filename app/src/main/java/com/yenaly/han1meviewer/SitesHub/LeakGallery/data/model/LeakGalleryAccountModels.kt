package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model

data class LeakGalleryMe(
    val id: Long,
    val username: String,
    val email: String,
    val avatarUrl: String,
    val createdAt: String = "",
    val language: String = "",
    val ageVerified: Boolean = false,
    val manager: Boolean = false,
    val hasUploads: Boolean = false,
    val banned: Boolean = false,
)

data class LeakGalleryPremium(
    val active: Boolean,
    val until: String = "",
    val daysRemaining: Int? = null,
)

data class LeakGalleryShort(
    val id: String,
    val title: String,
    val videoUrl: String,
    val coverUrl: String,
    val username: String = "",
    val profileId: Long = 0L,
    val avatarUrl: String = "",
    val likes: Long = 0L,
    val liked: Boolean = false,
    val views: Long = 0L,
    val subscribed: Boolean = false,
    val durationSec: Int = 0,
)

data class LeakGalleryShortsPage(
    val shorts: List<LeakGalleryShort>,
) {
    val hasNext: Boolean get() = shorts.isNotEmpty()
}

data class LeakGalleryProfileInfo(
    val id: Long,
    val username: String,
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val subscribed: Boolean = false,
    val subscribers: Long? = null,
    val mediaCount: Long? = null,
)

data class LeakGalleryNotification(
    val id: String,
    val type: String,
    val text: String,
    val createdAt: String = "",
    val read: Boolean = true,
)

sealed interface LeakGalleryLoginResult {
    data class Success(val token: String) : LeakGalleryLoginResult

    data class TwoFactor(val tempToken: String) : LeakGalleryLoginResult

    data class Failure(val kind: Kind, val message: String? = null) : LeakGalleryLoginResult

    enum class Kind { INVALID, SOCIAL_ACCOUNT, CAPTCHA, UNAVAILABLE, BANNED, TOO_MANY, EXISTS, OTHER }
}
