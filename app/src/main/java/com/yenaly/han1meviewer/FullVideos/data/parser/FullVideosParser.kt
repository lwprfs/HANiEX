package com.yenaly.han1meviewer.FullVideos.data.parser

import com.yenaly.han1meviewer.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosDetail
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosEntity
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosEntityPage
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosLink
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosListPage
import com.yenaly.han1meviewer.FullVideos.data.model.FullVideosSource
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object FullVideosParser {

    private val TITLE_DATE_SUFFIX = Regex("""\s*/\s*\d{1,2}\.\d{1,2}\.\d{4}\s*$""")
    private val TITLE_DATE = Regex("""/\s*(\d{1,2}\.\d{1,2}\.\d{4})\s*$""")
    private val HEIGHT_REGEX = Regex("""(\d{3,4})\s*p""", RegexOption.IGNORE_CASE)
    private val FILE_HEIGHT_REGEX = Regex("""_(\d{3,4})m?\.mp4""", RegexOption.IGNORE_CASE)

    private fun hasNext(doc: Document): Boolean =
        doc.selectFirst("div.pagination li.next:not(.no_link) a[href], div.pagination li.next a.link[href]") != null

    private fun Element.coverOf(): String {
        val img = selectFirst("img") ?: return ""
        for (attr in listOf("data-original", "data-src", "data-webp", "src")) {
            val v = img.absUrl(attr)
            if (v.isNotBlank() && !v.startsWith("data:")) return v
        }
        return ""
    }

    fun parseListPage(html: String): FullVideosListPage {
        val doc = Jsoup.parse(html, FullVideosConstants.BASE_URL)
        val items = doc.select("div.list-videos div.item").mapNotNull(::parseCard)
        return FullVideosListPage(items = items.distinctBy { it.videoCode }, hasNext = hasNext(doc))
    }

    internal fun parseCard(el: Element): HanimeInfo? {
        val link = el.selectFirst("a[href*=/videos/]") ?: return null
        val code = FullVideosConstants.codeFromUrl(link.absUrl("href")) ?: return null
        val title = el.selectFirst("strong.title")?.text().orEmpty()
            .ifBlank { link.attr("title") }
            .ifBlank { el.selectFirst("img")?.attr("alt").orEmpty() }
            .trim()
        val duration = el.selectFirst("span.duration, div.duration")?.text().orEmpty()
            .replace(Regex("(?i)full\\s*video"), "").trim().ifBlank { null }
        val views = el.selectFirst("div.views")?.text()?.trim()?.ifBlank { null }
        val rating = el.selectFirst("div.rating")?.text()?.trim()?.ifBlank { null }
        val added = el.selectFirst("div.added")?.text()?.trim()?.ifBlank { null }
        return HanimeInfo(
            title = title,
            coverUrl = el.coverOf(),
            videoCode = code,
            duration = duration,
            views = views,
            uploadTime = added,
            reviews = rating,
            itemType = HanimeInfo.NORMAL,
        )
    }

    private fun entityPath(href: String): String =
        runCatching { java.net.URI(href).path }.getOrNull().orEmpty().trimEnd('/')

    fun parseCategories(html: String): FullVideosEntityPage {
        val doc = Jsoup.parse(html, FullVideosConstants.BASE_URL)
        val items = doc.select("a.item[href*=/categories/]").mapNotNull { a ->
            val path = entityPath(a.absUrl("href"))
            if (!path.startsWith("/categories/") || path.count { it == '/' } != 2) return@mapNotNull null
            val name = (a.selectFirst("strong.title, .title")?.text() ?: a.attr("title").ifBlank { a.text() })
                .trim()
            if (name.isBlank()) return@mapNotNull null
            val count = a.selectFirst("div.videos, .videos")?.text()?.trim().orEmpty()
            FullVideosEntity(name, path, a.coverOf(), count)
        }.distinctBy { it.path }
        return FullVideosEntityPage(items, hasNext = false)
    }

    fun parseModels(html: String): FullVideosEntityPage {
        val doc = Jsoup.parse(html, FullVideosConstants.BASE_URL)
        val items = doc.select("a.item[href*=/models/]").mapNotNull { a ->
            val path = entityPath(a.absUrl("href"))
            if (!path.startsWith("/models/") || path.count { it == '/' } != 2) return@mapNotNull null
            val name = (a.selectFirst("strong.title, .title")?.text() ?: a.attr("title")).trim()
            if (name.isBlank()) return@mapNotNull null
            val count = a.selectFirst("div.videos, .videos")?.text()?.trim().orEmpty()
            FullVideosEntity(name, path, a.coverOf(), count)
        }.distinctBy { it.path }
        return FullVideosEntityPage(items, hasNext(doc))
    }

    fun parseSites(html: String): FullVideosEntityPage {
        val doc = Jsoup.parse(html, FullVideosConstants.BASE_URL)
        val items = doc.select("div.headline").mapNotNull { head ->
            val more = head.selectFirst("a[href*=/sites/]")
                ?: head.parent()?.selectFirst("a[href*=/sites/]") ?: return@mapNotNull null
            val path = entityPath(more.absUrl("href"))
            if (!path.startsWith("/sites/") || path.count { it == '/' } != 2) return@mapNotNull null

            val name = head.selectFirst("h2")?.text()?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val count = Regex("""\d[\d,.]*""").find(more.text())?.value.orEmpty()
            val box = head.nextElementSibling()
            val cover = box?.selectFirst("div.item")?.coverOf().orEmpty()
            FullVideosEntity(name, path, cover, count)
        }.distinctBy { it.path }
        return FullVideosEntityPage(items, hasNext(doc))
    }

    private fun link(a: Element) = FullVideosLink(a.text().trim(), a.absUrl("href"))

    fun parseDetail(html: String, code: String): FullVideosDetail? {
        val doc = Jsoup.parse(html, FullVideosConstants.BASE_URL)
        val rawTitle = doc.selectFirst("div.headline h1, h1")?.text().orEmpty().trim()
        val title = rawTitle.replace(TITLE_DATE_SUFFIX, "").trim()
            .ifBlank { doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty() }
        if (title.isBlank()) return null
        val date = TITLE_DATE.find(rawTitle)?.groupValues?.get(1).orEmpty()

        val sources = doc.select("video source[src]").mapNotNull { s ->
            val url = s.absUrl("src").ifBlank { return@mapNotNull null }
            val label = s.attr("label").ifBlank { s.attr("title") }.trim()
            val height = HEIGHT_REGEX.find(label)?.groupValues?.get(1)?.toIntOrNull()
                ?: FILE_HEIGHT_REGEX.find(url)?.groupValues?.get(1)?.toIntOrNull()
                ?: 0
            FullVideosSource(label.ifBlank { if (height > 0) "${height}p" else "Default" }, url, height)
        }.distinctBy { it.label }.sortedByDescending { it.height }

        val cover = doc.selectFirst("video[poster]")?.absUrl("poster").orEmpty()
            .ifBlank { doc.selectFirst("meta[property=og:image]")?.absUrl("content").orEmpty() }
        val duration = doc.selectFirst("meta[property=video:duration]")?.attr("content")
            ?.toLongOrNull() ?: 0L

        val related = doc.select("div.related-videos div.item, #list_videos_related_videos div.item")
            .mapNotNull(::parseCard)
            .filter { it.videoCode != code }

        return FullVideosDetail(
            code = code,
            title = title,
            coverUrl = cover,
            pageUrl = FullVideosConstants.videoUrl(code),
            date = date,
            durationSeconds = duration,
            channel = doc.selectFirst("a.btn_sponsor")?.let(::link),
            network = doc.selectFirst("a.btn_sponsor_group")?.let(::link),
            models = doc.select("a.btn_model").map(::link).filter { it.title.isNotBlank() }.distinctBy { it.url },
            categories = doc.select("div.info a[href*=/categories/], a.btn_tag")
                .map(::link).filter { it.title.isNotBlank() && it.url.contains("/categories/") }
                .distinctBy { it.url },
            sources = sources,
            related = related.distinctBy { it.videoCode },
        )
    }
}
