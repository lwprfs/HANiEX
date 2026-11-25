package com.yenaly.han1meviewer.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.yenaly.han1meviewer.APP_NAME
import com.yenaly.han1meviewer.HFileManager.DEF_VIDEO_COVER_TYPE
import com.yenaly.han1meviewer.HFileManager.HANIME_DOWNLOAD_FOLDER
import com.yenaly.han1meviewer.HFileManager.createVideoCoverName
import com.yenaly.han1meviewer.HFileManager.getAppDownloadFolder
import com.yenaly.han1meviewer.HFileManager.getDownloadVideoCoverFile
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.dao.download.HanimeDownloadDao
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.state.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object SafFileManager {

    const val KEY_TREE_URI = "saf_download_path"
    private const val TAG = "SafFileManager"
    private const val NO_MEDIA_NAME = ".nomedia"
    private const val INFO_JSON_NAME = "info.json"
    private const val NO_MEDIA_MIME = "application/octet-stream"
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    private val noMediaDupRegex = Regex("""^\s*\(\d+\)\.nomedia$|^\.nomedia\s*\(\d+\)$""")

    private val safFolderLock = Any()

    private fun isSafReady(): Boolean = !Preferences.safDownloadPath.isNullOrBlank()
    fun buildOpenDirectoryIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra("android.provider.extra.SHOW_ADVANCED", true)
        }
    }

    fun persistUriPermission(context: Context, data: Intent?) {
        val treeUri = data?.data ?: return
        val contentResolver = context.contentResolver
        val flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        contentResolver.takePersistableUriPermission(treeUri, flags)
        Preferences.preferenceSp.edit {
            putString(KEY_TREE_URI, treeUri.toString())
        }
    }

    fun getSavedUri(): Uri? {
        val uriStr = Preferences.safDownloadPath
        return uriStr?.toUri()
    }

    fun getAppDownloadFolderSaf(context: Context): DocumentFile? {
        if (!isSafReady()) return null
        val uri = runCatching { Preferences.safDownloadPath?.toUri() }.getOrNull() ?: return null
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
        if (!tree.isDirectory) return null
        return tree
    }

    fun getDownloadVideoFolderSaf(context: Context, videoCode: String): DocumentFile? {

        synchronized(safFolderLock) {
            val root = getAppDownloadFolderSaf(context) ?: return null
            var dir: DocumentFile = root
            var first = true
            for (segment in DownloadLayout.segmentsFor(videoCode)) {
                dir = dir.ensureChildDir(segment) ?: return null
                if (first) {
                    dir.ensureNoMedia()
                    first = false
                }
            }
            dir.ensureNoMedia()
            return dir
        }
    }

    fun getDownloadVideoCoverDoc(
        context: Context,
        videoCode: String,
        title: String,
        suffix: String = DEF_VIDEO_COVER_TYPE
    ): DocumentFile? {
        val dir = getDownloadVideoFolderSaf(context, videoCode) ?: return null
        val name = createVideoCoverName(title, suffix)
        dir.findFile(name)?.let { return it }
        return dir.createFile(mimeForExt(suffix), name)
    }

    fun openOutputStreamForCover(
        context: Context,
        videoCode: String,
        title: String,
        suffix: String = DEF_VIDEO_COVER_TYPE
    ): Pair<OutputStream?, Uri?> {
        return if (isSafReady()) {
            val doc = getDownloadVideoCoverDoc(context, videoCode, title, suffix) ?: return Pair(
                null,
                null
            )
            val os = context.contentResolver.openOutputStream(doc.uri, "rwt")
            Pair(os, doc.uri)
        } else {
            val file = getDownloadVideoCoverFile(context, videoCode, title, suffix)
            file.parentFile?.mkdirs()
            Pair(FileOutputStream(file), file.toUri())
        }
    }

    fun writeInfoJson(context: Context, videoCode: String, content: String): Boolean {
        return try {
            if (isSafReady()) {
                val dir = getDownloadVideoFolderSaf(context, videoCode) ?: return false
                val doc = dir.findFile(INFO_JSON_NAME)
                    ?: dir.createFile("application/json", INFO_JSON_NAME) ?: return false
                context.contentResolver.openOutputStream(doc.uri, "rwt")
                    ?.use { it.write(content.toByteArray(Charsets.UTF_8)) } != null
            } else {
                val file = File(com.yenaly.han1meviewer.HFileManager.getDownloadVideoFolder(context, videoCode), INFO_JSON_NAME)
                file.parentFile?.mkdirs()
                file.writeText(content, Charsets.UTF_8)
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "write info.json failed: ${videoCode.logId()}", e)
            false
        }
    }

    private fun DocumentFile.ensureChildDir(name: String): DocumentFile? {
        findFile(name)?.let { return if (it.isDirectory) it else null }
        return createDirectory(name)
    }

    private fun DocumentFile.ensureNoMedia() {
        synchronized(safFolderLock) {
            runCatching {
                val children = listFiles()

                children.forEach { child ->
                    val name = child.name ?: return@forEach
                    if (name != NO_MEDIA_NAME && child.isFile && noMediaDupRegex.matches(name)) {
                        child.delete()
                    }
                }
                if (children.none { it.name == NO_MEDIA_NAME }) {
                    val created = createFile(NO_MEDIA_MIME, NO_MEDIA_NAME)
                    if (created != null && created.name != NO_MEDIA_NAME) {

                        created.delete()
                    }
                }
            }.onFailure { Log.w(TAG, "⚠️ ensureNoMedia 失败: ${it.message}") }
        }
    }

    fun getDownloadVideoFileUri(
        context: Context,
        videoCode: String,
        fileName: String
    ): Uri? {
        if (!isSafReady()) return null
        val videoDir = getDownloadVideoFolderSaf(context, videoCode) ?: return null
        findVideoDoc(videoDir, fileName)?.let { return it.uri }
        val ext = fileName.substringAfterLast('.', "")
        val target = videoDir.createFile(mimeForExt(ext), fileName) ?: return null

        if (target.name != fileName) runCatching { target.renameTo(fileName) }
        return (DocumentFile.fromSingleUri(context, target.uri) ?: target).uri
    }

    fun findVideoDoc(dir: DocumentFile, fileName: String): DocumentFile? {
        dir.findFile(fileName)?.takeIf { it.isFile }?.let { return it }
        val prefix = "$fileName."
        return runCatching {
            dir.listFiles()
                .filter { doc ->
                    val n = doc.name ?: return@filter false
                    doc.isFile && n.startsWith(prefix) && n.substring(prefix.length).none { it == '.' || it == ' ' }
                }
                .maxByOrNull { it.length() }
        }.getOrNull()
    }

    fun listVideoFiles(context: Context, videoCode: String): List<Triple<String, String, Long>> {
        return runCatching {
            if (isSafReady()) {
                val dir = getDownloadVideoFolderSaf(context, videoCode) ?: return emptyList()
                dir.listFiles()
                    .filter { it.isFile && DownloadLayout.isVideoFileName(it.name) && it.length() > 0 }
                    .map { Triple(it.uri.toString(), it.name.orEmpty(), it.length()) }
            } else {
                val dir = com.yenaly.han1meviewer.HFileManager.getDownloadVideoFolder(context, videoCode)
                dir.listFiles().orEmpty()
                    .filter { it.isFile && DownloadLayout.isVideoFileName(it.name) && it.length() > 0 }
                    .map { Triple(Uri.fromFile(it).toString(), it.name, it.length()) }
            }.sortedByDescending { it.third }
        }.getOrDefault(emptyList())
    }

    fun isUsableVideoUri(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return false
        return runCatching {
            if (uri.scheme == "content") {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize > 0 } == true
            } else {
                val f = File(uri.path ?: return false)
                f.isFile && f.length() > 0
            }
        }.getOrDefault(false)
    }

    private fun mimeForExt(ext: String): String = when (ext.lowercase()) {
        "mp4" -> "video/mp4"

        "ts", "m2ts", "mts" -> "application/octet-stream"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "json" -> "application/json"
        else -> "application/octet-stream"
    }

    fun migratePrivateToSaf(
        context: Context,
        dao: HanimeDownloadDao? = null,
        onProgress: ((migrated: Int, total: Int) -> Unit)? = null
    ) = CoroutineScope(Dispatchers.IO).launch {
        val privateFolder =
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APP_NAME)
        val topDirs = privateFolder.listFiles { f -> f.isDirectory && !f.name.startsWith(".") }
            ?.toList().orEmpty()

        if (topDirs.isEmpty()) {
            Log.e("Migrate", "private download folder is empty or missing")
            withContext(Dispatchers.Main) { onProgress?.invoke(0, 0) }
            return@launch
        }

        val treeUri = Preferences.safDownloadPath?.toUri()
        val rootDocFile = treeUri?.let { DocumentFile.fromTreeUri(context, it) }
        if (treeUri == null || rootDocFile == null) {
            Log.e("Migrate", "SAF root unavailable")
            withContext(Dispatchers.Main) { onProgress?.invoke(0, -1) }
            return@launch
        }

        val leafDirs = ArrayList<Pair<File, List<String>>>()
        fun collect(dir: File, segments: List<String>) {
            val kids = dir.listFiles().orEmpty()
            if (kids.any { it.isFile }) leafDirs += dir to segments
            kids.filter { it.isDirectory && !it.name.startsWith(".") }
                .forEach { collect(it, segments + it.name) }
        }
        topDirs.forEach { collect(it, listOf(it.name)) }

        val total = leafDirs.size
        var migrated = 0
        Log.d("Migrate", "start, folders: $total")

        for ((dir, segments) in leafDirs) {
            var target: DocumentFile? = rootDocFile
            for ((i, segment) in segments.withIndex()) {
                target = target?.ensureChildDir(segment)
                if (i == 0) target?.ensureNoMedia()
            }
            if (target == null) {
                Log.e("Migrate", "cannot create folder: ${segments.joinToString("/").logId()}")
                continue
            }
            target.ensureNoMedia()
            val destination: DocumentFile = target

            dir.listFiles()?.filter { it.isFile }?.forEach { file ->
                if (file.name.startsWith(NO_MEDIA_NAME)) {
                    file.delete()
                    return@forEach
                }
                try {
                    destination.findFile(file.name)?.delete()
                    val newFile = destination.createFile(mimeForExt(file.extension.lowercase()), file.name)
                    if (newFile == null) {
                        Log.e("Migrate", "create file failed: ${file.name.logId()}")
                        return@forEach
                    }
                    val copied = file.inputStream().use { input ->
                        val output = context.contentResolver.openOutputStream(newFile.uri)
                        if (output == null) false else {
                            output.use { input.copyTo(it) }
                            true
                        }
                    }
                    if (copied) file.delete()
                } catch (e: Exception) {
                    Log.e("Migrate", "copy failed: ${file.name.logId()}", e)
                }
            }

            migrated++
            withContext(Dispatchers.Main) { onProgress?.invoke(migrated, total) }
        }

        fun deleteEmpty(dir: File) {
            dir.listFiles()?.filter { it.isDirectory }?.forEach { deleteEmpty(it) }
            if (dir.listFiles().isNullOrEmpty()) dir.delete()
        }
        topDirs.forEach { deleteEmpty(it) }

        try {
            if (dao != null) DownloadScanner.scan(dao, SafTreeNode.root(context, treeUri))
        } catch (e: Exception) {
            Log.e("saf", e.message.toString())
        }
        withContext(Dispatchers.Main) { onProgress?.invoke(migrated, total) }
    }

    fun hasReadAccess(context: Context): Boolean {
        val treeUri = Preferences.safDownloadPath?.toUri() ?: return false
        val granted = context.contentResolver.persistedUriPermissions
            .any { it.uri == treeUri && it.isReadPermission }
        return granted && DocumentFile.fromTreeUri(context, treeUri)?.canRead() == true
    }

    private fun scanRoot(context: Context): Pair<DownloadTreeNode?, ImportResult.Status> {
        if (Preferences.isUsePrivateStorage) {
            val dir = com.yenaly.han1meviewer.HFileManager.getAppDownloadFolder(context)
            return if (dir.exists() && dir.isDirectory) FileTreeNode(dir) to ImportResult.Status.OK
            else null to ImportResult.Status.ROOT_MISSING
        }
        val treeUri = Preferences.safDownloadPath?.toUri() ?: return null to ImportResult.Status.ROOT_MISSING
        if (!hasReadAccess(context)) return null to ImportResult.Status.NO_PERMISSION
        return SafTreeNode.root(context, treeUri) to ImportResult.Status.OK
    }

    suspend fun scanAndImportHanimeDownloads(
        context: Context,
        dao: HanimeDownloadDao,
        onProgress: ((imported: Int, total: Int, currentName: String?) -> Unit)? = null,
    ): ImportResult {
        val (root, status) = scanRoot(context)
        if (root == null) return ImportResult(status = status)
        return try {
            DownloadScanner.scan(dao, root, onProgress)
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "scan cancelled")
            ImportResult(status = ImportResult.Status.CANCELLED)
        }
    }

    fun isValidHanimeFolder(
        folder: File,
        updateMetadata: (videoCode: String) -> Unit
    ): Boolean {
        if (!folder.exists() || !folder.isDirectory) return false
        val hasVideo = folder.listFiles()?.any { file ->
            file.isFile && DownloadLayout.VIDEO_EXTENSIONS.contains(file.extension.lowercase())
        } ?: false

        if (!hasVideo) return false

        val videoCode = folder.name
        updateMetadata(videoCode)
        return true
    }

    fun updateMetadata(
        context: Context,
        videoCode: String,
        uri: Uri
    ): Boolean {

        return true
    }

    fun checkSafPermissions(context: Context): Boolean {
        val treeUri = Preferences.safDownloadPath?.toUri() ?: return false
        val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        return try {
            val testFile = docTree.createFile("text/plain", ".test_permission")
            val result = testFile != null
            testFile?.delete()
            result
        } catch (e: Exception) {
            Log.w("HFileMigrator", "SAF 权限检查失败", e)
            false
        }
    }

    fun deleteDownloadVideoFolder(context: Context, videoCode: String) {

        val segments = DownloadLayout.segmentsFor(videoCode)

        runCatching {
            val folder = com.yenaly.han1meviewer.HFileManager.getDownloadVideoFolder(context, videoCode)
            if (folder.exists()) folder.deleteRecursively()
        }.onFailure { Log.w("SafFileManager", "delete local folder failed", it) }

        if (Preferences.isUsePrivateStorage) return
        runCatching {
            val treeUri = Preferences.safDownloadPath?.toUri() ?: return
            var dir: DocumentFile = DocumentFile.fromTreeUri(context, treeUri) ?: return
            for (segment in segments) {
                dir = dir.findFile(segment)?.takeIf { it.isDirectory } ?: return
            }
            dir.delete()
        }.onFailure { Log.w("SafFileManager", "delete SAF folder failed", it) }
    }

    fun deleteVideoFolderAndForget(context: Context, videoCode: String) {
        deleteDownloadVideoFolder(context, videoCode)
        DownloadFolderRegistry.remove(videoCode)
    }
}
