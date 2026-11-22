package com.yenaly.han1meviewer.util.offline

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import com.yenaly.han1meviewer.HFileManager
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.util.DownloadFolderRegistry
import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.DownloadScanner
import com.yenaly.han1meviewer.util.DownloadTreeNode
import com.yenaly.han1meviewer.util.FapFolderImageLocation
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.SafTreeNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

object OfflineMigration {

    private const val TAG = "OfflineMigration"
    private const val KEY_DONE = "offline_layout_v3_done"
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    var postponedThisSession = false

    fun isDone(): Boolean = Preferences.preferenceSp.getBoolean(KEY_DONE, false)

    fun markDone() = Preferences.preferenceSp.edit { putBoolean(KEY_DONE, true) }

    data class Plan(val videoFolders: Int, val photoAlbums: Int, val shortFolders: Int) {
        val total get() = videoFolders + photoAlbums + shortFolders
        val needed get() = total > 0
    }

    data class Report(val moved: Int, val failed: Int)

    private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    fun migrateAsync(
        context: Context,
        onProgress: (done: Int, total: Int) -> Unit,
        onDone: (Report) -> Unit,
    ) {
        val app = context.applicationContext
        appScope.launch {
            val report = runCatching { migrate(app, onProgress) }.getOrElse { Report(0, 1) }
            withContext(Dispatchers.Main) { onDone(report) }
        }
    }

    private enum class LeafKind { VIDEO, PHOTO, SHORT }

    private class Leaf(
        val kind: LeafKind,
        val segments: List<String>,
        val chain: List<DownloadTreeNode>,
        val node: DownloadTreeNode,
        val children: List<DownloadTreeNode>,

        val inRoot: Boolean,
    )

    private fun isReservedRoot(name: String) =
        DownloadLayout.RESERVED_ROOT_FOLDERS.any { it.equals(name, true) }

    private fun collect(
        root: DownloadTreeNode,
        inRoot: Boolean,
        allowVideoLeaves: Boolean,
    ): List<Leaf> {
        val result = ArrayList<Leaf>()
        class Item(val node: DownloadTreeNode, val segments: List<String>, val chain: List<DownloadTreeNode>)

        val queue = ArrayDeque<Item>()
        queue.add(Item(root, emptyList(), emptyList()))
        var visited = 0
        while (queue.isNotEmpty() && visited < 20_000) {
            val (node, segments, chain) = queue.removeFirst().let { Triple(it.node, it.segments, it.chain) }
            visited++
            val children = node.children()
            if (segments.isNotEmpty()) {
                val hasVideo = children.any { !it.isDirectory && DownloadLayout.isVideoFileName(it.name) }
                val hasImage = children.any { !it.isDirectory && OfflineRepository.isImage(it.name) }
                val isGifPath = segments.size >= 3 && segments[1].equals(DownloadLayout.GIFS_FOLDER, true)
                when {
                    hasVideo && isGifPath -> {
                        result += Leaf(LeafKind.SHORT, segments, chain, node, children, inRoot); continue
                    }
                    hasVideo && allowVideoLeaves -> {
                        result += Leaf(LeafKind.VIDEO, segments, chain, node, children, inRoot); continue
                    }
                    hasImage && !hasVideo && !allowVideoLeaves -> {
                        result += Leaf(LeafKind.PHOTO, segments, chain, node, children, inRoot)
                    }
                }
                if (segments.size >= 8) continue
            }
            children.filter { it.isDirectory && !it.name.startsWith(".") && it.name.isNotBlank() }
                .filterNot { segments.isEmpty() && inRoot && isReservedRoot(it.name) }
                .forEach { queue.add(Item(it, segments + it.name, chain + it)) }
        }
        return result
    }

    private fun legacyLeaves(context: Context): List<Leaf> {
        val leaves = ArrayList<Leaf>()
        OfflineFs.root(context)?.let { leaves += collect(it, inRoot = true, allowVideoLeaves = true) }
        val imageTree = FapFolderImageLocation.uri
        val downloadTree = Preferences.safDownloadPath
        if (imageTree != null && imageTree != downloadTree && FapFolderImageLocation.isReady(context)) {
            runCatching {
                leaves += collect(SafTreeNode.root(context, imageTree.toUri()), inRoot = false, allowVideoLeaves = false)
            }
        }
        return leaves
    }

    suspend fun detect(context: Context): Plan = withContext(Dispatchers.IO) {
        val leaves = runCatching { legacyLeaves(context) }.getOrDefault(emptyList())
        Plan(
            videoFolders = leaves.count { it.kind == LeafKind.VIDEO },
            photoAlbums = leaves.count { it.kind == LeafKind.PHOTO },
            shortFolders = leaves.count { it.kind == LeafKind.SHORT },
        ).also { if (!it.needed) markDone() }
    }

