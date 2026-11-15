package com.yenaly.han1meviewer.RouVideo.data.parser

import com.yenaly.han1meviewer.RouVideo.common.RouVideoFormat
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoFollowRow
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoFollowsPage
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoListPage
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoUser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Parsers for the signed-in pages: the session, `/favorite/{id}`, `/history/{id}` and `/follows`.
 * They read the same server-rendered loader data as the public pages.
 */
object RouVideoAccountParser {

    private fun JsonObject.prim(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

    private fun JsonObject.str(key: String): String? = prim(key)?.contentOrNull

    private fun JsonObject.dbl(key: String): Double = prim(key)?.doubleOrNull ?: 0.0

    private fun JsonObject.lng(key: String): Long =
        prim(key)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: 0L

    private fun JsonObject.int(key: String): Int = lng(key).toInt()

    private fun JsonObject.bool(key: String): Boolean = prim(key)?.booleanOrNull ?: false

    private fun JsonObject.strList(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?: emptyList()

    private fun JsonObject.objList(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private val ID_IN_LINK = Regex("""/(?:history|favorite|profile)/([A-Za-z0-9]{15,40})""")

    /**
     * The signed-in user of any page. The root route's loader data is the session
     * (`{ user: { id, name, email } }`); a signed-out visitor has none.
     */
    fun parseSessionUser(html: String): RouVideoUser? {
        val fromLoader = RouVideoLiteral.allLoaderObjects(html)
            .asSequence()
            .mapNotNull { it.obj("user") }
            .firstOrNull { !it.str("id").isNullOrBlank() }
        if (fromLoader != null) {
            return RouVideoUser(
                id = fromLoader.str("id").orEmpty(),
                name = fromLoader.str("name").orEmpty(),
                email = fromLoader.str("email").orEmpty(),
            )
        }
        return null
    }

    /** Fallback: the user id can also be read from the library links in the page chrome. */
    fun parseUserIdFromLinks(html: String): String? =
        ID_IN_LINK.find(html)?.groupValues?.get(1)

    /** `/favorite/{id}` — saved videos, plus ongoing series the viewer saved single episodes of. */
    fun parseFavorites(html: String): RouVideoListPage {
        val root = RouVideoLiteral.loaderObject(html, "userFavorites", "pageNum")
        val videos = root.objList("userFavorites").mapNotNull { row ->
            val video = row.obj("video") ?: return@mapNotNull null
            RouVideoParser.videoCard(video)
        }
        val pageNum = root.int("pageNum").coerceAtLeast(1)
        val series = if (pageNum == 1) {
            root.objList("ongoing").mapNotNull { RouVideoParser.seriesCard(it) }
        } else emptyList()
        return RouVideoListPage(
            items = videos + series,
            pageNum = pageNum,
            totalPage = root.int("totalPage"),
            total = root.int("totalVideoNum"),
        )
    }

    /** `/history/{id}` — what the viewer watched recently, with where they stopped. */
    fun parseHistory(html: String): RouVideoListPage {
        val root = RouVideoLiteral.loaderObject(html, "userHistories", "pageNum")
        val items = root.objList("userHistories").mapNotNull { row ->
            val video = row.obj("video") ?: return@mapNotNull null
            val card = RouVideoParser.videoCard(video) ?: return@mapNotNull null
            val finished = row.bool("finished")
            val position = row.dbl("position")
            val note = when {
                finished -> null
                position > 0.0 -> RouVideoFormat.duration(position).takeIf { it.isNotBlank() }
                else -> null
            }
            card.copy(
                finished = finished,
                note = note?.let { "\u25B6 $it" },
                createdAt = row.str("viewedAt") ?: card.createdAt,
            )
        }
        return RouVideoListPage(
            items = items,
            pageNum = root.int("pageNum").coerceAtLeast(1),
            totalPage = root.int("totalPage"),
            total = root.int("totalVideoNum"),
        )
    }

    /** `/follows` — series the viewer follows, with unseen-episode counts. */
    fun parseFollows(html: String): RouVideoFollowsPage {
        val root = RouVideoLiteral.loaderObject(html, "rows", "anon")
        val rows = root.objList("rows").mapNotNull { row ->
            val series = row.obj("series") ?: return@mapNotNull null
            val id = series.str("id")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val offer = row.obj("offer")
            val playId = offer?.str("id")?.takeIf { it.isNotBlank() }
                ?: series.str("firstEpisodeId")?.takeIf { it.isNotBlank() }
                ?: series.str("coverVideoId")?.takeIf { it.isNotBlank() }
                ?: series.objList("episodes").firstOrNull()?.str("id")?.takeIf { it.isNotBlank() }
            RouVideoFollowRow(
                seriesId = id,
                title = series.str("name").orEmpty(),
                titleZh = series.str("nameZh"),
                coverUrl = series.str("coverImageUrl").orEmpty(),
                isPoster = series.bool("isPoster"),
                tags = series.strList("tags"),
                epLabel = series.str("epLabel"),
                episodeCount = series.int("episodeCount"),
                completed = series.str("status").equals("COMPLETED", ignoreCase = true),
                unseen = row.int("unseen"),
                lastSeenEpisode = row.prim("lastSeenEpisode")?.longOrNull?.toInt(),
                playVideoId = playId,
                playKind = offer?.str("kind"),
                playNumber = offer?.int("number") ?: 0,
            )
        }
        return RouVideoFollowsPage(
            rows = rows,
            pageNum = root.int("pageNum").coerceAtLeast(1),
            totalPage = root.int("totalPage"),
            total = root.int("total"),
            newCount = root.int("newCount"),
            signedIn = !root.bool("anon"),
        )
    }
}
