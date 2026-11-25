package com.yenaly.han1meviewer.SitesHub.Coomer.data.parser

import com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerConstants
import com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerThumbFallback
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerCard
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerDetail
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerListPage
import com.yenaly.han1meviewer.SitesHub.Coomer.data.model.CoomerTerm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object CoomerParser {

    private const val BLANK_POST = "blank_post"

    private fun clean(text: String?): String =
        text.orEmpty().replace('\u00a0', ' ').replace(Regex("""\s+"""), " ").trim()

    private fun pathOf(href: String): String {
        val noQuery = href.substringBefore('?').substringBefore('#')
        return if (noQuery.startsWith("http")) {
            noQuery.substringAfter("://").substringAfter('/', "").let { "/$it" }
        } else noQuery
    }

    private fun dateOf(raw: String): String =
        clean(raw).removePrefix("Added").trim().take(10)

    private fun imgUrl(root: Element?): String {
        val img = root?.selectFirst("img") ?: return ""
        val url = img.absUrl("src").ifBlank { img.attr("src") }
        return if (url.contains(BLANK_POST)) "" else url
    }

    private fun postCover(thumb: String, postPath: String): String {
        val creatorId = CoomerConstants.creatorIdFromPostPath(postPath)
        val avatar = creatorId?.let { CoomerConstants.avatarUrl(it) }.orEmpty()
        if (thumb.isBlank()) return avatar
        if (avatar.isNotBlank()) CoomerThumbFallback.register(thumb, avatar)
        return thumb
    }

    private fun parseCreatorTile(tile: Element): CoomerTerm? {
        val a = tile.selectFirst("a[href^=/u/], a[href*=/u/]") ?: return null
        val slug = CoomerConstants.creatorSlugFromPath(pathOf(a.attr("href"))) ?: return null
        val name = clean(tile.selectFirst("p")?.text())
            .ifBlank { clean(a.attr("title")) }
            .ifBlank { slug.substringAfterLast('/') }
        return CoomerTerm(
            slug = slug,
            name = name,
            thumbUrl = imgUrl(tile),
            service = clean(tile.selectFirst(".th-tag")?.text()).ifBlank { slug.substringBefore('/') },
        )
    }

    private fun parseCreatorTiles(root: Element): List<CoomerTerm> =
        root.select(".thumb").mapNotNull { parseCreatorTile(it) }.distinctBy { it.slug }

    private fun parsePostTiles(root: Element): List<CoomerCard> =
        root.select(".thumb").mapNotNull { tile ->
            val a = tile.selectFirst("a[href*=/p/]") ?: return@mapNotNull null
            val slug = CoomerConstants.slugFromPath(pathOf(a.attr("href"))) ?: return@mapNotNull null
            val title = clean(tile.selectFirst("span")?.text()).ifBlank { clean(a.attr("title")) }
                .ifBlank { "Post $slug" }
            CoomerCard(
                slug = slug,
                title = title,
                coverUrl = postCover(imgUrl(tile), pathOf(a.attr("href"))),
            )
        }.distinctBy { it.slug }

    private fun parsePostBlocks(root: Element, creatorName: String): List<CoomerCard> =
        root.select(".posts-list .post").mapNotNull { block ->
            val a = block.selectFirst("a[href*=/p/]") ?: return@mapNotNull null
            val slug = CoomerConstants.slugFromPath(pathOf(a.attr("href"))) ?: return@mapNotNull null
            val title = clean(block.selectFirst("h3")?.text())
                .ifBlank { clean(block.selectFirst("p")?.text()) }
                .ifBlank { "Post $slug" }
            CoomerCard(
                slug = slug,
                title = title,
                coverUrl = postCover(imgUrl(block.selectFirst(".post-thumbs")), pathOf(a.attr("href"))),
                creatorName = creatorName,
                date = dateOf(block.selectFirst(".post-date")?.text().orEmpty()),
            )
        }.distinctBy { it.slug }

    private fun nextUrl(doc: Document): String? {
        val next = doc.selectFirst("""a[title="Next Page"], .pagination a.next, a[rel=next]""") ?: return null
        val href = next.absUrl("href").ifBlank { next.attr("href") }
        return href.takeIf { it.isNotBlank() && it != "#" }
    }

    fun parseListPage(html: String, baseUri: String): CoomerListPage {
        val doc = Jsoup.parse(html, baseUri)
        val main = doc.selectFirst("main") ?: doc.body()
        val creatorName = clean(main.selectFirst(".model-info h1, .model-info .model-name")?.text())
        val blocks = parsePostBlocks(main, creatorName)
        val postTiles = parsePostTiles(main)
        val terms = if (postTiles.isEmpty() && blocks.isEmpty()) parseCreatorTiles(main) else emptyList()
        return CoomerListPage(
            cards = if (blocks.isNotEmpty()) blocks else postTiles,
            terms = terms,
            nextUrl = nextUrl(doc),
        )
    }

    fun parseDetail(html: String, slug: String, pageUrl: String): CoomerDetail? {
        val doc = Jsoup.parse(html, pageUrl)
        val wrap = doc.selectFirst(".post-wrap") ?: return null
        val body = wrap.selectFirst(".post-body") ?: wrap

        val videos = body.select("video source[src], video[src]")
            .map { it.absUrl("src").ifBlank { it.attr("src") } }
            .filter { it.isNotBlank() }
            .distinct()
        val photos = body.select("img[src]")
            .map { it.absUrl("src").ifBlank { it.attr("src") } }
            .filter { it.isNotBlank() && !it.contains(BLANK_POST) }
            .distinct()

        val info = doc.selectFirst("article.model-info")
        val creatorLink = doc.select(".pagination a[href*=/u/], article.model-info a[href*=/u/]")
            .firstNotNullOfOrNull { CoomerConstants.creatorSlugFromPath(pathOf(it.attr("href"))) }
        val creatorName = clean(info?.selectFirst(".model-name, h1")?.text())
        val service = clean(info?.selectFirst(".as-tag")?.text())
        val creator = creatorLink?.let {
            CoomerTerm(
                slug = it,
                name = creatorName.ifBlank { it.substringAfterLast('/') },
                thumbUrl = imgUrl(info),
                service = service.ifBlank { it.substringBefore('/') },
            )
        }

        val caption = wrap.children().firstOrNull { it.tagName() == "p" }?.wholeText()?.trim().orEmpty()
        val heading = clean(wrap.selectFirst("h1")?.text())
        val title = heading
            .ifBlank { clean(caption).take(80) }
            .ifBlank { creatorName.ifBlank { "Post $slug" } }

        val related = doc.select(".thumbs-list").flatMap { parseCreatorTiles(it) }
            .filter { it.slug != creator?.slug }

        val cover = photos.firstOrNull().orEmpty().ifBlank { creator?.thumbUrl.orEmpty() }
        if (photos.isNotEmpty() && creator != null) CoomerThumbFallback.register(cover, creator.thumbUrl)

        return CoomerDetail(
            slug = slug,
            title = title,
            pageUrl = pageUrl,
            coverUrl = cover,
            photos = photos,
            videos = videos,
            creator = creator,
            related = related,
            caption = caption,
            postedAt = dateOf(wrap.selectFirst(".post-date")?.text().orEmpty()),
        )
    }
}
