package com.yenaly.han1meviewer.Cosplaytele.data.parser

import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleCard
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleDetail
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleListPage
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTerm
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleTermKind
import com.yenaly.han1meviewer.Cosplaytele.data.model.CosplayteleVideoRef
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object CosplayteleParser {

    private val PHOTO_COUNT_REGEX = Regex("""(\d+)\s*photos?""", RegexOption.IGNORE_CASE)
    private val VIDEO_COUNT_REGEX = Regex("""(\d+)\s*videos?""", RegexOption.IGNORE_CASE)
    private val TERM_PATH_REGEX = Regex("""^/(category|tag)/([^?#]+?)/?$""")
    private val TERM_COUNT_REGEX = Regex("""^(.*?)\s*[(\[]\s*(\d[\d,.]*)\s*[)\]]\s*$""")
    private val EMBED_REGEX = Regex("""https?:\\?/\\?/cossora\.stream\\?/embed\\?/[0-9a-fA-F-]{8,}""")
    private val TRAILING_INDEX_REGEX = Regex("""[-_]?\d+(_result)?$""")

    private val IMG_ATTRS = listOf(
        "data-src", "data-lazy-src", "data-original", "data-orig-file", "data-pagespeed-lazy-src",
        "data-full-url", "data-large-file", "src",
    )
    private val BG_ATTRS = listOf("data-bg", "data-bg-src", "data-background", "data-background-image")
    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "avif")
    private val VIDEO_EXT = setOf("mp4", "m3u8", "webm", "mov", "m4v")
    private val GENERIC_TITLES = setOf("result image", "read more", "continue reading", "more", "view")
    private val AD_HOSTS = listOf(
        "googletagmanager", "doubleclick", "googlesyndication", "google.com", "facebook.com",
        "ouo.io", "ouo.press", "twitter.com", "adsterra", "exoclick", "juicyads",
    )
    private val RELATED_HINTS = listOf(
        "related", "wpp", "widget", "popular", "recent", "comment", "sidebar", "navigation",
    )
    private val IMAGE_JUNK = listOf(
        "cropped-icon", "emoji", "gravatar", "/font-awesome/", "logo", "avatar", "-32x32", "-192x192",
    )

    private const val NOISE_SELECTOR =
        "script, style, noscript, template, #header, .header-wrapper, #masthead, #wide-nav, " +
                "#footer, .footer-wrapper, .mobile-sidebar, .off-canvas, #comments, .comments-area, " +
                "body > header, body > footer, body > nav, .asl_w_container"

    private const val SIDEBAR_SELECTOR = ".post-sidebar, #secondary, .sidebar, aside.widget-area"

    private fun isSiteHost(host: String): Boolean {
        val h = host.lowercase()
        return CosplayteleConstants.HOSTNAME.any { h == it || h.endsWith(".$it") }
    }

    fun postSlug(absUrl: String): String? {
        if (absUrl.isBlank()) return null
        val url = absUrl.toHttpUrlOrNull() ?: return null
        if (!isSiteHost(url.host)) return null
        if (url.querySize > 0) return null
        return CosplayteleConstants.slugFromPath(url.encodedPath)
    }

    private fun termOf(absUrl: String): Pair<CosplayteleTermKind, String>? {
        val url = absUrl.toHttpUrlOrNull() ?: return null
        if (!isSiteHost(url.host)) return null
        val path = url.encodedPath
        val searchQuery = url.queryParameter("s")
        if (!searchQuery.isNullOrBlank() && path.trim('/').isEmpty()) {
            return CosplayteleTermKind.SEARCH to searchQuery.trim()
        }
        if (path.contains("/page/")) return null
        val single = path.trim('/')
        if (!single.contains('/') && CosplayteleConstants.isPeriodSlug(single)) {
            return CosplayteleTermKind.PAGE to single
        }
        val match = TERM_PATH_REGEX.find(path) ?: return null
        val kind =
            if (match.groupValues[1] == "category") CosplayteleTermKind.CATEGORY else CosplayteleTermKind.TAG
        val slug = match.groupValues[2].substringAfterLast('/')
        if (slug.isBlank()) return null
        return kind to slug
    }

    private fun clean(text: String?): String =
        text.orEmpty().replace('\u00a0', ' ').replace(Regex("""\s+"""), " ").trim()

    private fun usableTitle(text: String?): Boolean {
        val t = clean(text)
        return t.length >= 2 && t.lowercase() !in GENERIC_TITLES
    }

    private fun firstSrcsetUrl(srcset: String, base: Element): String {
        val first = srcset.split(',').firstOrNull()?.trim()?.substringBefore(' ').orEmpty()
        if (first.isBlank()) return ""
        return runCatching { java.net.URI(base.baseUri()).resolve(first).toString() }.getOrDefault(first)
    }

    private fun imgUrl(img: Element): String {
        for (name in IMG_ATTRS) {
            val v = img.absUrl(name)
            if (v.isNotBlank() && !v.startsWith("data:")) return v
        }
        val srcset = img.attr("data-lazy-srcset").ifBlank { img.attr("data-srcset") }
            .ifBlank { img.attr("srcset") }
        return if (srcset.isBlank()) "" else firstSrcsetUrl(srcset, img)
    }

    private fun backgroundUrl(el: Element): String {
        val style = el.attr("style")
        val m = Regex("""url\(\s*['"]?([^'")]+)['"]?\s*\)""").find(style) ?: return ""
        val raw = m.groupValues[1]
        return runCatching { java.net.URI(el.baseUri()).resolve(raw).toString() }.getOrDefault(raw)
    }

    private fun extensionOf(url: String): String =
        url.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()

    private fun isImageUrl(url: String): Boolean = url.isNotBlank() && extensionOf(url) in IMAGE_EXT

    private fun buildCard(slug: String, title: String, cover: String): CosplayteleCard {
        val photos = PHOTO_COUNT_REGEX.find(title)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val videos = VIDEO_COUNT_REGEX.find(title)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return CosplayteleCard(
            slug = slug,
            title = title,
            coverUrl = cover,
            photoCount = photos,
            videoCount = videos,
        )
    }

    private fun contentRoot(doc: Document): Element {
        doc.select(NOISE_SELECTOR).remove()
        val root = doc.selectFirst("#content, main#main, #main, main, .page-wrapper") ?: doc.body()
        root.select(SIDEBAR_SELECTOR).remove()
        return root
    }

    private fun cardContainer(anchor: Element, root: Element): Element {
        var best: Element = anchor
        var cur: Element? = anchor.parent()
        var depth = 0
        while (cur != null && cur !== root && depth < 5) {
            val slugs = cur.select("a[href]").mapNotNull { postSlug(it.absUrl("href")) }.toSet()
            if (slugs.size > 1) break
            best = cur
            cur = cur.parent()
            depth++
        }
        return best
    }

    private fun pickTitle(container: Element, anchors: List<Element>): String {
        val heading = container.selectFirst(
            ".post-title, .wpp-post-title, .entry-title, h1, h2, h3, h4, h5, h6"
        )?.text()
        if (usableTitle(heading)) return clean(heading)
        for (a in anchors) if (usableTitle(a.attr("title"))) return clean(a.attr("title"))
        for (a in anchors) if (usableTitle(a.text())) return clean(a.text())
        for (img in container.select("img")) if (usableTitle(img.attr("alt"))) return clean(img.attr("alt"))
        return ""
    }

    private fun pickCover(container: Element): String {
        for (img in container.select("img")) {
            val url = imgUrl(img)
            if (url.isNotBlank() && IMAGE_JUNK.none { url.contains(it, ignoreCase = true) }) return url
        }
        for (el in container.select("[style*=background-image]")) {
            val url = backgroundUrl(el)
            if (url.isNotBlank()) return url
        }
        return ""
    }

    private fun parseCards(root: Element): List<CosplayteleCard> {
        val groups = LinkedHashMap<String, MutableList<Element>>()
        for (a in root.select("a[href]")) {
            val slug = postSlug(a.absUrl("href")) ?: continue
            groups.getOrPut(slug) { mutableListOf() }.add(a)
        }
        val out = ArrayList<CosplayteleCard>(groups.size)
        for ((slug, anchors) in groups) {
            val container = cardContainer(anchors.first(), root)
            val title = pickTitle(container, anchors)
            if (title.isBlank()) continue
            out += buildCard(slug, title, pickCover(container))
        }
        return out
    }

    private fun termKey(a: Element): String? =
        termOf(a.absUrl("href"))?.let { "${it.first.name}:${it.second}" }

    private fun termKeysIn(el: Element): Set<String> =
        el.select("a[href]").mapNotNull { termKey(it) }.toSet()

    private fun termContainer(anchor: Element, root: Element): Element {
        var best: Element = anchor
        var cur: Element? = anchor.parent()
        var depth = 0
        while (cur != null && cur !== root && depth < 6) {
            if (termKeysIn(cur).size > 1) break
            best = cur
            cur = cur.parent()
            depth++
        }
        return best
    }

    private fun looseBackground(el: Element): String {
        for (name in BG_ATTRS) {
            val v = el.absUrl(name)
            if (v.isNotBlank() && !v.startsWith("data:")) return v
        }
        return backgroundUrl(el)
    }

    private fun usableThumb(url: String): Boolean =
        url.isNotBlank() && !url.startsWith("data:") &&
                IMAGE_JUNK.none { url.contains(it, ignoreCase = true) } &&
                (isImageUrl(url) || url.contains("/uploads/"))

    /** First usable picture inside [scope]: <img>, <picture><source>, data-bg / inline background. */
    private fun thumbIn(scope: Element): String {
        val candidates = scope.select(
            "img, source[srcset], source[data-srcset], [data-bg], [data-bg-src], " +
                    "[data-background], [data-background-image], [style*=background]"
        )
        for (el in candidates) {
            val url = when (el.tagName()) {
                "img" -> imgUrl(el)
                "source" -> firstSrcsetUrl(el.attr("srcset").ifBlank { el.attr("data-srcset") }, el)
                else -> looseBackground(el)
            }
            if (usableThumb(url)) return url
        }
        return ""
    }

    /** Walks up from [anchor] while the ancestor still holds only this one term, looking for a picture. */
    private fun tileThumb(anchor: Element, root: Element): String {
        var cur: Element? = anchor
        var depth = 0
        while (cur != null && cur !== root && depth < 8) {
            if (termKeysIn(cur).size > 1) break
            val t = thumbIn(cur)
            if (t.isNotBlank()) return t
            cur = cur.parent()
            depth++
        }
        return ""
    }

    /**
     * Fallback for layouts where the picture is a sibling of the link rather than inside the same
     * wrapper: pair each term with the picture directly before it (else directly after it) in
     * document order. Pictures that sit in a post link or a widget are ignored so the sidebar's
     * "popular posts" thumbnails never get attached to a term.
     */
    private fun positionalThumbs(root: Element): Map<String, String> {
        val events = ArrayList<Pair<String?, String?>>()
        for (el in root.allElements) {
            when (el.tagName()) {
                "a" -> termKey(el)?.let { events += it to null }
                "img" -> {
                    val url = imgUrl(el)
                    if (usableThumb(url) && !isRelatedContext(el)) events += null to url
                }
            }
        }
        val termIdx = events.indices.filter { events[it].first != null }
        val out = HashMap<String, String>()
        val used = HashSet<String>()
        for ((n, idx) in termIdx.withIndex()) {
            val key = events[idx].first!!
            if (out.containsKey(key)) continue
            val prevIdx = if (n > 0) termIdx[n - 1] else -1
            val nextIdx = if (n + 1 < termIdx.size) termIdx[n + 1] else events.size
            val before = (prevIdx + 1 until idx).mapNotNull { events[it].second }
            val after = (idx + 1 until nextIdx).mapNotNull { events[it].second }
            val pick = before.lastOrNull { it !in used } ?: after.firstOrNull { it !in used }
            if (pick != null) {
                out[key] = pick
                used += pick
            }
        }
        return out
    }

    private class TermAcc(
        val kind: CosplayteleTermKind,
        val slug: String,
        var name: String,
        var nameIsFallback: Boolean,
        var thumb: String,
        var count: Int,
    )

    private fun parseTerms(root: Element): List<CosplayteleTerm> {
        val acc = LinkedHashMap<String, TermAcc>()
        for (a in root.select("a[href]")) {
            val (kind, slug) = termOf(a.absUrl("href")) ?: continue
            val key = "${kind.name}:$slug"
            val container = termContainer(a, root)
            var name = clean(a.text())
            if (!usableTitle(name)) name = clean(a.attr("title"))
            if (!usableTitle(name)) {
                name = clean(container.selectFirst("h1, h2, h3, h4, h5, h6, .title, .box-text, span")?.text())
            }
            if (!usableTitle(name)) name = clean(container.selectFirst("img")?.attr("alt"))
            if (!usableTitle(name)) name = ""
            var count = 0
            TERM_COUNT_REGEX.find(name)?.let { m ->
                count = m.groupValues[2].replace(",", "").replace(".", "").toIntOrNull() ?: 0
                name = clean(m.groupValues[1])
            }
            val fallback = name.isBlank()
            if (fallback) {
                name = if (kind == CosplayteleTermKind.SEARCH) slug
                else slug.replace('-', ' ').replaceFirstChar { it.uppercase() }
            }
            val thumb = tileThumb(a, root)
            val cur = acc[key]
            if (cur == null) {
                acc[key] = TermAcc(kind, slug, name, fallback, thumb, count)
            } else {
                if (cur.thumb.isBlank() && thumb.isNotBlank()) cur.thumb = thumb
                if (cur.nameIsFallback && !fallback) {
                    cur.name = name
                    cur.nameIsFallback = false
                }
                if (cur.count == 0) cur.count = count
            }
        }
        if (acc.values.any { it.thumb.isBlank() }) {
            val positional = positionalThumbs(root)
            val taken = acc.values.mapNotNull { it.thumb.takeIf { t -> t.isNotBlank() } }.toSet()
            for ((key, term) in acc) {
                if (term.thumb.isNotBlank()) continue
                val url = positional[key] ?: continue
                if (url !in taken) term.thumb = url
            }
        }
        return acc.values.map { CosplayteleTerm(it.kind, it.slug, it.name, it.thumb, it.count) }
    }

    private fun nextUrl(doc: Document): String? {
        val head = doc.selectFirst("link[rel=next]")?.absUrl("href").orEmpty()
        if (head.isNotBlank()) return head
        val link = doc.selectFirst(
            "a.next.page-numbers, a.next.page-number, a[rel=next], li.next a, a.next"
        )?.absUrl("href").orEmpty()
        return link.ifBlank { null }
    }

    fun parseListPage(html: String, collectTerms: Boolean): CosplayteleListPage {
        val doc = Jsoup.parse(html, CosplayteleConstants.BASE_URL)
        val next = nextUrl(doc)
        val root = contentRoot(doc)
        val terms = if (collectTerms) parseTerms(root) else emptyList()
        // Term pages (Best cosplayer / Top search / Categories) are tile grids; the posts the theme
        // lists around them (popular posts, footer widgets) are not part of the page.
        val cards = if (terms.isNotEmpty()) emptyList() else parseCards(root).distinctBy { it.slug }
        return CosplayteleListPage(cards = cards, terms = terms, nextUrl = next)
    }

    fun parseAjaxSearch(json: String): List<CosplayteleCard> {
        val html = runCatching { JSONObject(json).optString("html") }.getOrDefault("")
        if (html.isBlank()) return emptyList()
        val doc = Jsoup.parseBodyFragment(html, CosplayteleConstants.BASE_URL)
        val out = ArrayList<CosplayteleCard>()
        for (item in doc.select("div.item")) {
            val a = item.selectFirst("a.asl_res_url") ?: item.selectFirst("a[href]") ?: continue
            val slug = postSlug(a.absUrl("href")) ?: continue
            val title = clean(a.text())
            if (title.isBlank()) continue
            val cover = item.selectFirst("img.asl_image, img")?.let { imgUrl(it) }.orEmpty()
            out += buildCard(slug, title, cover)
        }
        return out.distinctBy { it.slug }
    }

    private fun isRelatedContext(el: Element): Boolean {
        var p: Element? = el.parent()
        while (p != null) {
            if (p.tagName() == "a" && postSlug(p.absUrl("href")) != null) return true
            val cls = p.className().lowercase()
            if (cls.isNotEmpty() && RELATED_HINTS.any { cls.contains(it) }) return true
            if (p.id().lowercase().contains("related")) return true
            p = p.parent()
        }
        return false
    }

    private fun detailTitle(doc: Document): String {
        val h1 = clean(doc.selectFirst("h1.entry-title, h1.post-title, article h1, h1")?.text())
        if (h1.isNotBlank()) return h1
        val og = clean(doc.selectFirst("meta[property=og:title]")?.attr("content"))
        val raw = og.ifBlank { clean(doc.title()) }
        return raw.removeSuffix("- Cosplaytele").trim().trimEnd('-').trim()
    }

    private fun isVideoEmbed(src: String): Boolean {
        if (src.isBlank() || src.startsWith("about:")) return false
        val lower = src.lowercase()
        if (AD_HOSTS.any { lower.contains(it) }) return false
        val host = src.toHttpUrlOrNull()?.host.orEmpty()
        return host.endsWith(CosplayteleConstants.STREAM_HOST) || lower.contains("/embed/") ||
                lower.contains("/player/") || lower.contains("/e/") || lower.contains("/v/")
    }

    private fun collectVideos(doc: Document, rawHtml: String): List<CosplayteleVideoRef> {
        val out = LinkedHashMap<String, CosplayteleVideoRef>()
        fun add(url: String, embed: Boolean) {
            val key = url.substringBefore('?')
            if (key.isNotBlank() && key !in out) out[key] = CosplayteleVideoRef(url = url, isEmbed = embed)
        }
        for (frame in doc.select("iframe")) {
            val src = listOf("src", "data-src", "data-lazy-src")
                .map { frame.absUrl(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
            if (isVideoEmbed(src)) add(src, true)
        }
        for (m in EMBED_REGEX.findAll(rawHtml)) add(m.value.replace("\\/", "/"), true)
        for (s in doc.select("video[src], video source[src]")) {
            val u = s.absUrl("src")
            if (u.isNotBlank()) add(u, false)
        }
        for (a in doc.select("a[href]")) {
            val u = a.absUrl("href")
            if (u.isNotBlank() && extensionOf(u) in VIDEO_EXT) add(u, false)
        }
        return out.values.toList()
    }

    private fun baseKey(url: String): String {
        val name = url.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
        return name.removeSuffix("-scaled").replace(TRAILING_INDEX_REGEX, "")
    }

    private fun collectPhotos(entry: Element): List<String> {
        val urls = ArrayList<String>()
        val seen = HashSet<String>()
        for (img in entry.select("img")) {
            if (isRelatedContext(img)) continue
            val linked = img.parent()?.takeIf { it.tagName() == "a" }?.absUrl("href").orEmpty()
            val src = if (isImageUrl(linked)) linked else imgUrl(img)
            if (!isImageUrl(src)) continue
            if (IMAGE_JUNK.any { src.contains(it, ignoreCase = true) }) continue
            if (seen.add(src.substringBefore('?'))) urls += src
        }
        if (urls.size < 4) return urls

        val dominant = urls.groupBy { baseKey(it) }.maxByOrNull { it.value.size } ?: return urls
        return if (dominant.value.size >= 3 && dominant.value.size * 2 >= urls.size) {
            urls.filter { baseKey(it) == dominant.key }
        } else urls
    }

    private fun collectArticleTerms(article: Element): Pair<List<CosplayteleTerm>, List<CosplayteleTerm>> {
        val cats = LinkedHashMap<String, CosplayteleTerm>()
        val tags = LinkedHashMap<String, CosplayteleTerm>()
        for (a in article.select("a[href]")) {
            val (kind, slug) = termOf(a.absUrl("href")) ?: continue
            if (kind == CosplayteleTermKind.PAGE || kind == CosplayteleTermKind.SEARCH) continue
            if (isRelatedContext(a)) continue
            var name = clean(a.text())
            if (name.isBlank()) name = clean(a.attr("title"))
            if (name.isBlank()) continue
            val term = CosplayteleTerm(kind, slug, name)
            if (kind == CosplayteleTermKind.CATEGORY) cats.putIfAbsent(slug, term)
            else tags.putIfAbsent(slug, term)
        }
        return cats.values.toList() to tags.values.toList()
    }

    fun parseDetail(html: String, slug: String): CosplayteleDetail? {
        val doc = Jsoup.parse(html, CosplayteleConstants.BASE_URL)
        val title = detailTitle(doc)
        if (title.isBlank()) return null

        val videos = collectVideos(doc, html)
        val article = doc.selectFirst("article, .article-inner, #content") ?: doc.body()
        val entry = article.selectFirst(".entry-content, .single-page, .post-content") ?: article
        val photos = collectPhotos(entry)
        val (categories, tags) = collectArticleTerms(article)

        val og = doc.selectFirst("meta[property=og:image]")?.absUrl("content").orEmpty()
        val cover = og.ifBlank { photos.firstOrNull().orEmpty() }
        val posted = doc.selectFirst("meta[property=article:published_time]")?.attr("content")
            ?.substringBefore('T')
            ?: doc.selectFirst("time[datetime]")?.attr("datetime")?.substringBefore('T').orEmpty()

        if (entry !== article && entry !== doc.body()) entry.remove()
        val related = parseCards(contentRoot(doc))
            .filter { it.slug != slug && it.coverUrl.isNotBlank() }
            .distinctBy { it.slug }

        return CosplayteleDetail(
            slug = slug,
            title = title,
            pageUrl = CosplayteleConstants.postUrl(slug),
            coverUrl = cover,
            photos = photos,
            videos = videos,
            categories = categories,
            tags = tags,
            related = related,
            postedAt = posted,
        )
    }
}
