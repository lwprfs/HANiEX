package com.yenaly.han1meviewer.Buondua.data.parser

import com.yenaly.han1meviewer.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaAlbumPage
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaLink
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaListPage
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaPhoto
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaVideo
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Selectors come from the captured search/list page (`div.items-row`, `.item-thumb`,
 * `.item-tags a.tag`, `.pagination-next`) and the site stylesheet (`.article-fulltext`,
 * `.article-tags`, `.article-related-list`). Album pages were captured without their body,
 * so everything on them is parsed defensively with fallbacks.
 */
object BuonduaParser {

    private val PAGE_SUFFIX = Regex("""\s*-?\s*\(\s*Page\s+\d+\s*/\s*\d+\s*\)\s*$""", RegexOption.IGNORE_CASE)
    private val PAGE_OF = Regex("""Page\s+(\d+)\s*/\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val PHOTO_COUNT = Regex("""(\d+)\s+photos?""", RegexOption.IGNORE_CASE)
    private val VIDEO_COUNT = Regex("""(\d+)\s+videos?""", RegexOption.IGNORE_CASE)
    private val IMAGE_EXT = Regex("""\.(jpe?g|png|webp|gif|avif)(\?|$)""", RegexOption.IGNORE_CASE)
    private val VIDEO_EXT = Regex("""\.(mp4|webm|m4v|mov|mkv)(\?|$)""", RegexOption.IGNORE_CASE)
    private val VIDEO_URL_IN_TEXT =
        Regex("""https?://[^\s"'<>\\]+?\.(?:mp4|webm|m4v|mov|mkv)(?:\?[^\s"'<>\\]*)?""", RegexOption.IGNORE_CASE)

    fun cleanTitle(raw: String): String =
        raw.replace('\u00A0', ' ').replace(PAGE_SUFFIX, "").trim()

    // ---------------------------------------------------------------- listings

    fun parseListPage(html: String): BuonduaListPage {
        val doc = Jsoup.parse(html, BuonduaConstants.BASE_URL + "/")
        val cards = doc.select("div.items-row").ifEmpty { doc.select("div.collection-item") }
        val items = cards.mapNotNull(::parseCard).distinctBy { it.videoCode }
        val heading = doc.selectFirst("div.content > h1, h1")?.text()?.trim()?.takeIf { it.isNotBlank() }
        return BuonduaListPage(items = items, nextUrl = nextPageUrl(doc), heading = heading)
    }

    private fun nextPageUrl(doc: Document): String? {
        val link = doc.selectFirst(".pagination a.pagination-next[href]")
            ?: doc.selectFirst(".pagination a[next][href]")
            ?: doc.selectFirst("a[rel=next][href]")
            ?: return null
        if (link.hasClass("disabled") || link.hasAttr("disabled")) return null
        val href = link.absUrl("href")
        return href.takeIf { it.isNotBlank() && !it.endsWith("#") }
    }

    private fun parseCard(el: Element): HanimeInfo? {
        val link = el.selectFirst("h2 a[href]")
            ?: el.selectFirst("a.item-link[href]")
            ?: el.selectFirst("a[href]")
            ?: return null
        val code = BuonduaConstants.albumCodeFromUrl(link.absUrl("href")) ?: return null
        val img = el.selectFirst("img")
        val title = link.text().trim().ifBlank { img?.attr("alt").orEmpty().trim() }
        if (title.isBlank()) return null
        val cover = imageUrlOf(img).orEmpty()
        val tags = el.select(".item-tags a.tag span, .item-tags a.tag")
            .map { it.text().trim() }.filter { it.isNotBlank() }.distinct()
        return HanimeInfo(
            title = title,
            coverUrl = cover,
            videoCode = code,
            duration = countLabelOf(title),
            genre = tags.joinToString(", ").ifBlank { null },
            itemType = HanimeInfo.NORMAL,
        )
    }

    /** "(157 photos + 2 videos)" -> "157 photos + 2 videos"; the title itself stays untouched. */
    fun countLabelOf(title: String): String? {
        val photos = PHOTO_COUNT.find(title)?.groupValues?.get(1)
        val videos = VIDEO_COUNT.find(title)?.groupValues?.get(1)
        return when {
            photos != null && videos != null -> "$photos photos + $videos videos"
            photos != null -> "$photos photos"
            videos != null -> "$videos videos"
            else -> null
        }
    }

    // ------------------------------------------------------------------- album

    fun parseAlbum(html: String, code: String, requestedPage: Int): BuonduaAlbumPage? {
        val doc = Jsoup.parse(html, BuonduaConstants.BASE_URL + "/")
        val rawTitle = doc.selectFirst(".article-header h1, .article h1, h1")?.text()
            ?.takeIf { it.isNotBlank() }
            ?: doc.title()
        val title = cleanTitle(rawTitle)
        if (title.isBlank()) return null

        val pageInfo = PAGE_OF.find(doc.title()) ?: PAGE_OF.find(
            doc.selectFirst("meta[name=description]")?.attr("content").orEmpty()
        )
        val page = pageInfo?.groupValues?.get(1)?.toIntOrNull() ?: requestedPage
        val totalPages = pageInfo?.groupValues?.get(2)?.toIntOrNull()
            ?: doc.select(".pagination a[href*=page=]").mapNotNull {
                Regex("""[?&]page=(\d+)""").find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
            }.maxOrNull()
            ?: 1

        val body = doc.selectFirst(".article-fulltext") ?: doc.selectFirst(".article") ?: doc.body()
        val photos = parsePhotos(doc, body)
        val videos = parseVideos(doc, body, html)

        val tags = doc.select(".article-tags a[href], a.tag[href^='/tag/']")
            .filter { !it.parents().any { p -> p.hasClass("items-row") } }
            .map { BuonduaLink(it.text().trim(), it.absUrl("href")) }
            .filter { it.title.isNotBlank() && it.url.contains("/tag/") }
            .distinctBy { it.url }

        val related = (doc.select(".article-related-list a[href]").mapNotNull { a ->
            val c = BuonduaConstants.albumCodeFromUrl(a.absUrl("href")) ?: return@mapNotNull null
            if (c == code) return@mapNotNull null
            val t = a.text().trim().ifBlank { return@mapNotNull null }
            HanimeInfo(
                title = t, coverUrl = "", videoCode = c,
                duration = countLabelOf(t), itemType = HanimeInfo.NORMAL,
            )
        } + doc.select("div.items-row").mapNotNull(::parseCard))
            .filter { it.videoCode != code }
            .distinctBy { it.videoCode }

        return BuonduaAlbumPage(
            code = code,
            title = title,
            pageUrl = BuonduaConstants.albumUrl(code),
            coverUrl = photos.firstOrNull()?.url.orEmpty(),
            photoCount = PHOTO_COUNT.find(title)?.groupValues?.get(1)?.toIntOrNull(),
            videoCount = VIDEO_COUNT.find(title)?.groupValues?.get(1)?.toIntOrNull(),
            page = page,
            totalPages = totalPages.coerceAtLeast(page),
            photos = photos,
            videos = videos,
            tags = tags,
            related = related,
        )
    }

    private fun imageUrlOf(img: Element?): String? {
        img ?: return null
        val raw = listOf("data-src", "data-original", "data-lazy-src", "src")
            .map { img.absUrl(it) }
            .firstOrNull { it.isNotBlank() && !it.startsWith("data:") }
        return raw
    }

    private fun hostOf(url: String): String =
        runCatching { java.net.URI(url).host }.getOrNull().orEmpty()

    private fun parsePhotos(doc: Document, body: Element): List<BuonduaPhoto> {
        fun collect(scope: Element, strict: Boolean): List<String> = scope.select("img")
            .asSequence()
            .filter { img ->
                img.parents().none {
                    it.hasClass("items-row") || it.hasClass("collection-item") ||
                            it.hasClass("article-related-list") || it.tagName() == "nav" ||
                            it.hasClass("search-box")
                }
            }
            .mapNotNull { imageUrlOf(it) }
            .filter { url ->
                val host = hostOf(url)
                BuonduaConstants.isImageHost(host) &&
                        (IMAGE_EXT.containsMatchIn(url) || !strict)
            }
            .toList()

        var urls = collect(body, strict = true)
        if (urls.isEmpty()) urls = collect(doc.body(), strict = true)
        return urls.distinct().mapIndexed { i, u -> BuonduaPhoto(u, i) }
    }

    private fun fileNameOf(url: String): String {
        val path = runCatching { java.net.URI(url).path }.getOrNull() ?: url.substringBefore('?')
        val name = path.substringAfterLast('/')
        return runCatching { java.net.URLDecoder.decode(name.replace("+", "%2B"), "UTF-8") }
            .getOrDefault(name)
    }

    private fun parseVideos(doc: Document, body: Element, rawHtml: String): List<BuonduaVideo> {
        val found = LinkedHashMap<String, BuonduaVideo>()

        fun add(url: String, hint: String?, poster: String?) {
            if (url.isBlank() || url.startsWith("data:") || url.startsWith("blob:")) return
            val host = hostOf(url)
            if (host.isBlank() || BuonduaConstants.isAdHost(host)) return
            if (!VIDEO_EXT.containsMatchIn(url)) return
            if (url in found) return
            val fileName = fileNameOf(url)
            val title = hint?.trim()?.takeIf { it.isNotBlank() } ?: fileName
            found[url] = BuonduaVideo(
                url = url,
                title = title,
                posterUrl = poster?.takeIf { it.isNotBlank() },
                fileName = fileName,
            )
        }

        val scopes = listOf(body).ifEmpty { listOf(doc.body()) }
        for (scope in scopes) {
            scope.select("video").forEach { v ->
                val poster = v.absUrl("poster")
                val hint = v.attr("title").ifBlank { v.attr("data-title") }
                    .ifBlank { v.attr("aria-label") }
                val direct = v.absUrl("src")
                if (direct.isNotBlank()) add(direct, hint, poster)
                v.select("source[src]").forEach { s -> add(s.absUrl("src"), hint.ifBlank { s.attr("title") }, poster) }
            }
            scope.select("a[href]").forEach { a ->
                val href = a.absUrl("href")
                if (VIDEO_EXT.containsMatchIn(href)) {
                    add(href, a.attr("download").ifBlank { a.attr("title") }, null)
                }
            }
            scope.select("[data-src], [data-video], [data-mp4]").forEach { e ->
                listOf("data-video", "data-mp4", "data-src").forEach { attr ->
                    val v = e.absUrl(attr)
                    if (VIDEO_EXT.containsMatchIn(v)) add(v, e.attr("title"), null)
                }
            }
        }

        // Last resort: a plain URL inside an inline script of the article body only.
        if (found.isEmpty()) {
            val articleHtml = body.outerHtml().ifBlank { rawHtml }
            VIDEO_URL_IN_TEXT.findAll(articleHtml).forEach { m ->
                add(m.value.replace("\\/", "/"), null, null)
            }
        }
        return found.values.toList()
    }
}
