package com.yenaly.han1meviewer.Coser.data.parser

import com.yenaly.han1meviewer.Coser.common.CoserConstants
import com.yenaly.han1meviewer.Coser.data.model.CoserCard
import com.yenaly.han1meviewer.Coser.data.model.CoserDetail
import com.yenaly.han1meviewer.Coser.data.model.CoserListPage
import com.yenaly.han1meviewer.Coser.data.model.CoserMirror
import com.yenaly.han1meviewer.Coser.data.model.CoserTerm
import com.yenaly.han1meviewer.Coser.data.model.CoserTermKind
import com.yenaly.han1meviewer.Coser.data.model.CoserVideoRef
import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object CoserParser {

    private val COUNT_REGEX = Regex("""(\d[\d,.]*)""")
    private val SERIES_HREF = Regex("""/series/([^/?#]+)/?""")
    private val TAG_HREF = Regex("""/tag/([^/?#]+)/?""")
    private val MIRROR_PRIORITY = listOf("streamtape", "mixdrop", "doodstream")

    private fun clean(text: String?): String =
        text.orEmpty().replace('\u00a0', ' ').replace(Regex("""\s+"""), " ").trim()

    private fun pathOf(href: String): String {
        val noQuery = href.substringBefore('?').substringBefore('#')
        return if (noQuery.startsWith("http")) {
            noQuery.substringAfter("://").substringAfter('/', "").let { "/$it" }
        } else noQuery
    }

    private fun slugOfHref(href: String): String? = CoserConstants.slugFromPath(pathOf(href))

    private fun coverOf(root: Element): String {
        val img = root.selectFirst("picture img, img") ?: return ""
        val webp = root.selectFirst("picture source[srcset]")?.attr("srcset")
            ?.split(',')?.firstOrNull()?.trim()?.substringBefore(' ').orEmpty()
        val jpg = img.absUrl("src").ifBlank { img.attr("src") }
        // jpg first: it is universally decodable and has the same dimensions as the webp.
        return jpg.ifBlank { webp }
    }

    private fun parseCard(li: Element): CoserCard? {
        val a = li.selectFirst("a[href]") ?: return null
        val slug = slugOfHref(a.attr("href")) ?: return null
        val title = clean(li.selectFirst("h3")?.text()).ifBlank { clean(a.attr("title")) }
        if (title.isBlank()) return null
        val marks = li.select(".marks b")
        val quality = clean(marks.firstOrNull { it.hasClass("q") }?.text())
        val duration = clean(marks.firstOrNull { !it.hasClass("q") && !it.hasClass("items") }?.text())
        val extra = clean(marks.firstOrNull { it.hasClass("items") }?.text())
            .removePrefix("+").toIntOrNull() ?: 0
        val meta = li.select(".card-meta span").map { clean(it.text()) }
        val views = meta.firstOrNull { it.contains("view", ignoreCase = true) }.orEmpty()
            .replace(Regex("""\s*views?""", RegexOption.IGNORE_CASE), "")
        val date = meta.firstOrNull { it.matches(Regex("""\d{4}/\d{2}/\d{2}""")) }.orEmpty()
        return CoserCard(
            slug = slug,
            title = title,
            coverUrl = coverOf(li),
            seriesName = clean(li.selectFirst(".series-tag")?.text()),
            quality = quality,
            duration = duration,
            extraItems = extra,
            views = views,
            date = date,
        )
    }

    private fun parseCards(root: Element): List<CoserCard> =
        root.select("li.post-card").mapNotNull { parseCard(it) }.distinctBy { it.slug }

    private fun parseSeriesTerms(root: Element): List<CoserTerm> =
        root.select("li.series-card").mapNotNull { li ->
            val href = li.selectFirst("a[href]")?.attr("href") ?: return@mapNotNull null
            val slug = SERIES_HREF.find(pathOf(href))?.groupValues?.get(1) ?: return@mapNotNull null
            val name = clean(li.selectFirst("h2, h3")?.text())
            if (name.isBlank()) return@mapNotNull null
            val count = COUNT_REGEX.find(li.selectFirst(".tier")?.text().orEmpty())
                ?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
            val img = li.selectFirst("img")
            CoserTerm(
                kind = CoserTermKind.SERIES,
                slug = slug,
                name = name,
                thumbUrl = img?.absUrl("src").orEmpty().ifBlank { img?.attr("src").orEmpty() },
                count = count,
            )
        }

    private fun nextUrl(doc: Document): String? {
        val next = doc.selectFirst("nav.pagination a[rel=next]") ?: return null
        return next.absUrl("href").ifBlank { null }
    }

    fun parseListPage(html: String, baseUri: String, collectTerms: Boolean): CoserListPage {
        val doc = Jsoup.parse(html, baseUri)
        val main = doc.selectFirst("main") ?: doc.body()
        val terms = if (collectTerms) parseSeriesTerms(main) else emptyList()
        val cards = parseCards(main)
        return CoserListPage(cards = cards, terms = terms, nextUrl = nextUrl(doc))
    }

    /** Home page has two rails (weekly trending / latest); not used for paging but kept for reuse. */
    fun parseHome(html: String, baseUri: String): CoserListPage = parseListPage(html, baseUri, false)

    private fun termsOf(root: Element): Pair<List<CoserTerm>, List<CoserTerm>> {
        val series = ArrayList<CoserTerm>()
        val tags = ArrayList<CoserTerm>()
        for (a in root.select("ul.tags a[href]")) {
            val href = a.attr("href")
            val name = clean(a.text())
            if (name.isBlank()) continue
            val path = pathOf(href)
            val seriesMatch = SERIES_HREF.find(path)
            val tagMatch = TAG_HREF.find(path)
            when {
                seriesMatch != null -> series += CoserTerm(CoserTermKind.SERIES, seriesMatch.groupValues[1], name)
                tagMatch != null -> tags += CoserTerm(CoserTermKind.TAG, tagMatch.groupValues[1], name)
            }
        }
        return series to tags
    }

    private fun orderMirrors(list: List<CoserMirror>): List<CoserMirror> =
        list.sortedBy { m ->
            MIRROR_PRIORITY.indexOf(m.provider.lowercase()).let { if (it < 0) Int.MAX_VALUE else it }
        }

    private fun parseVideos(doc: Document): List<CoserVideoRef> {
        val json = doc.selectFirst("script#mirror-data")?.data()?.trim().orEmpty()
        if (json.isEmpty()) return emptyList()
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val out = ArrayList<CoserVideoRef>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val ms = o.optJSONArray("mirrors") ?: continue
            val mirrors = ArrayList<CoserMirror>()
            for (j in 0 until ms.length()) {
                val m = ms.optJSONObject(j) ?: continue
                val embed = m.optString("embed")
                val direct = m.optString("direct")
                if (embed.isBlank() && direct.isBlank()) continue
                mirrors += CoserMirror(m.optString("provider"), embed, direct)
            }
            if (mirrors.isEmpty()) continue
            val ordered = orderMirrors(mirrors)
            val first = ordered.first()
            out += CoserVideoRef(
                url = first.direct.ifBlank { first.embed },
                isEmbed = first.direct.isBlank(),
                label = "",
                mirrors = ordered,
                poster = o.optString("poster"),
                durationSec = o.optInt("duration", 0),
            )
        }
        return out
    }

    private fun parsePhotos(doc: Document): List<String> {
        val json = doc.selectFirst("script#gallery-data")?.data()?.trim().orEmpty()
        if (json.isNotEmpty()) {
            val arr = runCatching { JSONArray(json) }.getOrNull()
            if (arr != null) {
                val out = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val url = o.optString("full").ifBlank { o.optString("thumb") }
                    if (url.isNotBlank()) out += url
                }
                if (out.isNotEmpty()) return out
            }
        }
        return doc.select(".gallery a[href]").map { it.absUrl("href") }.filter { it.isNotBlank() }
    }

    fun parseDetail(html: String, slug: String, pageUrl: String): CoserDetail? {
        val doc = Jsoup.parse(html, pageUrl)
        val post = doc.selectFirst("article.post") ?: return null
        val title = clean(post.selectFirst(".credential-print h1, h1")?.text())
            .ifBlank { clean(doc.selectFirst("meta[property=og:title]")?.attr("content")) }
        if (title.isBlank()) return null
        val stats = post.select(".stats > span").map { clean(it.text()) }
        val views = stats.firstOrNull { it.contains("view", ignoreCase = true) }.orEmpty()
            .replace(Regex("""\s*views?""", RegexOption.IGNORE_CASE), "")
        val duration = stats.firstOrNull { it.matches(Regex("""\d+(:\d{2}){1,2}""")) }.orEmpty()
        val resolution = stats.firstOrNull { it.matches(Regex("""\d+\s*[x×]\s*\d+""")) }.orEmpty()
        val date = stats.firstOrNull { it.matches(Regex("""\d{4}/\d{2}/\d{2}""")) }.orEmpty()

        val videos = parseVideos(doc)
        val photos = parsePhotos(doc)
        val (series, tags) = termsOf(post)

        val cover = doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
            .ifBlank { videos.firstOrNull()?.poster.orEmpty() }
            .ifBlank { photos.firstOrNull().orEmpty() }

        val side = doc.selectFirst("aside.post-side")
        val related = side?.let { parseCards(it) }.orEmpty().filter { it.slug != slug }

        return CoserDetail(
            slug = slug,
            title = title,
            pageUrl = pageUrl,
            coverUrl = cover,
            photos = photos,
            videos = videos,
            categories = series,
            tags = tags,
            related = related,
            postedAt = date,
            views = views,
            duration = duration,
            resolution = resolution,
        )
    }
}
