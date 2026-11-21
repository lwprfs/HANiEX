package com.yenaly.han1meviewer.Wumaobi.data.parser

import com.yenaly.han1meviewer.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiDetail
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiEntity
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiEntityPage
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiFavCollection
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiFavFolder
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiLink
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiListPage
import com.yenaly.han1meviewer.Wumaobi.data.model.WumaobiSource
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object WumaobiParser {

    private val PAGE_GO_REGEX = Regex("""page_go\((\d+)\)""")
    private val CAT_COUNT_REGEX = Regex("""^(.*?)\s*\((\d[\d,]*)\)\s*$""", RegexOption.DOT_MATCHES_ALL)
    private val FAV_COUNT_REGEX = Regex("""\d+\s*/\s*\d+""")
    private val SOURCE_OBJECT_REGEX = Regex("""\{[^{}]*\}""")
    private val JS_TRUE = setOf("!0", "true", "1")

    private fun jsField(obj: String, name: String): String? =
        Regex("""\b$name\s*:\s*"((?:[^"\\]|\\.)*)"""").find(obj)?.groupValues?.get(1)?.let(::jsUnescape)

    private fun jsFlag(obj: String, name: String): Boolean =
        Regex("""\b$name\s*:\s*(!0|!1|true|false|1|0)""").find(obj)?.groupValues?.get(1) in JS_TRUE

    private fun jsUnescape(raw: String): String {
        if (!raw.contains('\\')) return raw
        val out = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                val n = raw[i + 1]
                when {
                    n == 'u' && raw.length >= i + 6 -> {
                        val hex = raw.substring(i + 2, i + 6)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            out.append(code.toChar())
                            i += 6
                            continue
                        }
                        out.append(n)
                    }

                    n == 'x' && raw.length >= i + 4 -> {
                        val code = raw.substring(i + 2, i + 4).toIntOrNull(16)
                        if (code != null) {
                            out.append(code.toChar())
                            i += 4
                            continue
                        }
                        out.append(n)
                    }

                    else -> out.append(n)
                }
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    /** The pagination bar uses `page_go(N)` handlers; there is a next page when N > current. */
    private fun hasNext(doc: Document): Boolean {
        val bar = doc.selectFirst("ul.pagination") ?: return false
        val current = bar.selectFirst("li.active a")?.text()?.trim()?.toIntOrNull() ?: 1
        return bar.select("a[onclick]").any { a ->
            val n = PAGE_GO_REGEX.find(a.attr("onclick"))?.groupValues?.get(1)?.toIntOrNull()
            n != null && n > current
        }
    }

    private fun rawPath(href: String): String =
        runCatching { java.net.URI(href).rawPath }.getOrNull().orEmpty().trimEnd('/')

    fun parseListPage(html: String): WumaobiListPage {
        val doc = Jsoup.parse(html, WumaobiConstants.BASE_URL)
        val items = doc.select("div.card.h-100").mapNotNull(::parseCard)
        return WumaobiListPage(items = items.distinctBy { it.videoCode }, hasNext = hasNext(doc))
    }

    internal fun parseCard(el: Element): HanimeInfo? {
        val link = el.select("a[href]").firstOrNull {
            WumaobiConstants.codeFromUrl(it.attr("href")) != null
        } ?: return null
        val code = WumaobiConstants.codeFromUrl(link.attr("href")) ?: return null
        val img = el.selectFirst("img")
        val title = el.selectFirst("h3.card-title")?.text().orEmpty()
            .ifBlank { img?.attr("alt").orEmpty() }
            .trim()
        val cover = img?.absUrl("src").orEmpty()
        val actress = el.selectFirst("a[href*=/actress/] p.card-text")?.text()?.trim()
            ?.ifBlank { null }
        val badge = el.selectFirst("span.badge")?.text()?.trim()?.ifBlank { null }
        return HanimeInfo(
            title = title,
            coverUrl = cover,
            videoCode = code,
            genre = badge,
            currentArtist = actress,
            itemType = HanimeInfo.NORMAL,
        )
    }

    fun parseCategoryPane(html: String, pane: String): WumaobiEntityPage {
        val doc = Jsoup.parse(html, WumaobiConstants.BASE_URL)
        val items = doc.select("div.cat-pane[data-pane=$pane] a.cat-tag").mapNotNull { a ->
            val path = rawPath(a.absUrl("href"))
            if (path.isBlank()) return@mapNotNull null
            val text = a.text().trim()
            val m = CAT_COUNT_REGEX.find(text)
            val name = (m?.groupValues?.get(1) ?: text).trim()
            if (name.isBlank()) return@mapNotNull null
            WumaobiEntity(name, path, "", m?.groupValues?.get(2).orEmpty())
        }.distinctBy { it.path }
        return WumaobiEntityPage(items, hasNext = false)
    }

    fun parseActresses(html: String): WumaobiEntityPage {
        val doc = Jsoup.parse(html, WumaobiConstants.BASE_URL)
        val items = doc.select("div.card.h-100").mapNotNull { card ->
            val a = card.selectFirst("a[href*=/actress/]") ?: return@mapNotNull null
            val path = rawPath(a.absUrl("href"))
            if (!path.startsWith("/actress/")) return@mapNotNull null
            val name = card.selectFirst("h3")?.text()?.trim().orEmpty()
                .ifBlank { card.selectFirst("img")?.attr("alt").orEmpty() }
            if (name.isBlank()) return@mapNotNull null
            WumaobiEntity(name, path, card.selectFirst("img")?.absUrl("src").orEmpty(), "")
        }.distinctBy { it.path }
        return WumaobiEntityPage(items, hasNext(doc))
    }

    private fun link(a: Element): WumaobiLink {
        val text = a.text().trim()
        val label = text.substringAfter(':', text).substringAfter('：', text).trim()
        return WumaobiLink(label.ifBlank { text }, a.absUrl("href"))
    }

    private fun parseSources(doc: Document): List<WumaobiSource> {
        val script = doc.select("script").map { it.data() }
            .firstOrNull { it.contains("QUALITY_SOURCE") } ?: return emptyList()
        val start = script.indexOf("QUALITY_SOURCE")
        val open = script.indexOf('[', start)
        if (open < 0) return emptyList()
        val close = script.indexOf("]", open).takeIf { it > open } ?: script.length
        val segment = script.substring(open, close + 1)
        return SOURCE_OBJECT_REGEX.findAll(segment).mapNotNull { m ->
            val obj = m.value
            val url = jsField(obj, "url")?.takeIf { it.startsWith("http") } ?: return@mapNotNull null
            val tier = jsField(obj, "tier").orEmpty().replace('_', ' ').trim()
            val line = jsField(obj, "line").orEmpty().trim()
            val label = listOf(tier, line).filter { it.isNotBlank() }.joinToString(" · ")
                .ifBlank { "Default" }
            WumaobiSource(label = label, url = url, height = 0, hq = jsFlag(obj, "hq"))
        }.distinctBy { it.label }.toList()
    }

    fun parseDetail(html: String, code: String): WumaobiDetail? {
        val doc = Jsoup.parse(html, WumaobiConstants.BASE_URL)
        val title = doc.selectFirst("h2.video-title")?.text()?.trim().orEmpty()
            .ifBlank { doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty() }
            .ifBlank { doc.title().substringBefore(" - ") }
        if (title.isBlank()) return null

        val cover = doc.selectFirst("video#playerCnt")?.absUrl("poster").orEmpty()
            .ifBlank { doc.selectFirst("meta[property=og:image]")?.absUrl("content").orEmpty() }

        val metaLinks = doc.select("div.post-meta a[href]")
        val actresses = metaLinks.filter { it.attr("href").contains("/actress/") }
            .map(::link).filter { it.title.isNotBlank() }.distinctBy { it.url }
        val series = metaLinks.firstOrNull { it.attr("href").contains("/series/") }?.let(::link)

        val favCollections = doc.select("input[name=edit-fav-check-box]").mapNotNull { input ->
            val id = input.attr("data-col-id").ifBlank { return@mapNotNull null }
            val name = input.parent()?.selectFirst("label")?.text()?.trim().orEmpty().ifBlank { id }
            WumaobiFavCollection(id = id, name = name, checked = input.hasAttr("checked"))
        }
        val loggedIn = doc.selectFirst("button[data-mdb-target=#edit-fav-modal]") != null ||
            favCollections.isNotEmpty()

        val related = doc.select("div.card.h-100").mapNotNull(::parseCard)
            .filter { it.videoCode != code }.distinctBy { it.videoCode }

        return WumaobiDetail(
            code = code,
            title = title,
            coverUrl = cover,
            pageUrl = WumaobiConstants.videoUrl(code),
            date = "",
            durationSeconds = 0L,
            channel = series,
            network = null,
            models = actresses,
            categories = emptyList(),
            sources = parseSources(doc),
            related = related,
            loggedIn = loggedIn,
            favCollections = favCollections,
        )
    }

    fun parseFavFolders(html: String): List<WumaobiFavFolder> {
        val doc = Jsoup.parse(html, WumaobiConstants.BASE_URL)
        return doc.select("div.col[style*=position]").mapNotNull { wrapper ->
            val a = wrapper.selectFirst("a[href*=/user/fav/]") ?: return@mapNotNull null
            val name = wrapper.selectFirst("h3.card-title")?.text()?.trim().orEmpty()
                .ifBlank { rawPath(a.attr("href")).substringAfterLast('/') }
            if (name.isBlank()) return@mapNotNull null
            WumaobiFavFolder(name, FAV_COUNT_REGEX.find(wrapper.text())?.value.orEmpty())
        }.distinctBy { it.name }
    }
}
