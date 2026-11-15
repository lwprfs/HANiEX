package com.yenaly.han1meviewer.Cosplaytele.data.remote

import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleStream
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleVideoRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.IOException

/**
 * Posts embed their video as `https://cossora.stream/embed/<uuid>` (JW Player). Playing it needs:
 *  1. the embed page, fetched with the post as Referer, which carries a signed
 *     `api-embed/<uuid>/index.m3u8?token=...` url;
 *  2. that master playlist (variants `master_1920p.m3u8`, ...), fetched with the embed page as Referer;
 *  3. segments from `hls-cdn.cossora.stream`, which want `Referer/Origin: https://cossora.stream`.
 */
object CosplayteleStreamResolver {

    private val M3U8_REGEX = Regex("""https?:\\?/\\?/[^"'\s<>\\]+(?:\\/[^"'\s<>\\]+)*\.m3u8[^"'\s<>]*""")
    private val FILE_REGEX = Regex("""file\s*[:=]\s*["']([^"']+)["']""")
    private val TOKEN_REGEX = Regex("""eyJ[\w-]+\.eyJ[\w-=]+\.[\w-]+""")
    private val ID_REGEX = Regex("""/embed/([0-9a-fA-F-]{8,})""")
    private val RESOLUTION_REGEX = Regex("""RESOLUTION=(\d+)x(\d+)""")
    private val BANDWIDTH_REGEX = Regex("""BANDWIDTH=(\d+)""")

    suspend fun resolve(ref: CosplayteleVideoRef, pageUrl: String): List<CosplayteleStream> =
        withContext(Dispatchers.IO) {
            if (!ref.isEmbed) {
                return@withContext listOf(direct(ref.url, pageUrl))
                    .let { expandPlaylist(it.first()) }
            }
            val embed = ref.url
            val html = get(embed, pageUrl)
            val playlist = findPlaylist(html, embed)
                ?: throw IOException("No playable stream in embed page")
            val origin = embed.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}" }
                ?: CosplayteleConstants.STREAM_ORIGIN
            expandPlaylist(CosplayteleStream(playlist, "auto", embed, origin))
        }

    private fun direct(url: String, referer: String): CosplayteleStream {
        val origin = url.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}" }.orEmpty()
        return CosplayteleStream(url, "auto", referer, origin)
    }

    private fun get(url: String, referer: String): String {
        val request = Request.Builder().url(url).header("Referer", referer).get().build()
        CosplayteleNetwork.sharedOkHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body.string()
        }
    }

    private fun unescape(s: String): String =
        s.replace("\\/", "/").replace("&amp;", "&").replace("\\u0026", "&")

    private fun findPlaylist(html: String, embedUrl: String): String? {
        M3U8_REGEX.find(html)?.let { return unescape(it.value) }
        FILE_REGEX.findAll(html).map { unescape(it.groupValues[1]) }
            .firstOrNull { it.contains(".m3u8") }?.let { return absolute(it, embedUrl) }
        // Token present but url assembled by script: rebuild it the way the player does.
        val id = ID_REGEX.find(embedUrl)?.groupValues?.get(1)
        val token = TOKEN_REGEX.find(html)?.value
        val http: HttpUrl? = embedUrl.toHttpUrlOrNull()
        if (id != null && token != null && http != null) {
            return "${http.scheme}://${http.host}/api-embed/$id/index.m3u8?token=$token"
        }
        return null
    }

    private fun absolute(url: String, base: String): String =
        if (url.startsWith("http")) url
        else runCatching { java.net.URI(base).resolve(url).toString() }.getOrDefault(url)

    /** Splits a master playlist into one entry per variant; media playlists stay as they are. */
    private fun expandPlaylist(auto: CosplayteleStream): List<CosplayteleStream> {
        if (!auto.url.contains(".m3u8")) return listOf(auto)
        val text = runCatching { get(auto.url, auto.referer) }.getOrNull() ?: return listOf(auto)
        if (!text.contains("#EXT-X-STREAM-INF")) return listOf(auto)
        val lines = text.lines().map { it.trim() }
        val variants = ArrayList<CosplayteleStream>()
        for (i in lines.indices) {
            val line = lines[i]
            if (!line.startsWith("#EXT-X-STREAM-INF")) continue
            val uri = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: continue
            val res = RESOLUTION_REGEX.find(line)
            val w = res?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val h = res?.groupValues?.get(2)?.toIntOrNull() ?: 0
            // Portrait clips report 1080x1920: the quality label follows the short side.
            val side = if (w > 0 && h > 0) minOf(w, h) else 0
            val label = if (side > 0) "${side}p" else {
                val bw = BANDWIDTH_REGEX.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                if (bw > 0) "${bw / 1000}k" else "video"
            }
            variants += auto.copy(url = absolute(uri, auto.url), label = label, height = side)
        }
        if (variants.isEmpty()) return listOf(auto)
        val sorted = variants.distinctBy { it.label }.sortedByDescending { it.height }
        return if (sorted.size > 1) listOf(auto) + sorted else sorted
    }

    /** Picks the stream for the preferred quality ("auto", "1080p" ...). */
    fun choose(streams: List<CosplayteleStream>, preferred: String): CosplayteleStream? {
        if (streams.isEmpty()) return null
        if (preferred == "auto") return streams.first()
        val want = preferred.removeSuffix("p").toIntOrNull() ?: return streams.first()
        val sized = streams.filter { it.height > 0 }
        return sized.filter { it.height <= want }.maxByOrNull { it.height }
            ?: sized.minByOrNull { it.height } ?: streams.first()
    }
}