    suspend fun migrate(
        context: Context,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Report = withContext(Dispatchers.IO) {
        val leaves = legacyLeaves(context)
        var done = 0
        var moved = 0
        var failed = 0
        onProgress(0, leaves.size)
        for (leaf in leaves) {
            try {
                val ok = when (leaf.kind) {
                    LeafKind.VIDEO -> migrateVideo(context, leaf)
                    LeafKind.PHOTO -> migratePhotos(context, leaf)
                    LeafKind.SHORT -> migrateShorts(context, leaf)
                }
                if (ok) moved++ else failed++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed++
                Log.w(TAG, "migrate failed: ${leaf.segments}", e)
            }
            done++
            onProgress(done, leaves.size)
        }
        if (leaves.any { it.kind == LeafKind.VIDEO }) {
            runCatching {
                SafFileManager.scanAndImportHanimeDownloads(context, DownloadDatabase.instance.hanimeDownloadDao)
            }
        }
        if (failed == 0) markDone()
        Report(moved, failed)
    }

    private fun moveInto(context: Context, leaf: Leaf, target: List<String>, rename: (String) -> String = { it }): Boolean {
        var ok = true
        for (child in leaf.children) {
            if (child.isDirectory || child.name.startsWith(OfflineFs.NO_MEDIA)) continue
            val newName = rename(child.name)
            val result = if (leaf.inRoot) {
                OfflineFs.moveFile(context, leaf.segments, child.name, target, newName)
            } else {
                OfflineFs.importNode(context, child, target, newName)
            }
            if (result == null) ok = false
        }
        return ok
    }

    private fun prune(context: Context, leaf: Leaf) {

        for (i in leaf.chain.indices.reversed()) {
            val node = leaf.chain[i]
            val rest = node.children().filterNot { it.name == OfflineFs.NO_MEDIA }
            if (rest.isEmpty()) OfflineFs.delete(context, node.uriString) else break
        }
    }

    private fun migrateVideo(context: Context, leaf: Leaf): Boolean {
        val infoNode = leaf.children.firstOrNull { !it.isDirectory && it.name.equals("info.json", true) }
        val info = infoNode?.let { OfflineFs.readText(it) }
            ?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
        val code = info?.str("videoCode") ?: DownloadLayout.legacyVideoCodeFromSegments(leaf.segments)
        val videoFile = leaf.children.firstOrNull { !it.isDirectory && DownloadLayout.isVideoFileName(it.name) }
        val title = info?.str("title")
            ?: videoFile?.let { DownloadScanner.parseVideoFileName(it.name).first }
            ?: leaf.segments.last()
        val site = leaf.segments.first().let {
            if (it.equals(HFileManager.HANIME_DOWNLOAD_FOLDER, true)) "Hanime" else DownloadLayout.sanitize(it)
        }
        var target = listOf(DownloadLayout.VIDEOS_FOLDER, site, DownloadLayout.sanitize(title))
        val owner = DownloadFolderRegistry.codeFor(target)
        if (owner != null && owner != code) {
            target = target.dropLast(1) + (target.last() + " [" + DownloadLayout.sanitize(code ?: leaf.segments.last(), "x").take(24) + "]")
        }
        if (!OfflineFs.ensureDir(context, target)) return false
        val ok = moveInto(context, leaf, target)
        if (code != null) {
            DownloadFolderRegistry.set(code, target)
            if (infoNode == null) {
                val text = buildJsonObject {
                    put("version", 3)
                    put("videoCode", code)
                    put("title", title)
                    put("site", site)
                    put("addDate", System.currentTimeMillis())
                }.toString()
                OfflineFs.writeText(context, target, "info.json", text)
            }
        }
        if (ok) prune(context, leaf)
        return ok
    }

    private fun migratePhotos(context: Context, leaf: Leaf): Boolean {
        val layout = OfflineDownloads.layoutFor(leaf.segments, leaf.segments.last())
        val target = listOf(OfflineKind.PHOTOS.folder, layout.siteFolder, layout.folder)
        if (!OfflineFs.ensureDir(context, target)) return false
        val ok = moveInto(context, leaf, target)
        val names = OfflineFs.find(context, target)?.children().orEmpty()
            .filter { !it.isDirectory && OfflineRepository.isImage(it.name) }
            .map { it.name }.sorted()
        if (names.isNotEmpty() && OfflineFs.find(context, target)?.children()
                ?.none { it.name.equals(OfflineRepository.INFO_JSON, true) } != false
        ) {
            val text = buildJsonObject {
                put("version", 3)
                put("kind", "photos")
                put("site", layout.siteFolder)
                put("title", layout.folder)
                layout.channel?.let { put("channel", it) }
                put("addedAt", System.currentTimeMillis())
                put("count", names.size)
            }.toString()
            OfflineFs.writeText(context, target, OfflineRepository.INFO_JSON, text)
        }
        if (ok) prune(context, leaf)
        return ok
    }

    private fun migrateShorts(context: Context, leaf: Leaf): Boolean {
        val site = DownloadLayout.sanitize(leaf.segments.first())
        val creator = DownloadLayout.sanitize(leaf.segments.last(), "Unknown")
        val target = listOf(OfflineKind.SHORTS.folder, site, creator)
        if (!OfflineFs.ensureDir(context, target)) return false
        val ok = moveInto(context, leaf, target)
        OfflineFs.find(context, target)?.children().orEmpty()
            .filter { !it.isDirectory && DownloadLayout.isVideoFileName(it.name) }
            .forEach { clip ->
                val base = clip.name.substringBeforeLast('.')
                val hasJson = OfflineFs.find(context, target)?.children()
                    ?.any { it.name.equals("$base.json", true) } == true
                if (!hasJson) {
                    val text = buildJsonObject {
                        put("version", 3)
                        put("kind", "shorts")
                        put("site", site)
                        put("title", base)
                        put("channel", creator)
                        put("videoFile", clip.name)
                        put("addedAt", clip.lastModified.takeIf { it > 0 } ?: System.currentTimeMillis())
                    }.toString()
                    OfflineFs.writeText(context, target, "$base.json", text)
                }
            }
        if (ok) prune(context, leaf)
        return ok
    }
}
