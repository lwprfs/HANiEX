package com.yenaly.han1meviewer.SitesHub.Strawberry.common

import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork

object StrawberryUrlUtils {

    private val KNOWN_HOSTS = StrawberryConstants.STRAWBERRY_HOSTNAME

    fun isStrawberryHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val lower = host.lowercase()
        return KNOWN_HOSTS.any { lower == it || lower.endsWith(".$it") }
    }

    fun normalizeVideoUrl(input: String): String {
        if (input.isBlank()) return input
        val base = StrawberryNetwork.baseUrl.trimEnd('/')

        if (input.startsWith("http://") || input.startsWith("https://")) {
            val videoId = Regex("""/file(\d+)""").find(input)?.groupValues?.getOrNull(1)
            return if (videoId != null) "$base/file$videoId" else input
        }

        val trimmed = input.trim('/')
        return when {
            trimmed.startsWith("file") -> "$base/$trimmed"
            trimmed.all { it.isDigit() } -> "$base/file$trimmed"
            else -> "$base/$trimmed"
        }
    }

    fun normalizeUserUrl(input: String): String {
        if (input.isBlank()) return input
        val base = StrawberryNetwork.baseUrl.trimEnd('/')
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input
        }
        val trimmed = input.trim('/')
        return when {
            trimmed.startsWith("id") -> "$base/$trimmed"
            trimmed.all { it.isDigit() } -> "$base/id$trimmed"
            else -> "$base/id$trimmed"
        }
    }

    fun normalizeTagUrl(input: String): String {
        if (input.isBlank()) return input
        val base = StrawberryNetwork.baseUrl.trimEnd('/')
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input
        }
        val trimmed = input.trim('/')
        return when {
            trimmed.startsWith("tag/") -> "$base/$trimmed"
            else -> "$base/tag/$trimmed"
        }
    }

    fun extractVideoId(input: String?): String? {
        if (input.isNullOrBlank()) return null
        return Regex("""/file(\d+)""").find(input)?.groupValues?.getOrNull(1)
    }

    fun extractUsername(input: String?): String? {
        if (input.isNullOrBlank()) return null
        return Regex("""/id([^/?#]+)""").find(input)?.groupValues?.getOrNull(1)
    }

    fun extractTagName(input: String?): String? {
        if (input.isNullOrBlank()) return null
        return Regex("""/tag/([^/?#]+)""").find(input)?.groupValues?.getOrNull(1)
    }
}
