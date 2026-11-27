package com.yenaly.han1meviewer.util.offline

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

enum class OfflineKind(val folder: String) {
    PHOTOS("Photos"),
    SHORTS("Shorts"),
}

data class OfflineItem(
    val key: String,
    val kind: OfflineKind,
    val site: String,
    val folder: String,
    val title: String,
    val channel: String?,
    val dirSegments: List<String>,

    val mediaUris: List<String>,
    val mediaNames: List<String>,
    val coverUri: String?,
    val sizeBytes: Long,
    val addedAt: Long,
    val sourceUrl: String?,
    val views: Long?,
    val likes: Long?,
    val durationSeconds: Int?,
    val tags: List<String>,
    val description: String?,

    val ownedUris: List<String>,
) {
    val count: Int get() = mediaUris.size

    /** Local video to grab a frame from when there is no thumbnail image. */
    fun videoFallbackUri(): String? =
        if (kind == OfflineKind.SHORTS) mediaUris.firstOrNull() else null
}

internal fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

internal fun JsonObject.num(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

internal fun JsonObject.strList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

internal fun JsonElement?.asObject(): JsonObject? = this as? JsonObject
