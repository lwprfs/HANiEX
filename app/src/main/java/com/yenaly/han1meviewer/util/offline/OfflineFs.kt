package com.yenaly.han1meviewer.util.offline

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.yenaly.han1meviewer.HFileManager
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.DownloadTreeNode
import com.yenaly.han1meviewer.util.FileTreeNode
import com.yenaly.han1meviewer.util.SafTreeNode
import java.io.File
import java.io.OutputStream

/**
 * File access for photos / shorts under the same download path the video downloader uses
 * (app-private folder, or the folder chosen with the system picker).
 *
 * Layout (relative to the download path):
 *  - Videos/<Site>/<title>/<videos, cover jpg/png, info.json, .nomedia>
 *  - Photos/<Site>/<channel or title>/<photos, info.json, .nomedia>
 *  - Shorts/<Site>/<channel or title>/<clips, thumbnails, one json per clip, .nomedia>
 */
object OfflineFs {

    private const val TAG = "OfflineFs"
    const val NO_MEDIA = ".nomedia"
    private val lock = Any()

    fun isReady(context: Context): Boolean {
        if (Preferences.isUsePrivateStorage) return true
        val raw = Preferences.safDownloadPath?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            val tree = raw.toUri()
            context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission }
        }.getOrDefault(false)
    }

    private fun treeUri(): Uri? =
        Preferences.safDownloadPath?.takeIf { it.isNotBlank() }?.let { runCatching { it.toUri() }.getOrNull() }

    private fun fileRoot(context: Context): File =
        HFileManager.getAppDownloadFolder(context).also { it.mkdirs() }

    fun root(context: Context): DownloadTreeNode? {
        if (Preferences.isUsePrivateStorage) return FileTreeNode(fileRoot(context))
        val uri = treeUri() ?: return null
        return SafTreeNode.root(context, uri)
    }

    /** Walks [segments] down from the download path; null when any folder is missing. */
    fun find(context: Context, segments: List<String>): DownloadTreeNode? {
        var node = root(context) ?: return null
        for (segment in segments) {
            node = node.children().firstOrNull { it.isDirectory && it.name == segment } ?: return null
        }
        return node
    }

    private fun safRoot(context: Context): DocumentFile? {
        val uri = treeUri() ?: return null
        return DocumentFile.fromTreeUri(context, uri)?.takeIf { it.isDirectory }
    }

    private fun DocumentFile.childDir(name: String): DocumentFile? {
        findFile(name)?.let { return if (it.isDirectory) it else null }
        return createDirectory(name)
    }

    private fun DocumentFile.ensureNoMedia() {
        runCatching {
            if (findFile(NO_MEDIA) == null) {
                val created = createFile("application/octet-stream", NO_MEDIA)
                if (created != null && created.name != NO_MEDIA) created.delete()
            }
        }.onFailure { Log.w(TAG, "nomedia failed: ${it.message}") }
    }

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "ts" -> "video/mp2t"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "json" -> "application/json"
        else -> "application/octet-stream"
    }

    /** Creates the folder chain and puts a `.nomedia` in the last folder (and the first one). */
    fun ensureDir(context: Context, segments: List<String>): Boolean = synchronized(lock) {
        runCatching {
            if (Preferences.isUsePrivateStorage) {
                var dir = fileRoot(context)
                segments.forEachIndexed { i, s ->
                    dir = File(dir, s)
                    dir.mkdirs()
                    if (i == 0 || i == segments.lastIndex) File(dir, NO_MEDIA).createNewFile()
                }
                dir.isDirectory
            } else {
                var dir: DocumentFile = safRoot(context) ?: return@runCatching false
                segments.forEachIndexed { i, s ->
                    dir = dir.childDir(s) ?: return@runCatching false
                    if (i == 0 || i == segments.lastIndex) dir.ensureNoMedia()
                }
                true
            }
        }.getOrElse {
            Log.w(TAG, "ensureDir failed", it)
            false
        }
    }

    /** Opens (creating) a file for writing. Returns the stream and the file's uri string. */
    fun openOutput(
        context: Context,
        segments: List<String>,
        name: String,
        overwrite: Boolean = true,
    ): Pair<OutputStream, String>? = synchronized(lock) {
        runCatching {
            if (!ensureDir(context, segments)) return@runCatching null
            if (Preferences.isUsePrivateStorage) {
                var dir = fileRoot(context)
                segments.forEach { dir = File(dir, it) }
                val file = File(dir, name)
                if (file.exists() && !overwrite) return@runCatching null
                file.outputStream() to Uri.fromFile(file).toString()
            } else {
                var dir: DocumentFile = safRoot(context) ?: return@runCatching null
                for (s in segments) dir = dir.childDir(s) ?: return@runCatching null
                val existing = dir.findFile(name)
                if (existing != null && !overwrite) return@runCatching null
                val target = existing ?: dir.createFile(mimeFor(name), name) ?: return@runCatching null
                val out = context.contentResolver.openOutputStream(target.uri, "wt")
                    ?: return@runCatching null
                out to target.uri.toString()
            }
        }.getOrElse {
            Log.w(TAG, "openOutput failed: $name", it)
            null
        }
    }

    fun writeText(context: Context, segments: List<String>, name: String, text: String): String? {
        val (out, uri) = openOutput(context, segments, name, overwrite = true) ?: return null
        return runCatching {
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            uri
        }.getOrNull()
    }

    fun readText(node: DownloadTreeNode): String? =
        runCatching { node.openInput()?.use { it.bufferedReader().readText() } }.getOrNull()

    fun delete(context: Context, uriString: String): Boolean {
        return runCatching {
            val uri = uriString.toUri()
            if (uri.scheme == "file") {
                val file = File(requireNotNull(uri.path))
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            } else {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            }
        }.getOrElse {
            Log.w(TAG, "delete failed: $uriString", it)
            false
        }
    }

    fun deleteDir(context: Context, segments: List<String>): Boolean {
        val node = find(context, segments) ?: return true
        return delete(context, node.uriString)
    }

    /** Deletes [dir]'s folder when it no longer holds anything but `.nomedia`. */
    fun deleteIfEmpty(context: Context, segments: List<String>) {
        val node = find(context, segments) ?: return
        val rest = node.children().filterNot { it.name == NO_MEDIA }
        if (rest.isEmpty()) delete(context, node.uriString)
    }

    /**
     * Moves one file inside the download path. Fast rename when possible, copy + delete as the
     * fallback. Returns the new uri string, or null when it failed.
     */
    fun moveFile(
        context: Context,
        srcSegments: List<String>,
        name: String,
        dstSegments: List<String>,
        dstName: String = name,
    ): String? = synchronized(lock) {
        runCatching {
            if (!ensureDir(context, dstSegments)) return@runCatching null
            if (Preferences.isUsePrivateStorage) {
                var src = fileRoot(context)
                srcSegments.forEach { src = File(src, it) }
                var dst = fileRoot(context)
                dstSegments.forEach { dst = File(dst, it) }
                val from = File(src, name)
                val to = File(dst, dstName)
                if (!from.exists()) return@runCatching null
                if (to.exists()) {
                    if (to.length() > 0) { from.delete(); return@runCatching Uri.fromFile(to).toString() }
                    to.delete()
                }
                if (from.renameTo(to)) return@runCatching Uri.fromFile(to).toString()
                from.copyTo(to, overwrite = true)
                from.delete()
                Uri.fromFile(to).toString()
            } else {
                val root = safRoot(context) ?: return@runCatching null
                var srcDir: DocumentFile = root
                for (s in srcSegments) srcDir = srcDir.findFile(s)?.takeIf { it.isDirectory } ?: return@runCatching null
                var dstDir: DocumentFile = root
                for (s in dstSegments) dstDir = dstDir.childDir(s) ?: return@runCatching null
                val srcDoc = srcDir.findFile(name) ?: return@runCatching null
                val existing = dstDir.findFile(dstName)
                if (existing != null) {
                    if (existing.length() > 0) { srcDoc.delete(); return@runCatching existing.uri.toString() }
                    existing.delete()
                }
                if (dstName == name) {
                    val moved = runCatching {
                        DocumentsContract.moveDocument(context.contentResolver, srcDoc.uri, srcDir.uri, dstDir.uri)
                    }.getOrNull()
                    if (moved != null) return@runCatching moved.toString()
                }
                val created = dstDir.createFile(mimeFor(dstName), dstName) ?: return@runCatching null
                val ok = context.contentResolver.openInputStream(srcDoc.uri)?.use { input ->
                    context.contentResolver.openOutputStream(created.uri, "wt")?.use { output ->
                        input.copyTo(output); true
                    }
                } == true
                if (!ok) { created.delete(); return@runCatching null }
                srcDoc.delete()
                created.uri.toString()
            }
        }.getOrElse {
            Log.w(TAG, "moveFile failed: $name", it)
            null
        }
    }

    /** Copies [node] (possibly from another tree) into the download path and deletes the source. */
    fun importNode(
        context: Context,
        node: DownloadTreeNode,
        dstSegments: List<String>,
        dstName: String = node.name,
    ): String? {
        val input = node.openInput() ?: return null
        val (out, uri) = openOutput(context, dstSegments, dstName, overwrite = true)
            ?: run { input.close(); return null }
        val ok = runCatching {
            input.use { i -> out.use { o -> i.copyTo(o) } }
            true
        }.getOrDefault(false)
        if (!ok) {
            delete(context, uri)
            return null
        }
        delete(context, node.uriString)
        return uri
    }
}
