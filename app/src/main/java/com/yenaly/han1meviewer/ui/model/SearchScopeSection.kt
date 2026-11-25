package com.yenaly.han1meviewer.ui.model

import com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model.SearchOption

data class SearchScopeSection(
    val titleRes: Int,
    val options: List<SearchOption>,
    val spanCount: Int = 3,
)
