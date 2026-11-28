package com.yenaly.han1meviewer.SitesHub.Strawberry.data.parser

import com.yenaly.han1meviewer.util.logId

import android.util.Log
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberryQuality
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberryThumbCache
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryQualityLink
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryVideoCard
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryVideoDetail
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryVideoTag
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StreamResult
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.parser.StrawberryHtmlUtils.cleanText
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.parser.StrawberryHtmlUtils.parseTrailingNumber
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.parser.StrawberryHtmlUtils.parseViews
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object StrawberryWatchParser {

    private const val TAG = "StrawberryWatchParser"
    private const val DEFAULT_QUALITY = "240"

    private val durationAttrRegex = Regex("""data-duration=["']([\d.]+)["']""")
    private val watchQueryQualityRegex = Regex("""[?&]quality=([^&]+)""")

    fun parseWatchPage(html: String, baseUrl: String): StrawberryVideoDetail? {
        val doc = Jsoup.parse(html, baseUrl)
        val fileBlock = doc.selectFirst(".file_block")
        val paidMarker = fileBlock?.selectFirst(
            ".file_locked, .file_premium, .file_paid, .paid, .premium, img[src*=locked], img[src*=diamond]"
        ) != null || fileBlock?.text()?.contains(Regex("\\b(paid|premium|members only|locked)\\b", RegexOption.IGNORE_CASE)) == true
        if (paidMarker) return null
        val titleElement = doc.selectFirst("h1.watch-title")
            ?: doc.selectFirst("h1.h1")
            ?: doc.selectFirst("h1")
        if (titleElement == null) {
            Log.w(TAG, "Strawberry file page missing title")
            return null
        }

        val title = cleanText(titleElement.text())
        val videoId = extractVideoId(doc, baseUrl)

        val infoCard = doc.selectFirst("div.watch-info-card")
        val uploaderAnchor = infoCard?.selectFirst("a.watch-uploader-name")
        val uploaderName = uploaderAnchor?.text()?.trim()?.takeIf { it.isNotBlank() }
        val uploaderUrl = uploaderAnchor?.absUrl("href")?.takeIf { it.isNotBlank() }

        val statsText = infoCard?.selectFirst("span.watch-stats")?.text()?.trim().orEmpty()
        val statsParts = statsText.split("·").map { it.trim() }
        val viewsRaw = statsParts.getOrNull(0)?.takeIf { it.isNotBlank() }
        val views = parseViews(viewsRaw)
        val duration = statsParts.getOrNull(1)?.takeIf { it.isNotBlank() }
        val fileSize = statsParts.getOrNull(2)?.takeIf { it.isNotBlank() }

        val favoriteButton = infoCard?.selectFirst("button#favorite-btn")
        val favoriteCount = favoriteButton
            ?.selectFirst("span#favorite-count")
            ?.text()?.let(::parseTrailingNumber)
        val isFavorited = favoriteButton?.attr("aria-pressed")?.equals("true", true) == true

        val tags = parseVideoTags(doc)
        val qualityLinks = parseQualityLinks(doc, baseUrl)
        val currentQuality = detectServedQuality(doc, baseUrl, qualityLinks)
        val qualityLinksByQuality = qualityLinks.associate { it.quality to it.url }
        val currentStream = parseCurrentStreamFromDoc(doc, baseUrl, currentQuality)
            ?: doc.selectFirst("a.file_load[href*=load_files_load.php]")?.absUrl("href")
                ?.takeIf { it.isNotBlank() }
                ?.let { StreamResult(currentQuality, it, mimeForFile(title), false) }
        val streamUrlByQuality = linkedMapOf<String, String>()
        if (currentStream != null) streamUrlByQuality[currentQuality] = currentStream.url
        collectAllStreams(doc, baseUrl).forEach { (q, url) ->
            streamUrlByQuality.putIfAbsent(q, url)
        }
        val currentMediaType = currentStream?.mediaType

        val recommended = parseRecommended(doc, baseUrl)
        val coverUrl = resolveCoverUrl(doc, baseUrl, videoId)

        Log.d(
            TAG,
            "parseWatchPage: id=${videoId.logId()} quality=$currentQuality " +
                    "streams=${streamUrlByQuality.keys} type=$currentMediaType " +
                    "qualityLinks=${qualityLinksByQuality.keys}",
        )

        return StrawberryVideoDetail(
            videoId = videoId,
            title = title,
            uploaderName = uploaderName,
            uploaderUrl = uploaderUrl,
            views = views,
            viewsRaw = viewsRaw,
            duration = duration,
            fileSize = fileSize,
            favoriteCount = favoriteCount,
            isFavorited = isFavorited,
            tags = tags,
            qualityLinks = qualityLinks,
            qualityLinksByQuality = qualityLinksByQuality,
            streamUrlByQuality = streamUrlByQuality,
            currentQuality = currentQuality,
            currentMediaType = currentMediaType,
            recommendedVideos = recommended,
            coverUrl = coverUrl,
        )
    }

    private fun resolveCoverUrl(doc: Document, baseUrl: String, videoId: String): String {
        val poster = listOf("video#watch-video", "video#watch-video-fallback", "video[poster]")
            .firstNotNullOfOrNull { sel ->
                doc.selectFirst(sel)?.attr("poster")?.takeIf { it.isNotBlank() }
            }
        resolveCover(poster, baseUrl)?.let { return it }

        StrawberryThumbCache.get(videoId)?.let { return it }

        val meta = listOf(
            "meta[property=og:image]" to "content",
            "meta[name=twitter:image]" to "content",
            "link[rel=image_src]" to "href",
        ).firstNotNullOfOrNull { (sel, attr) ->
            doc.selectFirst(sel)?.attr(attr)?.takeIf { it.isNotBlank() }
        }
        return resolveCover(meta, baseUrl).orEmpty()
    }

    private fun resolveCover(raw: String?, baseUrl: String): String? {
        val value = raw?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("data:") } ?: return null
        return resolveUrl(value, baseUrl)
    }

    fun parseStreamFromPage(
        html: String,
        baseUrl: String,
        quality: String,
    ): StreamResult? {
        val doc = Jsoup.parse(html, baseUrl)
        if (quality == StrawberryQuality.HLS) return parseHlsFromDoc(doc, baseUrl)
        return parseCurrentStreamFromDoc(doc, baseUrl, quality)
            ?: doc.selectFirst("a.file_load[href*=load_files_load.php]")?.absUrl("href")
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    val title = doc.selectFirst("h1")?.text().orEmpty()
                    StreamResult(quality, it, mimeForFile(title), false)
                }
    }

    private fun parseCurrentStreamFromDoc(
        doc: Document,
        baseUrl: String,
        quality: String,
    ): StreamResult? {
        val videoEl = doc.selectFirst("video#watch-video")
            ?: doc.selectFirst("video#watch-video-fallback")
            ?: doc.selectFirst("video")
            ?: return null

        val hlsAttr = videoEl.attr("data-hls-src").takeIf { it.isNotBlank() }

        val sources = videoEl.select("source")
        for (source in sources) {
            val raw = source.attr("src").takeIf { it.isNotBlank() }
                ?: source.attr("data-savepage-src").takeIf { it.isNotBlank() }
                ?: continue
            val abs = resolveUrl(raw, baseUrl) ?: continue
            val fromUrl = qualityOfStreamUrl(abs)
            if (fromUrl == quality || (quality == "original" && fromUrl == null)) {
                val mime = source.attr("type").takeIf { it.isNotBlank() }
                    ?: guessMime(abs)
                return StreamResult(
                    quality = quality,
                    url = abs,
                    mediaType = mime,
                    isHls = abs.contains(".m3u8"),
                )
            }
        }

        if (hlsAttr != null && quality == "original") {
            val abs = resolveUrl(hlsAttr, baseUrl) ?: return null
            return StreamResult(
                quality = quality,
                url = abs,
                mediaType = "application/vnd.apple.mpegurl",
                isHls = true,
            )
        }

        return null
    }

    private fun mainVideo(doc: Document): Element? =
        doc.selectFirst("video#watch-video")
            ?: doc.selectFirst("video#watch-video-fallback")
            ?: doc.selectFirst("video")

    fun parseHlsFromDoc(doc: Document, baseUrl: String): StreamResult? {
        val videoEl = mainVideo(doc) ?: return null
        val raw = videoEl.attr("data-hls-src").takeIf { it.isNotBlank() }
            ?: videoEl.select("source").firstNotNullOfOrNull { s ->
                (s.attr("src").takeIf { it.isNotBlank() }
                    ?: s.attr("data-savepage-src").takeIf { it.isNotBlank() })
                    ?.takeIf { it.contains(".m3u8", true) || it.contains("/hls/", true) }
            } ?: return null
        val abs = resolveUrl(raw, baseUrl) ?: return null
        return StreamResult(
            quality = StrawberryQuality.HLS,
            url = abs,
            mediaType = "application/vnd.apple.mpegurl",
            isHls = true,
        )
    }

    private fun collectAllStreams(doc: Document, baseUrl: String): Map<String, String> {
        val out = linkedMapOf<String, String>()
        val videoEl = mainVideo(doc) ?: return out
        for (source in videoEl.select("source")) {
            val raw = source.attr("src").takeIf { it.isNotBlank() }
                ?: source.attr("data-savepage-src").takeIf { it.isNotBlank() }
                ?: continue
            val abs = resolveUrl(raw, baseUrl) ?: continue
            if (abs.contains(".m3u8", true) || abs.contains("/hls/", true)) continue
            val q = qualityOfStreamUrl(abs) ?: StrawberryQuality.ORIGINAL
            out.putIfAbsent(q, abs)
        }
        parseHlsFromDoc(doc, baseUrl)?.let { out[StrawberryQuality.HLS] = it.url }
        return out
    }

    private fun detectServedQuality(
        doc: Document,
        baseUrl: String,
        links: List<StrawberryQualityLink>,
    ): String {
        links.firstOrNull { it.isActive }?.quality?.let { return it }

        val videoEl = doc.selectFirst("video#watch-video")
            ?: doc.selectFirst("video#watch-video-fallback")
            ?: doc.selectFirst("video")
        if (videoEl != null) {
            for (source in videoEl.select("source")) {
                val raw = source.attr("src").takeIf { it.isNotBlank() }
                    ?: source.attr("data-savepage-src").takeIf { it.isNotBlank() }
                    ?: continue
                qualityOfStreamUrl(raw)?.let { return it }
            }
            if (videoEl.attr("data-hls-src").isNotBlank()) return "original"
        }

        return extractQualityFromPageUrl(baseUrl) ?: DEFAULT_QUALITY
    }

    fun qualityOfStreamUrl(url: String): String? {
        val streamq = Regex("""/streamq/\d+/(\d+)""", RegexOption.IGNORE_CASE).find(url)
        if (streamq != null) return streamq.groupValues[1]
        if (url.contains("/hls/", ignoreCase = true) ||
            url.contains(".m3u8", ignoreCase = true) ||
            Regex("""/stream/[a-f0-9]{16,}""", RegexOption.IGNORE_CASE).containsMatchIn(url)
        ) {
            return "original"
        }
        return null
    }

    private fun extractQualityFromPageUrl(url: String): String? {
        return watchQueryQualityRegex.find(url)?.groupValues?.getOrNull(1)
    }

    private fun guessMime(url: String): String? = when {
        url.contains(".webm", ignoreCase = true) -> "video/webm"
        url.contains(".m3u8", ignoreCase = true) -> "application/vnd.apple.mpegurl"
        url.contains(".mp4", ignoreCase = true) -> "video/mp4"
        else -> null
    }

    private fun resolveUrl(raw: String, baseUrl: String): String? {
        if (raw.isBlank()) return null
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        if (raw.startsWith("//")) return "http:$raw"

        return runCatching { java.net.URI(baseUrl).resolve(raw).toString() }.getOrNull()
            ?: runCatching {
                val u = java.net.URI(baseUrl)
                if (raw.startsWith("/")) "${u.scheme}://${u.authority}$raw" else null
            }.getOrNull()
            ?: if (raw.startsWith("/")) baseUrl.trimEnd('/') + raw
            else baseUrl.trimEnd('/') + "/" + raw
    }

    private fun extractVideoId(doc: Document, baseUrl: String): String {
        val canonical = doc.selectFirst("link[rel=canonical]")?.attr("href")
            ?.takeIf { it.isNotBlank() }
        val fromCanonical = canonical?.let(StrawberryHtmlUtils::extractVideoId)
        if (fromCanonical != null) return fromCanonical

        val ogUrl = doc.selectFirst("meta[property=og:url]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
        val fromOg = ogUrl?.let(StrawberryHtmlUtils::extractVideoId)
        if (fromOg != null) return fromOg

        val fromBase = StrawberryHtmlUtils.extractVideoId(baseUrl)
        if (fromBase != null) return fromBase

        val segment = StrawberryHtmlUtils.extractPathSegment(baseUrl)
        if (segment != null && segment.all(Char::isDigit)) return segment

        return ""
    }

    private fun parseVideoTags(doc: Document): List<StrawberryVideoTag> {
        return doc.select("ul#tag-list li.tag-pill").mapNotNull { element: Element ->
            val id = element.id().removePrefix("tag-").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val name = element.selectFirst("span.tag-name")?.text()?.let(::cleanText)
                ?: return@mapNotNull null
            val score = element.selectFirst("span.tag-score")
                ?.text()?.toIntOrNull() ?: 0
            StrawberryVideoTag(tagId = id, name = name, score = score)
        }
    }

    private fun parseQualityLinks(doc: Document, baseUrl: String): List<StrawberryQualityLink> {
        return doc.select("nav.quality-links a.quality-link").mapNotNull { anchor ->
            val href = anchor.absUrl("href").takeIf { it.isNotBlank() }
                ?: resolveUrl(anchor.attr("href"), baseUrl)
                ?: return@mapNotNull null
            val quality = Regex("""[?&]quality=([^&]+)""")
                .find(href)?.groupValues?.getOrNull(1)
                ?: return@mapNotNull null
            val fullText = anchor.text().trim()
            val label = fullText.substringBefore('(').trim().ifBlank { quality }
            val sizeLabel = fullText.substringAfter('(', "").substringBefore(')').trim()
                .takeIf { it.isNotBlank() }?.let { "($it)" }
            val isActive = anchor.hasClass("is-active") ||
                    anchor.attr("aria-current") == "page"
            StrawberryQualityLink(
                quality = quality,
                label = label,
                url = href,
                sizeLabel = sizeLabel,
                isActive = isActive,
            )
        }
    }

    private fun mimeForFile(title: String): String = when (title.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        "mpg", "mpeg" -> "video/mpeg"
        else -> "application/octet-stream"
    }

    private fun parseRecommended(doc: Document, baseUrl: String): List<StrawberryVideoCard> {
        val sidebar = doc.selectFirst("div.watch-sidebar-videos")
            ?: doc.selectFirst("div.sidebar-card.watch-sidebar-videos")
            ?: return StrawberryListingParser.parseVideoCards(doc, baseUrl)
        return sidebar.select("a.sidebar-item").mapNotNull { anchor ->
            runCatching {
                val href = anchor.absUrl("href").takeIf { it.isNotBlank() }
                    ?: return@runCatching null
                val videoId = StrawberryHtmlUtils.extractVideoId(href)
                    ?: return@runCatching null
                val title = anchor.selectFirst("span.sidebar-title")?.text()?.let(::cleanText)
                    ?: return@runCatching null
                val uploader = anchor.selectFirst("span.sidebar-uploader")?.text()
                    ?.trim()?.takeIf { it.isNotBlank() }
                val thumb = anchor.selectFirst("span.sidebar-thumb-wrap img")
                val thumbUrl = thumb?.absUrl("src")?.takeIf { it.isNotBlank() } ?: ""
                val preview = thumb?.attr("data-preview")?.takeIf { it.isNotBlank() }
                val duration = anchor.selectFirst("span.thumb-duration-sm")?.text()?.trim()
                StrawberryThumbCache.put(videoId, thumbUrl)
                StrawberryVideoCard(
                    videoId = videoId,
                    title = title,
                    thumbUrl = thumbUrl,
                    previewUrl = preview,
                    duration = duration,
                    uploaderName = uploader,
                    viewsRaw = null,
                    views = null,
                )
            }.onFailure { Log.w(TAG, "Failed rec card: ${it.message}") }
                .getOrNull()
        }
    }

    fun parseDataDurationSeconds(html: String): Long? {
        val match = durationAttrRegex.find(html) ?: return null
        return match.groupValues[1].toDoubleOrNull()?.toLong()
    }
}
