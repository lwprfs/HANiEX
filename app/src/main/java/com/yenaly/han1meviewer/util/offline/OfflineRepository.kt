package com.yenaly.han1meviewer.util.offline

import android.content.Context
import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.DownloadTreeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** Reads `Photos/` and `Shorts/` from the download path into [OfflineItem]s. */
object OfflineRepository {

    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    const val INFO_JSON = "info.json"

    private val json = Json { ignoreUnknownKeys = true }

    private fun ext(name: String) = name.substringAfterLast('.', "").lowercase()

    fun isImage(name: String) = ext(name) in IMAGE_EXTENSIONS

    suspend fun scan(context: Context, kind: OfflineKind): List<OfflineItem> = withContext(Dispatchers.IO) {
        val base = OfflineFs.find(context, listOf(kind.folder)) ?: return@withContext emptyList()
        val out = ArrayList<OfflineItem>()
        for (site in base.children()) {
            if (!site.isDirectory || site.name.startsWith(".")) continue
            for (folder in site.children()) {
                if (!folder.isDirectory || folder.name.startsWith(".")) continue
                runCatching {
                    val kids = folder.children()
                    when (kind) {
                        OfflineKind.PHOTOS -> photoItem(site.name, folder, kids)?.let(out::add)
                        OfflineKind.SHORTS -> out += shortItems(site.name, folder, kids)
                    }
                }
            }
        }
        out
    }

    private fun parse(node: DownloadTreeNode?) = node
        ?.let { OfflineFs.readText(it) }
        ?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }

    private fun photoItem(site: String, folder: DownloadTreeNode, kids: List<DownloadTreeNode>): OfflineItem? {
        val images = kids.filter { !it.isDirectory && isImage(it.name) }
            .sortedWith(compareBy({ it.name.length }, { it.name.lowercase() }))
        if (images.isEmpty()) return null
        val info = parse(kids.firstOrNull { it.name.equals(INFO_JSON, true) })
        val newest = images.maxOf { it.lastModified }
        return OfflineItem(
            key = "${OfflineKind.PHOTOS.folder}/$site/${folder.name}",
            kind = OfflineKind.PHOTOS,
            site = site,
            folder = folder.name,
            title = info?.str("title") ?: folder.name,
            channel = info?.str("channel"),
            dirSegments = listOf(OfflineKind.PHOTOS.folder, site, folder.name),
            mediaUris = images.map { it.uriString },
            mediaNames = images.map { it.name },
            coverUri = images.first().uriString,
            sizeBytes = images.sumOf { it.length },
            addedAt = info?.num("addedAt") ?: newest,
            sourceUrl = info?.str("sourceUrl"),
            views = info?.num("views"),
            likes = info?.num("likes"),
            durationSeconds = null,
            tags = info?.strList("tags").orEmpty(),
            description = info?.str("description"),
            ownedUris = emptyList(),
        )
    }

    private fun shortItems(site: String, folder: DownloadTreeNode, kids: List<DownloadTreeNode>): List<OfflineItem> {
        val videos = kids.filter { !it.isDirectory && DownloadLayout.isVideoFileName(it.name) }
        return videos.map { video ->
            val base = video.name.substringBeforeLast('.')
            val thumb = kids.firstOrNull { !it.isDirectory && isImage(it.name) && it.name.substringBeforeLast('.').equals(base, true) }
            val jsonNode = kids.firstOrNull { !it.isDirectory && it.name.equals("$base.json", true) }
            val info = parse(jsonNode)
            OfflineItem(
                key = "${OfflineKind.SHORTS.folder}/$site/${folder.name}/${video.name}",
                kind = OfflineKind.SHORTS,
                site = site,
                folder = folder.name,
                title = info?.str("title") ?: base,
                channel = info?.str("channel") ?: folder.name,
                dirSegments = listOf(OfflineKind.SHORTS.folder, site, folder.name),
                mediaUris = listOf(video.uriString),
                mediaNames = listOf(video.name),
                coverUri = thumb?.uriString,
                sizeBytes = video.length,
                addedAt = info?.num("addedAt") ?: video.lastModified,
                sourceUrl = info?.str("pageUrl") ?: info?.str("sourceUrl"),
                views = info?.num("views"),
                likes = info?.num("likes"),
                durationSeconds = info?.num("durationSeconds")?.toInt(),
                tags = info?.strList("tags").orEmpty(),
                description = info?.str("description"),
                ownedUris = listOfNotNull(video.uriString, thumb?.uriString, jsonNode?.uriString),
            )
        }
    }

    /** Deletes the given entries from storage. Photo albums lose their whole folder; clips lose their own files. */
    suspend fun delete(context: Context, items: List<OfflineItem>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val touchedDirs = LinkedHashSet<List<String>>()
        for (item in items) {
            when (item.kind) {
                OfflineKind.PHOTOS -> OfflineFs.deleteDir(context, item.dirSegments)
                OfflineKind.SHORTS -> {
                    item.ownedUris.forEach { OfflineFs.delete(context, it) }
                    touchedDirs += item.dirSegments
                }
            }
        }
        touchedDirs.forEach { dir ->
            val node = OfflineFs.find(context, dir) ?: return@forEach
            val hasClips = node.children().any { !it.isDirectory && DownloadLayout.isVideoFileName(it.name) }
            if (!hasClips) OfflineFs.delete(context, node.uriString)
        }
        OfflineGroups.forget(items.first().kind, items.map { it.key })
    }
}
