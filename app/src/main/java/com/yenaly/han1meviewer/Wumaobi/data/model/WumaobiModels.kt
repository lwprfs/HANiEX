package com.yenaly.han1meviewer.Wumaobi.data.model

import com.yenaly.han1meviewer.logic.model.HanimeInfo

data class WumaobiListPage(
    val items: List<HanimeInfo>,
    val hasNext: Boolean,
)

data class WumaobiEntity(
    val name: String,
    val path: String,
    val imageUrl: String,
    val subtitle: String,
)

data class WumaobiEntityPage(
    val items: List<WumaobiEntity>,
    val hasNext: Boolean,
)

data class WumaobiLink(val title: String, val url: String) {

    val path: String
        get() = runCatching { java.net.URI(url).rawPath }.getOrNull().orEmpty().ifBlank { url }
}

data class WumaobiSource(
    val label: String,
    val url: String,
    val height: Int,
    val hq: Boolean = false,
)

data class WumaobiFavCollection(
    val id: String,
    val name: String,
    val checked: Boolean,
)

data class WumaobiFavFolder(
    val name: String,
    val count: String,
)

data class WumaobiDetail(
    val code: String,
    val title: String,
    val coverUrl: String,
    val pageUrl: String,
    val date: String,
    val durationSeconds: Long,

    val channel: WumaobiLink?,
    val network: WumaobiLink?,

    val models: List<WumaobiLink>,
    val categories: List<WumaobiLink>,
    val sources: List<WumaobiSource>,
    val related: List<HanimeInfo>,
    val loggedIn: Boolean = false,
    val favCollections: List<WumaobiFavCollection> = emptyList(),
)
