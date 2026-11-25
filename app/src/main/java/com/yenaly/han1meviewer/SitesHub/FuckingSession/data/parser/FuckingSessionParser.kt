package com.yenaly.han1meviewer.SitesHub.FuckingSession.data.parser

import com.yenaly.han1meviewer.SitesHub.FuckingSession.common.FuckingSessionConstants
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.model.FuckingSessionDetail
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.model.FuckingSessionLink
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.model.FuckingSessionListPage
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object FuckingSessionParser {

    private val DURATION_REGEX = Regex("""Duration:\s*(?:(\d{1,2}):)?(\d{1,3}):(\d{2})""", RegexOption.IGNORE_CASE)
    private val SITE_TITLE_SUFFIX = Regex("""\s*[-–|]\s*Fuckingsession.*$""", RegexOption.IGNORE_CASE)

    private fun hasNext(doc: Document): Boolean =
        doc.selectFirst("link[rel=next][href]") != null ||
                doc.selectFirst("a.next[href], a.g1-link.next[href]") != null

    private fun Element.coverOf(): String {
        val img = selectFirst("img") ?: return ""
        for (attr in listOf("data-src", "data-lazy-src", "src")) {
            val v = img.absUrl(attr)
            if (v.isNotBlank() && !v.startsWith("data:")) return v
        }
        return ""
    }

    fun parseListPage(html: String): FuckingSessionListPage {
        val doc = Jsoup.parse(html, FuckingSessionConstants.BASE_URL)
        val items = doc.select("article[class*=entry-tpl]").mapNotNull(::parseCard)
        return FuckingSessionListPage(
            items = items.distinctBy { it.videoCode },
            hasNext = hasNext(doc),
        )
    }

    internal fun parseCard(el: Element): HanimeInfo? {
        val link = el.selectFirst(".entry-title a[href]") ?: el.selectFirst("a.g1-frame[href]")
        ?: return null
        val code = FuckingSessionConstants.codeFromUrl(link.absUrl("href")) ?: return null
        val title = link.text().trim()
            .ifBlank { el.selectFirst("a.g1-frame")?.attr("title").orEmpty().trim() }
            .ifBlank { el.selectFirst("img")?.attr("alt").orEmpty().trim() }
        if (title.isBlank()) return null
        val date = el.selectFirst("time.entry-date")?.attr("datetime").orEmpty()
            .substringBefore('T').ifBlank { null }
        return HanimeInfo(
            title = title,
            coverUrl = el.coverOf(),
            videoCode = code,
            uploadTime = date,
            itemType = HanimeInfo.NORMAL,
        )
    }

    private fun link(a: Element) = FuckingSessionLink(a.text().trim(), a.absUrl("href"))

    fun parseDetail(html: String, code: String): FuckingSessionDetail? {
        val doc = Jsoup.parse(html, FuckingSessionConstants.BASE_URL)
        val title = doc.selectFirst("h1.entry-title")?.text().orEmpty().trim()
            .ifBlank {
                doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty()
                    .replace(SITE_TITLE_SUFFIX, "").trim()
            }
        if (title.isBlank()) return null

        val content = doc.selectFirst("div.entry-content")

        val embeds = content?.select("iframe[src]")
            ?.map { it.absUrl("src") }
            ?.filter { it.startsWith("http") }
            ?.distinct()
            .orEmpty()

        val siteHosts = FuckingSessionConstants.HOSTNAME
        val mirrors = content?.select("a[href]")
            ?.filter { it.text().trim().startsWith("http") }
            ?.mapNotNull { a ->
                val url = a.absUrl("href")
                val host = runCatching { java.net.URI(url).host }.getOrNull().orEmpty().lowercase()
                if (host.isBlank() || siteHosts.any { host == it || host.endsWith(".$it") }) return@mapNotNull null
                FuckingSessionLink(host.removePrefix("www."), url)
            }
            ?.distinctBy { it.url }
            .orEmpty()

        val duration = content?.text()?.let { DURATION_REGEX.find(it) }?.let { m ->
            val h = m.groupValues[1].toLongOrNull() ?: 0L
            val min = m.groupValues[2].toLongOrNull() ?: 0L
            val s = m.groupValues[3].toLongOrNull() ?: 0L
            h * 3600 + min * 60 + s
        } ?: 0L

        val date = doc.selectFirst("time.entry-date[datetime]")?.attr("datetime").orEmpty()
            .substringBefore('T')

        val cover = doc.selectFirst("meta[property=og:image]")?.absUrl("content").orEmpty()
            .ifBlank { content?.selectFirst("img[src]")?.absUrl("src").orEmpty() }

        val related = doc.select("aside.g1-related-entries article[class*=entry-tpl]")
            .mapNotNull(::parseCard)
            .filter { it.videoCode != code }

        return FuckingSessionDetail(
            code = code,
            title = title,
            coverUrl = cover,
            pageUrl = FuckingSessionConstants.videoUrl(code),
            date = date,
            durationSeconds = duration,
            categories = doc.select("header.entry-header a.entry-category")
                .map(::link).filter { it.title.isNotBlank() }.distinctBy { it.url },
            tags = doc.select("p.entry-tags a.entry-tag")
                .map(::link).filter { it.title.isNotBlank() }.distinctBy { it.url },
            embeds = embeds,
            mirrors = mirrors,
            sources = emptyList(),
            related = related.distinctBy { it.videoCode },
        )
    }
}
