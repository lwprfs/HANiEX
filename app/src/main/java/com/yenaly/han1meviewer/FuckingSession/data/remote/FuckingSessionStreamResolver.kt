package com.yenaly.han1meviewer.FuckingSession.data.remote

import com.yenaly.han1meviewer.util.logHost

import android.util.Log
import com.yenaly.han1meviewer.FuckingSession.data.model.FuckingSessionSource
import okhttp3.Request
import org.jsoup.Jsoup

object FuckingSessionStreamResolver {

    private const val TAG = "FuckingSessionResolver"

    private val MEDIA_REGEX = Regex(
        """https?://[^\s"'<>\\]+?\.(?:mp4|m3u8)(?:\?[^\s"'<>\\]*)?""",
        RegexOption.IGNORE_CASE,
    )
    private val HEIGHT_REGEX = Regex("""(\d{3,4})\s*p""", RegexOption.IGNORE_CASE)

    private val IGNORED_HOSTS = listOf(
        "bkcdn.net", "magsrv.com", "realsrv.com", "pemsrv.com", "exoclick", "juicyads",
        "trafficjunky", "doubleclick",
    )

    private const val MAX_EMBEDS = 3

    fun resolve(embeds: List<String>, pageUrl: String): List<FuckingSessionSource> {
        for (embed in embeds.take(MAX_EMBEDS)) {
            val sources = runCatching { resolveOne(embed, pageUrl) }
                .onFailure { Log.w(TAG, "resolve failed for ${embed.logHost()}", it) }
                .getOrDefault(emptyList())
            if (sources.isNotEmpty()) return sources
        }
        return emptyList()
    }

    private fun originOf(url: String): String =
        runCatching {
            val u = java.net.URI(url)
            "${u.scheme}://${u.host}/"
        }.getOrDefault(url)

    private fun resolveOne(embedUrl: String, pageUrl: String): List<FuckingSessionSource> {
        val request = Request.Builder()
            .url(embedUrl)
            .header("Referer", pageUrl)
            .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
            .build()
        val html = FuckingSessionNetwork.sharedOkHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "HTTP ${resp.code} for ${embedUrl.logHost()}")
                return emptyList()
            }
            resp.body?.string().orEmpty()
        }
        if (html.isBlank()) return emptyList()

        val normalized = html.replace("\\/", "/").replace("&amp;", "&")
        val doc = Jsoup.parse(normalized, embedUrl)

        val urls = LinkedHashSet<String>()
        doc.select("video source[src], video[src]").forEach { el ->
            el.absUrl("src").takeIf { it.startsWith("http") }?.let(urls::add)
        }
        MEDIA_REGEX.findAll(normalized).forEach { urls.add(it.value) }

        val referer = originOf(embedUrl)
        val usable = urls.filter { url ->
            val host = runCatching { java.net.URI(url).host }.getOrNull().orEmpty().lowercase()
            IGNORED_HOSTS.none { host.contains(it) }
        }

        return usable.mapIndexed { index, url ->
            val height = HEIGHT_REGEX.find(url.substringBefore('?').substringAfterLast('/'))
                ?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val label = when {
                height > 0 -> "${height}p"
                usable.size == 1 -> "Default"
                else -> "Source ${index + 1}"
            }
            FuckingSessionSource(label = label, url = url, height = height, referer = referer)
        }.distinctBy { it.label }.sortedByDescending { it.height }
    }
}
