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
        val cover = coverOf(el).ifBlank { imageUrlOf(img).orEmpty() }
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

        val related = collectRelated(doc, code)

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
        val direct = listOf("data-src", "data-original", "data-lazy-src", "data-lazy", "data-url", "data-echo", "src")
            .map { img.absUrl(it) }
            .firstOrNull { it.isNotBlank() && !it.startsWith("data:") }
        if (direct != null) return direct
        val srcset = img.attr("data-srcset").ifBlank { img.attr("srcset") }
        val first = srcset.split(',').firstOrNull()?.trim()?.substringBefore(' ').orEmpty()
        if (first.isBlank()) return null
        return runCatching { java.net.URI(img.baseUri()).resolve(first).toString() }.getOrNull()
    }

    private val BG_URL = Regex("""url\(\s*['"]?([^'")]+)['"]?\s*\)""")

    private fun backgroundUrlOf(el: Element): String? {
        val m = BG_URL.find(el.attr("style")) ?: return null
        val raw = m.groupValues[1]
        if (raw.startsWith("data:")) return null
        return runCatching { java.net.URI(el.baseUri()).resolve(raw).toString() }.getOrNull()
    }

    /** First usable thumbnail inside [container]: an <img> (lazy attributes included) or a CSS background. */
    private fun coverOf(container: Element): String {
        for (img in container.select("img")) {
            val u = imageUrlOf(img) ?: continue
            if (!BuonduaConstants.isAdHost(hostOf(u))) return u
        }
        for (el in container.select("[style*=background]")) {
            val u = backgroundUrlOf(el) ?: continue
            if (!BuonduaConstants.isAdHost(hostOf(u))) return u
        }
        return ""
    }

    private fun albumCodesIn(el: Element): Set<String> =
        el.select("a[href]").mapNotNull {
            val href = it.absUrl("href")
            if (!BuonduaConstants.isSiteHost(hostOf(href))) null else BuonduaConstants.albumCodeFromUrl(href)
        }.toSet()

    /** Largest ancestor of [anchor] that still only points at the album [code]. */
    private fun cardContainerOf(anchor: Element, code: String): Element {
        var best: Element = anchor
        var cur: Element? = anchor.parent()
        var depth = 0
        while (cur != null && depth < 5 && cur.tagName() != "body") {
            val codes = albumCodesIn(cur)
            if (codes.size > 1 || (codes.isNotEmpty() && code !in codes)) break
            best = cur
            cur = cur.parent()
            depth++
        }
        return best
    }

    /**
     * Every other album linked from the page outside the photo area - the cards above the photos,
     * the "popular" / "you may like" blocks and the plain related list. The same album usually
     * appears in several of these; they are merged so the entry that has a thumbnail wins.
     */
    private fun collectRelated(doc: Document, code: String): List<HanimeInfo> {
        val byCode = LinkedHashMap<String, HanimeInfo>()
        for (a in doc.body().select("a[href]")) {
            val skip = a.parents().any {
                it.hasClass("article-fulltext") || it.tagName() == "nav" || it.hasClass("navbar") ||
                        it.hasClass("search-box") || it.hasClass("article-tags") || it.hasClass("tags") ||
                        it.hasClass("pagination") || it.hasClass("article-header")
            }
            if (skip) continue
            val href = a.absUrl("href")
            if (!BuonduaConstants.isSiteHost(hostOf(href))) continue
            val c = BuonduaConstants.albumCodeFromUrl(href) ?: continue
            if (c == code) continue

            val container = cardContainerOf(a, c)
            val heading = container.selectFirst(".item-title, .page-header h2, h2, h3, h4")?.text()
            val title = cleanTitle(
                listOf(heading, a.attr("title"), a.text(), container.selectFirst("img")?.attr("alt"))
                    .firstOrNull { !it.isNullOrBlank() }.orEmpty()
            )
            if (title.isBlank()) continue
            val cover = coverOf(container)
            val tags = container.select(".item-tags a.tag span, .item-tags a.tag")
                .map { it.text().trim() }.filter { it.isNotBlank() }.distinct()

            val old = byCode[c]
            byCode[c] = if (old == null) {
                HanimeInfo(
                    title = title, coverUrl = cover, videoCode = c,
                    duration = countLabelOf(title),
                    genre = tags.joinToString(", ").ifBlank { null },
                    itemType = HanimeInfo.NORMAL,
                )
            } else {
                val bestTitle = if (title.length > old.title.length) title else old.title
                old.copy(
                    title = bestTitle,
                    coverUrl = old.coverUrl.ifBlank { cover },
                    duration = old.duration ?: countLabelOf(bestTitle),
                    genre = old.genre ?: tags.joinToString(", ").ifBlank { null },
                )
            }
        }
        return byCode.values.toList()
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

        if (found.isEmpty()) {
            val articleHtml = body.outerHtml().ifBlank { rawHtml }
            VIDEO_URL_IN_TEXT.findAll(articleHtml).forEach { m ->
                add(m.value.replace("\\/", "/"), null, null)
            }
        }
        return found.values.toList()
    }
}
