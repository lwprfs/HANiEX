package com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model

import com.yenaly.han1meviewer.logic.model.HanimeInfo

data class HanimeSearchResult(
    val list: List<HanimeInfo>,
    val totalPages: Int,
)
