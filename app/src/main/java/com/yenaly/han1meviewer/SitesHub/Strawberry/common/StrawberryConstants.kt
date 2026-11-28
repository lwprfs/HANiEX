package com.yenaly.han1meviewer.SitesHub.Strawberry.common

object StrawberryConstants {

    private val HOST_B64 = arrayOf("ZXJvc2V4eWd3cGY3enVmcGh3cW1nNTM2aDJwc2FoYmZtMnpscHlmdXhnMzZtbTN5dHphaml3aWQub25pb24=")
    private val URL_B64 = arrayOf("aHR0cDovL2Vyb3NleHlnd3BmN3p1ZnBod3FtZzUzNmgycHNhaGJmbTJ6bHB5ZnV4ZzM2bW0zeXR6YWppd2lkLm9uaW9uLw==")

    val STRAWBERRY_HOSTNAME: Array<String> by lazy {
        HOST_B64.map(StrawberryEndpointCodec::decodeHost).toTypedArray()
    }

    val STRAWBERRY_URL: Array<String> by lazy {
        URL_B64.map(StrawberryEndpointCodec::decodeUrl).toTypedArray()
    }

    const val DEFAULT_PAGE_SIZE = 20

    const val SEARCH_FIELD_TITLE = "title"
    const val SEARCH_FIELD_UPLOADER = "uploader"
    const val SEARCH_FIELD_TAG = "tag"

    const val LENGTH_ANY = "any"
    const val LENGTH_XSHORT = "xshort"
    const val LENGTH_SHORT = "short"
    const val LENGTH_MEDIUM = "medium"
    const val LENGTH_LONG = "long"
    const val LENGTH_XLONG = "xlong"

    const val DATE_ANY = "any"
    const val DATE_DAY = "day"
    const val DATE_WEEK = "week"
    const val DATE_MONTH = "month"
    const val DATE_YEAR = "year"

    const val SORT_NEWEST = "newest"
    const val SORT_OLDEST = "oldest"
    const val SORT_MOST_VIEWED = "most_viewed"
    const val SORT_LEAST_VIEWED = "least_viewed"
    const val SORT_LONGEST = "longest"
    const val SORT_SHORTEST = "shortest"

    fun getPrimaryUrl(): String = com.yenaly.han1meviewer.Preferences.strawberryBaseUrl

    fun getPrimaryHost(): String =
        runCatching {
            com.yenaly.han1meviewer.Preferences.strawberryBaseUrl
                .substringAfter("://")
                .substringBefore("/")
                .substringBefore(":")
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: StrawberrySitePreferences.currentHostname()
}
