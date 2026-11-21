package com.yenaly.han1meviewer.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.yenaly.han1meviewer.logic.dao.download.HanimeDownloadDao
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.state.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.InputStream
import kotlin.coroutines.coroutineContext

/** One entry (file or folder) of a download tree, independent of SAF vs plain files. */
interface DownloadTreeNode {
    val name: String
    val isDirectory: Boolean
    val length: Long
    val lastModified: Long

    /** What goes into `HanimeDownloadEntity.videoUri` / `coverUri`. */
    val uriString: String
    fun children(): List<DownloadTreeNode>
    fun openInput(): InputStream?
}

/** Private-storage mode: plain `java.io.File`. */
class FileTreeNode(private val file: File) : DownloadTreeNode {
    override val name: String get() = file.name
    override val isDirectory: Boolean get() = file.isDirectory
    override val length: Long get() = file.length()
    override val lastModified: Long get() = file.lastModified()
    override val uriString: String get() = Uri.fromFile(file).toString()
    override fun children(): List<DownloadTreeNode> =
        file.listFiles()?.map { FileTreeNode(it) }.orEmpty()

    override fun openInput(): InputStream? = runCatching { file.inputStream() }.getOrNull()
}

/**
 * SAF mode. One ContentResolver query per directory (with a projection) instead of one per file,
 * which is what makes `DocumentFile.listFiles()` so slow.
 */
class SafTreeNode(
    private val context: Context,
    private val treeUri: Uri,
    private val documentId: String,
    override val name: String,
    private val mimeType: String,
    override val length: Long,
    override val lastModified: Long,
) : DownloadTreeNode {

    override val isDirectory: Boolean get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR

    private val documentUri: Uri get() = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    override val uriString: String get() = documentUri.toString()

    override fun children(): List<DownloadTreeNode> {
        if (!isDirectory) return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val out = ArrayList<DownloadTreeNode>()
        runCatching {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                ),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    out += SafTreeNode(
                        context = context,
                        treeUri = treeUri,
                        documentId = c.getString(0) ?: continue,
                        name = c.getString(1).orEmpty(),
                        mimeType = c.getString(2).orEmpty(),
                        length = if (c.isNull(3)) 0L else c.getLong(3),
                        lastModified = if (c.isNull(4)) 0L else c.getLong(4),
                    )
                }
            }
        }.onFailure { Log.w("DownloadScanner", "list children failed: $name", it) }
        return out
    }

    override fun openInput(): InputStream? =
        runCatching { context.contentResolver.openInputStream(documentUri) }.getOrNull()

    companion object {
        fun root(context: Context, treeUri: Uri): SafTreeNode = SafTreeNode(
            context = context,
            treeUri = treeUri,
            documentId = DocumentsContract.getTreeDocumentId(treeUri),
            name = "",
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            length = 0L,
            lastModified = 0L,
        )
    }
}

enum class ImportFailureReason {
    INVALID_FOLDER_NAME,
    IMPORT_EXCEPTION,
    UNRECOGNIZED_FOLDER,
    UNREADABLE_INFO_JSON,
}

data class ImportFailure(
    val name: String,
    val reason: ImportFailureReason,
)

data class ImportResult(
    val scannedFolders: Int = 0,
    val imported: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val failures: List<ImportFailure> = emptyList(),
    val status: Status = Status.OK,
) {
    enum class Status { OK, ROOT_MISSING, NO_PERMISSION, CANCELLED }

    val successCount get() = imported + updated
    val failureCount get() = failures.size
}

/** What a `info.json` (v1 or v2) tells us about one downloaded folder. */
internal data class DownloadInfo(
    val videoCode: String?,
    val title: String?,
    val coverUrl: String?,
    val quality: String?,
    val videoFile: String?,
    val addDate: Long?,
    val videoUrls: Map<String, String>,
)

/**
 * Finds downloaded videos in a download tree and (re)registers them in the DB.
 *
 * A "video leaf" is a folder that directly contains a video file. The video code comes from
 * `info.json` when present, otherwise from the folder path ([DownloadLayout.videoCodeFromSegments]).
 */
object DownloadScanner {

    private const val TAG = "DownloadScanner"
    private const val MAX_DEPTH = 8
    private const val MAX_FOLDERS = 20_000
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private fun ext(name: String) = name.substringAfterLast('.', "").lowercase()

    private class Leaf(val segments: List<String>, val node: DownloadTreeNode, val children: List<DownloadTreeNode>)

    /** Splits `Title_720P.mp4` into (`Title`, `720P`), and `NoUnderscore.mp4` into (`NoUnderscore`, `default`). */
    fun parseVideoFileName(fileName: String): Pair<String, String> {
        val base = fileName.substringBeforeLast('.', fileName)
        val i = base.lastIndexOf('_')
        return if (i <= 0 || i == base.length - 1) base to "default"
        else base.substring(0, i) to base.substring(i + 1)
    }

