package com.yenaly.han1meviewer.XHamster.data.remote

import com.yenaly.han1meviewer.XHamster.common.XHamsterConstants
import com.yenaly.han1meviewer.XHamster.common.XHamsterSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

object XHamsterPrefsRepo {

    data class Option(val value: String, val label: String)

    data class Prefs(
        val orientation: String,
        val geo: String,
        val lang: Option,
        val languages: List<Option>,
        val historyActive: Boolean?,
        val titlesTranslation: Boolean?,
    )

    val ORIENTATIONS = listOf(
        Option("straight", "Straight"),
        Option("gay", "Gay"),
        Option("shemale", "Trans"),
    )

    private val JSON_TYPE = "application/json;charset=UTF-8".toMediaType()

    private fun base() = XHamsterConstants.BASE_URL

    private fun builder(url: String, referer: String = base() + "/"): Request.Builder =
        Request.Builder().url(url)
            .header("Accept", "*/*")
            .header("Origin", XHamsterConstants.ORIGIN)
            .header("Referer", referer)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("x-csrf-token", "1")

    private fun execute(request: Request): String =
        XHamsterNetwork.sharedOkHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.string()
        }

    private fun decode(raw: String): String =
        runCatching { Json.parseToJsonElement("\"$raw\"").jsonPrimitive.content }.getOrDefault(raw)

    suspend fun load(): Result<Prefs> = withContext(Dispatchers.IO) {
        runCatching {
            val html = execute(
                Request.Builder().url(base() + "/").build()
            ).replace("\\/", "/")

            fun bool(key: String): Boolean? =
                Regex("\"$key\":(true|false)").find(html)?.groupValues?.get(1)?.toBooleanStrictOrNull()

            val orientation = Regex("\"preferredOrientation\":\"(\\w+)\"").find(html)
                ?.groupValues?.get(1) ?: XHamsterSession.orientation
            val geo = Regex("\"preferredGeo\":\"(\\w+)\"").find(html)?.groupValues?.get(1)
                ?: XHamsterSession.geo
            val langMatch = Regex(
                "\"preferredLang\":\\{\"value\":\"([^\"]+)\",\"label\":\"((?:[^\"\\\\]|\\\\.)*)\""
            ).find(html)
            val langValue = langMatch?.groupValues?.get(1)
                ?: Regex("\"preferredLang\":\"(\\w+)\"").find(html)?.groupValues?.get(1)
                ?: XHamsterSession.lang

            val languages = mutableListOf<Option>()
            val listStart = html.indexOf("\"langList\"")
            if (listStart >= 0) {
                val window = html.substring(listStart, minOf(html.length, listStart + 20000))
                Regex("\\{\"value\":\"([^\"]+)\",\"label\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(window)
                    .forEach { m ->
                        val v = m.groupValues[1]
                        if (languages.none { it.value == v }) languages += Option(v, decode(m.groupValues[2]))
                    }
            }
            val langLabel = langMatch?.groupValues?.get(2)?.let(::decode)
                ?: languages.firstOrNull { it.value == langValue }?.label
                ?: langValue

            Regex("\"defaultCollectionId\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)
                ?.let { XHamsterSession.defaultCollectionId = it }

            XHamsterSession.orientation = orientation
            XHamsterSession.geo = geo
            XHamsterSession.lang = langValue
            XHamsterSession.langLabel = langLabel

            Prefs(
                orientation = orientation,
                geo = geo,
                lang = Option(langValue, langLabel),
                languages = languages,
                historyActive = bool("isWatchHistoryActive"),
                titlesTranslation = bool("videoTitlesTranslation"),
            )
        }
    }

    suspend fun countries(lang: String): Result<List<Option>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${base()}/api/front/country/list-with-subdivisions?preferredLang=$lang"
            val body = execute(builder(url).get().build())
            val root = Json.parseToJsonElement(body)
            val array = when (root) {
                is JsonArray -> root
                is JsonObject -> root.values.firstOrNull { it is JsonArray } as? JsonArray
                else -> null
            } ?: throw IOException("Unexpected country list")
            array.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val code = (o["code"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                val name = (o["name"] as? JsonPrimitive)?.contentOrNull ?: code
                if ('_' in code) null else Option(code.lowercase(), name)
            }.sortedBy { it.label }
        }
    }

    suspend fun setOrientation(value: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = when (value) {
                "gay" -> base() + "/gay"
                "shemale" -> base() + "/shemale"
                else -> base() + "/?straight="
            }
            execute(Request.Builder().url(url).build())
            XHamsterSession.orientation = value
        }
    }

    suspend fun setCountry(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            execute(Request.Builder().url("${base()}/?geo=$code").build())
            XHamsterSession.geo = code
        }
    }

    suspend fun setLanguage(option: Option): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildJsonObject { put("locale", option.value) }.toString()
            execute(
                builder("${base()}/api/front/settings/preferred-locale")
                    .post(payload.toRequestBody(JSON_TYPE)).build()
            )
            XHamsterSession.lang = option.value
            XHamsterSession.langLabel = option.label
        }
    }

    suspend fun setWatchHistoryActive(active: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            execute(
                builder("${base()}/api/front/watch-history/watch-history-pause-toggle?active=$active")
                    .post("{}".toRequestBody(JSON_TYPE)).build()
            )
            Unit
        }
    }

    suspend fun setTitlesTranslation(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (XHamsterAuth.isLoggedIn) {
                val payload = buildJsonObject { put("value", enabled) }.toString()
                execute(
                    builder("${base()}/api/front/user-settings/video-titles-translation")
                        .put(payload.toRequestBody(JSON_TYPE)).build()
                )
            } else {
                XHamsterCookieJar.putCookie(
                    base().toHttpUrl().host,
                    "video_titles_translation",
                    if (enabled) "1" else "0",
                )
            }
            Unit
        }
    }
}
