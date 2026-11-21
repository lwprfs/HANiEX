package com.yenaly.han1meviewer.XXXTube.data.parser

import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeDetail
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeEntity
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeEntityPage
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeLink
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeListPage
import com.yenaly.han1meviewer.XXXTube.data.model.XXXTubeSource
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object XXXTubeParser {

    private val FLASHVARS_BLOCK = Regex("""var\s+flashvars\s*=\s*\{(.*?)\}\s*;""", RegexOption.DOT_MATCHES_ALL)
    private val FLASHVARS_PAIR = Regex("""(\w+)\s*:\s*'((?:[^'\\]|\\.)*)'""")
    private val QUALITY_IN_URL = Regex("""_(\d{3,4})p\.""", RegexOption.IGNORE_CASE)
    private val HEIGHT_IN_LABEL = Regex("""(\d{3,4})\s*p""", RegexOption.IGNORE_CASE)
    private val ISO_DURATION = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")
    private val LD_UPLOAD_DATE = Regex(""""uploadDate"\s*:\s*"([^"]+)"""")
    private val LD_DURATION = Regex(""""duration"\s*:\s*"([^"]+)"""")
    private val LD_CONTENT_URL = Regex(""""contentUrl"\s*:\s*"([^"]+)"""")
    private val LD_VIEWS = Regex(""""userInteractionCount"\s*:\s*"?(\d+)"?""")
    private val LD_AUTHOR_NAME = Regex(""""author"\s*:\s*\{[^}]*?"name"\s*:\s*"([^"]+)"""")
    private val SITE_TITLE_SUFFIX = Regex("""\s*[-–|]\s*X-X-X\.tube.*$""", RegexOption.IGNORE_CASE)

    private fun nextUrl(doc: Document): String? {
        val pager = doc.selectFirst("ul.pagination") ?: return null
        val next = pager.select("li.pager").lastOrNull() ?: return null
        if (next.hasClass("not-active")) return null
        return next.selectFirst("a[href]")?.absUrl("href")?.takeIf { it.isNotBlank() }
    }

    private fun Element.coverOf(): String {
        val img = selectFirst("img") ?: return ""
        for (attr in listOf("src", "data-src", "data-original", "data-lazy-src")) {
            val v = img.absUrl(attr)
            if (v.isNotBlank() && !v.startsWith("data:")) return v
        }
        return ""
    }

    private fun Element.cardTitle(link: Element): String =
        link.attr("title").trim()
            .ifBlank { selectFirst("img")?.attr("alt").orEmpty().trim() }
            .ifBlank { selectFirst(".title")?.text().orEmpty().trim() }

    private fun pathOf(url: String): String =
        runCatching { java.net.URI(url).path }.getOrNull().orEmpty()

    fun parseListPage(html: String): XXXTubeListPage {
        val doc = Jsoup.parse(html, XXXTubeConstants.BASE_URL)
        return XXXTubeListPage(
            items = parseVideoCards(doc).distinctBy { it.videoCode },
            nextUrl = nextUrl(doc),
        )
    }

    private fun parseVideoCards(doc: Document): List<HanimeInfo> =
        doc.select(".thumb-holder .thumb-inner").mapNotNull(::parseVideoCard)

    internal fun parseVideoCard(el: Element): HanimeInfo? {
        val link = el.selectFirst("a[href]") ?: return null
        val code = XXXTubeConstants.codeFromUrl(link.absUrl("href")) ?: return null
        val title = el.cardTitle(link)
        if (title.isBlank()) return null
        return HanimeInfo(
            title = title,
            coverUrl = el.coverOf(),
            videoCode = code,
            duration = el.selectFirst(".duration")?.text()?.trim()?.ifBlank { null },
            views = el.selectFirst(".rating")?.text()?.trim()?.ifBlank { null },
            itemType = HanimeInfo.NORMAL,
            currentArtist = el.selectFirst(".model-info .name")?.text()?.trim().orEmpty(),
        )
    }

    fun parseEntityPage(html: String, kind: XXXTubeConstants.EntityKind): XXXTubeEntityPage {
        val doc = Jsoup.parse(html, XXXTubeConstants.BASE_URL)
        val items = doc.select(".thumb-holder .thumb-inner").mapNotNull { el ->
            val link = el.selectFirst("a[href]") ?: return@mapNotNull null
            val path = pathOf(link.absUrl("href"))
            if (!kind.itemRegex.matches(path)) return@mapNotNull null
            val title = el.cardTitle(link)
            if (title.isBlank()) return@mapNotNull null
            XXXTubeEntity(title = title, coverUrl = el.coverOf(), path = path, kind = kind)
        }
        return XXXTubeEntityPage(items = items.distinctBy { it.path }, nextUrl = nextUrl(doc))
    }

    private fun parseFlashvars(doc: Document): Map<String, String> {
        val script = doc.select("script").map { it.data() }.firstOrNull { "flashvars" in it && "video_url" in it }
            ?: return emptyMap()
        val body = FLASHVARS_BLOCK.find(script)?.groupValues?.get(1) ?: return emptyMap()
        return FLASHVARS_PAIR.findAll(body).associate { m ->
            m.groupValues[1] to m.groupValues[2].replace("\\/", "/").replace("\\'", "'")
        }
    }

    private fun qualityLabel(url: String, text: String?): String {
        text?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return QUALITY_IN_URL.find(url)?.let { "${it.groupValues[1]}p" } ?: "Default"
    }

    internal fun buildSources(flashvars: Map<String, String>, fallbackUrl: String?): List<XXXTubeSource> {
        val referer = XXXTubeConstants.BASE_URL + "/"
        val out = mutableListOf<XXXTubeSource>()
        fun add(url: String?, text: String?) {
            val u = url?.trim().orEmpty()
            if (!u.startsWith("http")) return
            val label = qualityLabel(u, text)
            val height = HEIGHT_IN_LABEL.find(label)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            out += XXXTubeSource(label = label, url = u, height = height, referer = referer)
        }
        add(flashvars["video_url"], flashvars["video_url_text"])
        add(flashvars["video_alt_url"], flashvars["video_alt_url_text"])
        for (i in 2..9) add(flashvars["video_alt_url$i"], flashvars["video_alt_url${i}_text"])
        if (out.isEmpty()) add(fallbackUrl, null)
        // The same label can show up twice (e.g. a plain and an `_hd` variant); keep the first.
        return out.distinctBy { it.url }.distinctBy { it.label }
    }

    private fun isoDurationSeconds(value: String?): Long {
        val m = value?.let { ISO_DURATION.find(it) } ?: return 0L
        val h = m.groupValues[1].toLongOrNull() ?: 0L
        val min = m.groupValues[2].toLongOrNull() ?: 0L
        val s = m.groupValues[3].toLongOrNull() ?: 0L
        return h * 3600 + min * 60 + s
    }

    private fun link(a: Element, title: String = a.text().trim()) = XXXTubeLink(title, a.absUrl("href"))

    fun parseDetail(html: String, code: String): XXXTubeDetail? {
        val doc = Jsoup.parse(html, XXXTubeConstants.BASE_URL)
        val flashvars = parseFlashvars(doc)
        val ld = doc.select("script[type=application/ld+json]").map { it.data() }
            .firstOrNull { "VideoObject" in it }.orEmpty()

        val title = flashvars["video_title"]?.trim().orEmpty()
            .ifBlank { doc.selectFirst("h1.video-title")?.text().orEmpty().trim() }
            .ifBlank {
                doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty()
                    .replace(SITE_TITLE_SUFFIX, "").trim()
            }
        if (title.isBlank()) return null

        val sources = buildSources(flashvars, LD_CONTENT_URL.find(ld)?.groupValues?.get(1)?.replace("\\/", "/"))

        val cover = flashvars["preview_url"].orEmpty()
            .ifBlank { doc.selectFirst("meta[property=og:image]")?.absUrl("content").orEmpty() }

        val authorName = LD_AUTHOR_NAME.find(ld)?.groupValues?.get(1).orEmpty()

        fun Element.inThumbHolder() = parents().any { it.hasClass("thumb-holder") }
        val models = doc.select("a[href]")
            .filter { !it.inThumbHolder() }
            .filter { XXXTubeConstants.EntityKind.MODELS.itemRegex.matches(pathOf(it.absUrl("href"))) }
            .map { a ->
                val text = a.text().trim()
                val name = if (text.isBlank() || text.equals("View Model", ignoreCase = true)) {
                    authorName.ifBlank { pathOf(a.absUrl("href")).trim('/').substringAfterLast('/') }
                } else text
                link(a, name)
            }
            .distinctBy { it.url }
        val infoLinks = doc.select(".video-info-bottom a[href]").filter { !it.inThumbHolder() }
        val categories = infoLinks
            .filter { XXXTubeConstants.EntityKind.CATEGORIES.itemRegex.matches(pathOf(it.absUrl("href"))) }
            .map { link(it) }.filter { it.title.isNotBlank() }.distinctBy { it.url }
        val tags = infoLinks
            .filter { pathOf(it.absUrl("href")).startsWith("/tags/") }
            .map { link(it) }.filter { it.title.isNotBlank() }.distinctBy { it.url }

        val related = parseVideoCards(doc)
            .filter { XXXTubeConstants.idOf(it.videoCode) != XXXTubeConstants.idOf(code) }
            .distinctBy { it.videoCode }

        return XXXTubeDetail(
            code = code,
            title = title,
            coverUrl = cover,
            pageUrl = XXXTubeConstants.videoUrl(code),
            date = LD_UPLOAD_DATE.find(ld)?.groupValues?.get(1).orEmpty().substringBefore('T'),
            durationSeconds = isoDurationSeconds(LD_DURATION.find(ld)?.groupValues?.get(1)),
            views = LD_VIEWS.find(ld)?.groupValues?.get(1).orEmpty(),
            models = models,
            categories = categories,
            tags = tags,
            sources = sources,
            related = related,
        )
    }
}