    internal fun parseInfo(text: String): DownloadInfo? = runCatching {
        val o: JsonObject = json.parseToJsonElement(text).jsonObject
        fun s(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        val urls = HashMap<String, String>()
        (o["videoUrls"] as? JsonObject)?.forEach { (q, v) ->
            ((v as? JsonObject)?.get("link") as? JsonPrimitive)?.contentOrNull?.let { urls[q] = it }
        }
        DownloadInfo(
            videoCode = s("videoCode"),
            title = s("title"),
            coverUrl = s("coverUrl"),
            quality = s("quality"),
            videoFile = s("videoFile"),
            addDate = (o["addDate"] as? JsonPrimitive)?.longOrNull,
            videoUrls = urls,
        )
    }.getOrNull()

    /**
     * Walks [root] (BFS, depth and folder caps, cancellable) and imports every video leaf.
     * Runs under a mutex so two scans can never overlap.
     */
    suspend fun scan(
        dao: HanimeDownloadDao,
        root: DownloadTreeNode,
        onProgress: ((done: Int, total: Int, current: String?) -> Unit)? = null,
    ): ImportResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val failures = mutableListOf<ImportFailure>()
            val leaves = mutableListOf<Leaf>()
            var scanned = 0

            data class Item(val node: DownloadTreeNode, val segments: List<String>)

            val queue = ArrayDeque<Item>()
            queue.add(Item(root, emptyList()))
            while (queue.isNotEmpty() && scanned < MAX_FOLDERS) {
                coroutineContext.ensureActive()
                val (node, segments) = queue.removeFirst()
                scanned++
                val children = node.children()
                if (segments.isNotEmpty() && children.any { !it.isDirectory && DownloadLayout.isVideoFileName(it.name) }) {
                    leaves += Leaf(segments, node, children)
                    continue
                }
                if (segments.size >= MAX_DEPTH) continue
                children.filter { it.isDirectory && !it.name.startsWith(".") && it.name.isNotBlank() }
                    .forEach { queue.add(Item(it, segments + it.name)) }
            }
            onProgress?.invoke(0, leaves.size, null)

            var imported = 0
            var updated = 0
            var skipped = 0
            val seen = HashSet<String>()

            leaves.forEachIndexed { index, leaf ->
                coroutineContext.ensureActive()
                try {
                    val infoNode = leaf.children.firstOrNull { !it.isDirectory && it.name.equals("info.json", true) }
                    var info: DownloadInfo? = null
                    if (infoNode != null) {
                        info = infoNode.openInput()?.use { it.bufferedReader().readText() }?.let(::parseInfo)
                        if (info == null) failures += ImportFailure(leaf.node.name, ImportFailureReason.UNREADABLE_INFO_JSON)
                    }
                    val code = info?.videoCode ?: DownloadLayout.videoCodeFromSegments(leaf.segments)
                    if (code == null) {
                        failures += ImportFailure(leaf.segments.joinToString("/"), ImportFailureReason.UNRECOGNIZED_FOLDER)
                    } else {
                        val videos = leaf.children
                            .filter { !it.isDirectory && DownloadLayout.isVideoFileName(it.name) }
                            .sortedBy { it.name.lowercase() }
                        val images = leaf.children.filter { !it.isDirectory && ext(it.name) in IMAGE_EXTENSIONS }
                        for (video in videos) {
                            val (parsedTitle, parsedQuality) = parseVideoFileName(video.name)
                            val fromInfo = info?.takeIf { it.videoFile.equals(video.name, true) }
                            val quality = fromInfo?.quality ?: parsedQuality
                            val title = fromInfo?.title ?: info?.title?.takeIf { videos.size == 1 } ?: parsedTitle
                            if (!seen.add("$code|$quality")) {
                                skipped++
                                continue
                            }
                            val videoBase = video.name.substringBeforeLast('.')
                            val cover = images.firstOrNull { it.name.substringBeforeLast('.').equals(parsedTitle, true) }
                                ?: images.firstOrNull { it.name.substringBeforeLast('.').equals(videoBase, true) }
                                ?: images.firstOrNull()
                            val addDate = info?.addDate ?: video.lastModified.takeIf { it > 0 } ?: System.currentTimeMillis()
                            val existing = dao.find(code, quality)
                            if (existing != null) {
                                if (existing.state == DownloadState.Queued || existing.state == DownloadState.Downloading) {
                                    skipped++
                                } else {
                                    dao.update(
                                        existing.copy(
                                            videoUri = video.uriString,
                                            coverUri = cover?.uriString ?: existing.coverUri,
                                            length = video.length,
                                            downloadedLength = video.length,
                                            state = DownloadState.Finished,
                                        )
                                    )
                                    updated++
                                }
                            } else {
                                dao.insert(
                                    HanimeDownloadEntity(
                                        coverUrl = info?.coverUrl.orEmpty(),
                                        coverUri = cover?.uriString,
                                        title = title,
                                        addDate = addDate,
                                        videoCode = code,
                                        videoUri = video.uriString,
                                        quality = quality,
                                        videoUrl = info?.videoUrls?.get(quality).orEmpty(),
                                        length = video.length,
                                        downloadedLength = video.length,
                                        state = DownloadState.Finished,
                                    )
                                )
                                imported++
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failures += ImportFailure(leaf.segments.joinToString("/"), ImportFailureReason.IMPORT_EXCEPTION)
                    Log.e(TAG, "import failed: ${leaf.segments.joinToString("/")}", e)
                } finally {
                    onProgress?.invoke(index + 1, leaves.size, leaf.node.name)
                }
            }
            ImportResult(
                scannedFolders = scanned,
                imported = imported,
                updated = updated,
                skipped = skipped,
                failures = failures,
            )
        }
    }
}
