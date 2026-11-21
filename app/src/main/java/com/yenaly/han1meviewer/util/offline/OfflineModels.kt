package com.yenaly.han1meviewer.util.offline

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** The two kinds handled by the offline downloader (videos keep their own Room-based downloader). */
enum class OfflineKind(val folder: String) {
    PHOTOS("Photos"),
    SHORTS("Shorts"),
}

/**
 * One entry of the "Downloaded" grid.
 * Photos: one entry per album folder. Shorts: one entry per clip.
 */
data class OfflineItem(
    val key: String,
    val kind: OfflineKind,
    val site: String,
    val folder: String,
    val title: String,
    val channel: String?,
    val dirSegments: List<String>,
    /** Photos: every image of the album. Shorts: the single clip. */
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
    /** Every file that belongs only to this entry (clip, thumbnail, json). Used when deleting a clip. */
    val ownedUris: List<String>,
) {
    val count: Int get() = mediaUris.size
}

internal fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

internal fun JsonObject.num(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

internal fun JsonObject.strList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

internal fun JsonElement?.asObject(): JsonObject? = this as? JsonObject
