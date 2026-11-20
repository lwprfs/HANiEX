package com.yenaly.han1meviewer.RedGifs.common

import com.yenaly.han1meviewer.Preferences

object RedGifsConstants {

    val BASE_URL: String
        get() = Preferences.redGifsBaseUrl.trimEnd('/')

    const val API_BASE = "https://api.redgifs.com"
    const val AUTH_BASE = "https://auth2.redgifs.com"

    const val CLIENT_ID = "e06c34dac7654821bcb37e0393b54350"

    val REFERER: String
        get() = "$BASE_URL/"

    val ORIGIN: String
        get() = BASE_URL

    const val PAGE_SIZE = 30

    fun gifUrl(id: String): String = "$BASE_URL/watch/$id"

    fun userUrl(name: String): String = "$BASE_URL/users/$name"

    fun nicheUrl(id: String): String = "$BASE_URL/niches/$id"

    val ORIENTATIONS = listOf("straight", "gay", "lesbian", "trans", "bisexual", "animated")
}
