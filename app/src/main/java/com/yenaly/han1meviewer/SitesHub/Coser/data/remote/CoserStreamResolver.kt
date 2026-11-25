package com.yenaly.han1meviewer.SitesHub.Coser.data.remote

import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserConstants
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserMirror
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserStream
import com.yenaly.han1meviewer.SitesHub.Coser.data.model.CoserVideoRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.IOException

object CoserStreamResolver {

    private val M3U8_REGEX = Regex("""https?:\\?/\\?/[^"'\s<>\\]+(?:\\/[^"'\s<>\\]+)*\.m3u8[^"'\s<>]*""")
    private val MP4_REGEX = Regex("""(?:https?:)?\\?/\\?/[^"'\s<>\\]+(?:\\/[^"'\s<>\\]+)*\.mp4[^"'\s<>]*""")
    private val FILE_REGEX = Regex("""(?:file|src|source)\s*[:=]\s*["']([^"']+\.(?:m3u8|mp4)[^"']*)["']""")
    private val RESOLUTION_REGEX = Regex("""RESOLUTION=(\d+)x(\d+)""")
    private val BANDWIDTH_REGEX = Regex("""BANDWIDTH=(\d+)""")

    private val ST_ASSIGN = Regex("""getElementById\(\s*['"](?:robotlink|ideoooolink|norobotlink)['"]\s*\)\s*\.innerHTML\s*=\s*([^;]+);""")
    private val ST_PART = Regex("""['"]([^'"]*)['"]\s*(?:\.\s*substring\(\s*(\d+)\s*\))?""")
    private val ST_COMBINED = Regex("""['"]([^'"]*)['"]\s*\+\s*\(\s*['"]([^'"]*)['"]\s*\)\s*\.substring\(\s*(\d+)\s*\)""")

    private val DOOD_PASS = Regex("""/pass_md5/[\w\-/]+""")
    private val DOOD_TOKEN = Regex("""[?&]token=([\w]+)""")

    private val PACKED = Regex("""\}\('(.*)',\s*(\d+),\s*(\d+),\s*'(.*?)'\.split\('\|'\)""", RegexOption.DOT_MATCHES_ALL)
    private val WURL = Regex("""(?:MDCore\.)?wurl\s*=\s*["']([^"']+)["']""")

    suspend fun resolve(
        ref: CoserVideoRef,
        pageUrl: String,
        preferredMirror: Int = 0,
    ): List<CoserStream> = withContext(Dispatchers.IO) {
        val mirrors = ref.mirrors.ifEmpty {
            listOf(CoserMirror("embed", if (ref.isEmbed) ref.url else "", if (ref.isEmbed) "" else ref.url))
        }
        val order = buildList {
            mirrors.getOrNull(preferredMirror)?.let { add(it) }
            mirrors.forEachIndexed { i, m -> if (i != preferredMirror) add(m) }
        }
        var lastError: Throwable? = null
        for (mirror in order) {
            val streams = runCatching { resolveMirror(mirror, pageUrl) }
                .onFailure { lastError = it }
                .getOrNull()
            if (!streams.isNullOrEmpty()) return@withContext streams
        }
        throw IOException("No playable stream", lastError)
    }

