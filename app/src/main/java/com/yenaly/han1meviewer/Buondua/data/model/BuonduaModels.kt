package com.yenaly.han1meviewer.Buondua.data.model

import com.yenaly.han1meviewer.logic.model.HanimeInfo

data class BuonduaListPage(
    val items: List<HanimeInfo>,

    val nextUrl: String?,

    val heading: String? = null,
)

data class BuonduaLink(val title: String, val url: String)

data class BuonduaPhoto(val url: String, val index: Int)

data class BuonduaVideo(

    val url: String,

    val title: String,
    val posterUrl: String?,
    val fileName: String,
)

data class BuonduaAlbumPage(
    val code: String,

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
