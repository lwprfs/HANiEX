package com.yenaly.han1meviewer.SitesHub.OnAir.github

import android.content.Context
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.state.DownloadState
import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.DownloadTreeNode
import com.yenaly.han1meviewer.util.offline.OfflineFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object OnAirDownloadedIds {

    private const val MIN_VIDEO_BYTES = 1024L
    private const val MAX_DEPTH = 6
    private const val MAX_NODES = 20_000

    private fun idOf(code: String): String? =
        if (code.startsWith("onair:", ignoreCase = true)) code.substringAfter(':').takeIf { it.isNotBlank() } else null

    suspend fun collect(context: Context): Set<String> = withContext(Dispatchers.IO) {
        val all = runCatching { DownloadDatabase.instance.hanimeDownloadDao.getAll() }.getOrDefault(emptyList())
        val finished = all.filter { it.state == DownloadState.Finished }.mapNotNull { idOf(it.videoCode) }.toSet()
        val inProgress = all.filter { it.state != DownloadState.Finished }.mapNotNull { idOf(it.videoCode) }.toSet()
        (finished + scanFolder(context)) - (inProgress - finished)
    }

    private fun scanFolder(context: Context): Set<String> {
        val root = runCatching { OfflineFs.root(context) }.getOrNull() ?: return emptySet()
        val out = HashSet<String>()
        val queue = ArrayDeque<Pair<DownloadTreeNode, List<String>>>()
        queue.add(root to emptyList())
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val (node, segments) = queue.removeFirst()
            visited++
            val children = runCatching { node.children() }.getOrDefault(emptyList())
            val hasVideo = segments.isNotEmpty() && children.any {
                !it.isDirectory && DownloadLayout.isVideoFileName(it.name) && it.length >= MIN_VIDEO_BYTES
            }
            if (hasVideo) {
                val fromInfo = children.firstOrNull { !it.isDirectory && it.name.equals("info.json", true) }
                    ?.let { OfflineFs.readText(it) }
                    ?.let { runCatching { JSONObject(it).optString("videoCode") }.getOrNull() }
                    ?.let(::idOf)
                val legacy = if (segments.first().equals("OnAir", true))
                    segments.last().takeIf { it.matches(Regex("\\d{1,12}")) } else null
                (fromInfo ?: legacy)?.let(out::add)
                continue
            }
            if (segments.size >= MAX_DEPTH) continue
            children.filter { it.isDirectory && !it.name.startsWith(".") && it.name.isNotBlank() }
                .filterNot {
                    segments.isEmpty() && (it.name.equals(DownloadLayout.PHOTOS_FOLDER, true) ||
                            it.name.equals(DownloadLayout.SHORTS_FOLDER, true))
                }
                .forEach { queue.add(it to segments + it.name) }
        }
        return out
    }
}
