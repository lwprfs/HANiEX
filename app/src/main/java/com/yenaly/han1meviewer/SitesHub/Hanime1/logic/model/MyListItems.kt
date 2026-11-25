package com.yenaly.han1meviewer.SitesHub.Hanime1.logic.model

data class MyListItems<I>(
    val hanimeInfo: List<I>,

    var desc: String? = null,
    val csrfToken: String? = null,
    val maxPage: Int = 1,
)