    private fun originOf(url: String): String =
        url.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}" }.orEmpty()

    private suspend fun resolveMirror(mirror: CoserMirror, pageUrl: String): List<CoserStream> {
        if (mirror.direct.isNotBlank()) {
            return expandPlaylist(CoserStream(mirror.direct, "auto", pageUrl, originOf(mirror.direct)), listOf(pageUrl))
        }
        val embed = mirror.embed
        if (embed.isBlank()) return emptyList()
        val origin = originOf(embed)
        val alternates = listOf(CoserConstants.STREAM_REFERER, pageUrl)

        val html = runCatching { get(embed, CoserConstants.STREAM_REFERER) }.getOrDefault("")
        var media: String? = if (html.isNotBlank()) findMedia(mirror.provider, html, embed) else null
        if (media == null) media = CoserWebSniffer.sniff(embed, CoserConstants.STREAM_REFERER)
        if (media == null) return emptyList()
        return expandPlaylist(CoserStream(media, "auto", embed, origin), alternates)
    }

    private fun get(url: String, referer: String): String {
        val request = Request.Builder().url(url).header("Referer", referer).get().build()
        CoserNetwork.sharedOkHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body.string()
        }
    }

    private fun unescape(s: String): String =
        s.replace("\\/", "/").replace("&amp;", "&").replace("\\u0026", "&")

    private fun cleanUrl(raw: String): String = unescape(raw).trimEnd('\\', ',', ';', ')')

    private fun absolute(url: String, base: String): String = when {
        url.startsWith("http") -> url
        url.startsWith("//") -> "https:$url"
        else -> runCatching { java.net.URI(base).resolve(url).toString() }.getOrDefault(url)
    }

    private fun findMedia(provider: String, html: String, embed: String): String? {
        when (provider.lowercase()) {
            "streamtape" -> streamtape(html)?.let { return it }
            "doodstream", "dood" -> doodstream(html, embed)?.let { return it }
            "mixdrop" -> mixdrop(html)?.let { return it }
        }
        return generic(html, embed)
    }

    private fun generic(html: String, embed: String): String? {
        M3U8_REGEX.find(html)?.let { return cleanUrl(it.value) }
        FILE_REGEX.find(html)?.let { return absolute(cleanUrl(it.groupValues[1]), embed) }
        MP4_REGEX.find(html)?.let { return absolute(cleanUrl(it.value), embed) }
        return null
    }

    private fun streamtape(html: String): String? {
        val assign = ST_ASSIGN.findAll(html).lastOrNull()?.groupValues?.get(1) ?: return null
        val url = ST_COMBINED.find(assign)?.let { m ->
            m.groupValues[1] + m.groupValues[2].substring(m.groupValues[3].toInt().coerceAtMost(m.groupValues[2].length))
        } ?: run {
            val sb = StringBuilder()
            for (p in ST_PART.findAll(assign)) {
                val text = p.groupValues[1]
                val skip = p.groupValues[2].toIntOrNull()
                sb.append(if (skip != null) text.substring(skip.coerceAtMost(text.length)) else text)
            }
            sb.toString()
        }
        if (!url.contains("get_video")) return null
        val full = absolute(url.trim(), "https://streamtape.com/")
        return if (full.contains("stream=")) full else "$full&stream=1"
    }

    private fun doodstream(html: String, embed: String): String? {
        val path = DOOD_PASS.find(html)?.value ?: return null
        val token = DOOD_TOKEN.find(html)?.groupValues?.get(1) ?: return null
        val origin = originOf(embed)
        val base = runCatching { get(origin + path, embed) }.getOrNull()?.trim() ?: return null
        if (!base.startsWith("http")) return null
        val rand = (1..10).joinToString("") {
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random().toString()
        }
        return "$base$rand?token=$token&expiry=${System.currentTimeMillis()}"
    }

    private fun mixdrop(html: String): String? {
        val m = PACKED.find(html) ?: return null
        val unpacked = runCatching {
            unpack(m.groupValues[1], m.groupValues[2].toInt(), m.groupValues[3].toInt(), m.groupValues[4].split('|'))
        }.getOrNull() ?: return null
        val url = WURL.find(unpacked)?.groupValues?.get(1) ?: return null
        return absolute(url, "https://mixdrop.ag/")
    }

    private fun unpack(payload: String, radix: Int, count: Int, dict: List<String>): String {
        fun encode(n: Int): String {
            val head = if (n < radix) "" else encode(n / radix)
            val c = n % radix
            return head + if (c > 35) (c + 29).toChar().toString() else c.toString(36)
        }
        val map = HashMap<String, String>()
        for (i in count - 1 downTo 0) {
            val key = encode(i)
            map[key] = dict.getOrNull(i)?.takeIf { it.isNotEmpty() } ?: key
        }
        return Regex("""\b\w+\b""").replace(payload.replace("\\'", "'")) { map[it.value] ?: it.value }
    }

    private fun expandPlaylist(auto: CoserStream, alternates: List<String> = emptyList()): List<CoserStream> {
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
        val variants = ArrayList<CoserStream>()
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
            variants += base.copy(url = absolute(uri, base.url), label = label, height = side)
        }
        if (variants.isEmpty()) return listOf(base)
        val sorted = variants.distinctBy { it.label }.sortedByDescending { it.height }
        return if (sorted.size > 1) listOf(base) + sorted else sorted
    }

    fun choose(streams: List<CoserStream>, preferred: String): CoserStream? {
        if (streams.isEmpty()) return null
        if (preferred == "auto") return streams.first()
        val want = preferred.removeSuffix("p").toIntOrNull() ?: return streams.first()
        val sized = streams.filter { it.height > 0 }
        return sized.filter { it.height <= want }.maxByOrNull { it.height }
            ?: sized.minByOrNull { it.height } ?: streams.first()
    }
}
