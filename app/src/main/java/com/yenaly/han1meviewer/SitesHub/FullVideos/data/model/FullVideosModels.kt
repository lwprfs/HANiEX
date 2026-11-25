package com.yenaly.han1meviewer.SitesHub.FullVideos.data.model

import com.yenaly.han1meviewer.logic.model.HanimeInfo

data class FullVideosListPage(
    val items: List<HanimeInfo>,
    val hasNext: Boolean,
)

data class FullVideosEntity(
    val name: String,

    val path: String,
    val imageUrl: String,
    val subtitle: String,
)

data class FullVideosEntityPage(
    val items: List<FullVideosEntity>,
    val hasNext: Boolean,
)

data class FullVideosLink(val title: String, val url: String) {

    val path: String
        get() = runCatching { java.net.URI(url).path }.getOrNull().orEmpty().ifBlank { url }
}

data class FullVideosSource(
    val label: String,
    val url: String,
    val height: Int,
)

data class FullVideosDetail(
    val code: String,
    val title: String,
    val coverUrl: String,
    val pageUrl: String,
    val date: String,
    val durationSeconds: Long,
    val channel: FullVideosLink?,
    val network: FullVideosLink?,
    val models: List<FullVideosLink>,
    val categories: List<FullVideosLink>,
    val sources: List<FullVideosSource>,
    val related: List<HanimeInfo>,
)
