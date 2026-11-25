package com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.yenaly.han1meviewer.logic.model.VideoItemType

@Parcelize
data class MySubscriptions(
    val subscriptions: List<SubscriptionItem>,
    val subscriptionsVideos: List<SubscriptionVideosItem>,
    val maxPage: Int
) : Parcelable

@Parcelize
data class SubscriptionItem(
    val artistName: String,
    val avatar: String
) : Parcelable

@Parcelize
data class SubscriptionVideosItem(
    override val title: String,
    override val coverUrl: String,
    override val videoCode: String,
    override val duration: String? = null,
    override val views: String? = null,
    override val reviews: String? = null,
    override val currentArtist: String? = null,
    override val uploadTime: String?= null,
) : Parcelable, VideoItemType
