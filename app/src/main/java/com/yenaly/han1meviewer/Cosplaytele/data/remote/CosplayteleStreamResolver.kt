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
                    .let { expandPlaylist(it.first(), listOf(pageUrl)) }
            }
            val embed = ref.url
            val origin = embed.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}" }
                ?: CosplayteleConstants.STREAM_ORIGIN
            val alternates = listOf(CosplayteleConstants.STREAM_REFERER, pageUrl)

            val html = runCatching { get(embed, pageUrl) }.getOrDefault("")
            var playlist = if (html.isNotBlank()) findPlaylist(html, embed) else null
            if (playlist == null) playlist = guessApiPlaylist(embed, pageUrl, alternates)
            if (playlist == null) playlist = CosplayteleWebSniffer.sniff(embed, pageUrl)
            if (playlist == null) throw IOException("No playable stream in embed page")
            expandPlaylist(CosplayteleStream(playlist, "auto", embed, origin), alternates)
        }

    private fun guessApiPlaylist(embed: String, pageUrl: String, alternates: List<String>): String? {
        val id = ID_REGEX.find(embed)?.groupValues?.get(1) ?: return null
        val http = embed.toHttpUrlOrNull() ?: return null
        val url = "${http.scheme}://${http.host}/api-embed/$id/index.m3u8"
        for (ref in listOf(embed, pageUrl) + alternates) {
            val text = runCatching { get(url, ref) }.getOrNull() ?: continue
            if (text.contains("#EXTM3U")) return url
        }
        return null
    }

    private val BASE64_REGEX = Regex("""[A-Za-z0-9+/]{40,}={0,2}""")

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

    private fun cleanUrl(raw: String): String =
        unescape(raw).trimEnd('\\', ',', ';', ')')

    private fun findPlaylist(html: String, embedUrl: String): String? {
        M3U8_REGEX.find(html)?.let { return cleanUrl(it.value) }

        val decoded = runCatching { java.net.URLDecoder.decode(html.replace("\\u002F", "/"), "UTF-8") }.getOrDefault(html)
        if (decoded !== html) M3U8_REGEX.find(decoded)?.let { return cleanUrl(it.value) }

        for (m in BASE64_REGEX.findAll(html)) {
            val text = runCatching { String(android.util.Base64.decode(m.value, android.util.Base64.DEFAULT)) }
                .getOrNull() ?: continue
            if (!text.contains(".m3u8")) continue
            M3U8_REGEX.find(text)?.let { return cleanUrl(it.value) }
        }
        FILE_REGEX.findAll(html).map { unescape(it.groupValues[1]) }
            .firstOrNull { it.contains(".m3u8") }?.let { return absolute(it, embedUrl) }

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

    private fun withMasterQuery(variant: String, master: String): String {
        if (variant.contains("token=") || !master.contains('?')) return variant
        if (variant.contains('?')) return variant
        return variant + "?" + master.substringAfter('?')
    }

    private fun expandPlaylist(auto: CosplayteleStream, alternates: List<String> = emptyList()): List<CosplayteleStream> {
        if (!auto.url.contains(".m3u8")) return listOf(auto)
        var base = auto
        var text: String? = null
        for (ref in listOf(auto.referer) + alternates) {
            text = runCatching { get(auto.url, ref) }.getOrNull()
            if (text != null) {
                base = auto.copy(referer = ref)
                break
            }
        }
        if (text == null) return listOf(auto)
        if (!text.contains("#EXT-X-STREAM-INF")) return listOf(base)
        val lines = text.lines().map { it.trim() }
        val variants = ArrayList<CosplayteleStream>()
        for (i in lines.indices) {
            val line = lines[i]
            if (!line.startsWith("#EXT-X-STREAM-INF")) continue
            val uri = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: continue
            val res = RESOLUTION_REGEX.find(line)
            val w = res?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val h = res?.groupValues?.get(2)?.toIntOrNull() ?: 0

            val side = if (w > 0 && h > 0) minOf(w, h) else 0
            val label = if (side > 0) "${side}p" else {
                val bw = BANDWIDTH_REGEX.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                if (bw > 0) "${bw / 1000}k" else "video"
            }
            variants += base.copy(url = withMasterQuery(absolute(uri, base.url), base.url), label = label, height = side)
        }
        if (variants.isEmpty()) return listOf(base)
        val sorted = variants.distinctBy { it.label }.sortedByDescending { it.height }
        return if (sorted.size > 1) listOf(base) + sorted else sorted
    }

    fun choose(streams: List<CosplayteleStream>, preferred: String): CosplayteleStream? {
        if (streams.isEmpty()) return null
        if (preferred == "auto") return streams.first()
        val want = preferred.removeSuffix("p").toIntOrNull() ?: return streams.first()
        val sized = streams.filter { it.height > 0 }
        return sized.filter { it.height <= want }.maxByOrNull { it.height }
            ?: sized.minByOrNull { it.height } ?: streams.first()
    }
}
