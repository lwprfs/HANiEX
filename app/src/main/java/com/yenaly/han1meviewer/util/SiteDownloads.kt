package com.yenaly.han1meviewer.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.yenaly.han1meviewer.SitesHub.FapFolder.common.FapFolderConstants
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.remote.FapFolderNetwork
import com.yenaly.han1meviewer.HFileManager
import com.yenaly.han1meviewer.SitesHub.HentaiMama.data.remote.HentaiMamaNetwork
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirCookieManager
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirNetwork
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.logic.network.ServiceCreator
import com.yenaly.han1meviewer.util.offline.OfflineDownloads
import com.yenaly.han1meviewer.util.offline.OfflineFs
import com.yenaly.han1meviewer.util.offline.OfflineKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

object DownloadLayout {

    private const val FALLBACK = "untitled"

    const val GIFS_FOLDER = "Gifs"

    fun sanitize(name: String?, fallback: String = FALLBACK): String {
        val cleaned = HFileManager.illegalCharsRegex.replace(name.orEmpty(), "_")
            .trim().trim('.').replace(Regex("\\s+"), " ")
            .take(80)
        return cleaned.ifBlank { fallback }
    }

    fun siteFolderName(site: SiteType): String? = when (site) {
        SiteType.HANIME, SiteType.JAVCHU -> null
        SiteType.MISSAV -> "MissAV"
        SiteType.HENTAIMAMA -> "HentaiMama"
        SiteType.ONAIR -> "OnAir"
        SiteType.STRAWBERRY -> "Strawberry"
        SiteType.FAPFOLDER -> "FapFolder"
        SiteType.SUPJAV -> "SupJav"
        SiteType.ROUVIDEO -> "RouVideo"
        SiteType.XHAMSTER -> "xHamster"
        SiteType.BUONDUA -> "Buondua"
        SiteType.COSPLAYTELE -> "Cosplaytele"
        SiteType.COSER -> "Coser"
        SiteType.FULLVIDEOS -> "FullVideos"
        SiteType.FUCKINGSESSION -> "FuckingSession"
        SiteType.REDGIFS -> "RedGifs"
        SiteType.XXXTUBE -> "XXXTube"
        SiteType.COOMER -> "Coomer"
        SiteType.LEAKGALLERY -> "LeakGallery"
        SiteType.WUMAOBI -> "Wumaobi"
    }

    const val VIDEOS_FOLDER = "Videos"
    const val PHOTOS_FOLDER = "Photos"
    const val SHORTS_FOLDER = "Shorts"

    val RESERVED_ROOT_FOLDERS = setOf(VIDEOS_FOLDER, PHOTOS_FOLDER, SHORTS_FOLDER)

    fun siteLabel(site: SiteType?): String = site?.let { siteFolderName(it) } ?: "Hanime"

    fun newSegmentsFor(videoCode: String, title: String): List<String> =
        listOf(
            VIDEOS_FOLDER,
            siteLabel(SiteCheckIn.siteOfDishCode(videoCode)),
            sanitize(title),
        )

    fun segmentsFor(videoCode: String): List<String> =
        DownloadFolderRegistry.get(videoCode) ?: legacySegmentsFor(videoCode)

    fun legacySegmentsFor(videoCode: String): List<String> {
        val site = SiteCheckIn.siteOfDishCode(videoCode)
            ?: return listOf(HFileManager.HANIME_DOWNLOAD_FOLDER, videoCode)
        val siteFolder = siteFolderName(site)
            ?: return listOf(HFileManager.HANIME_DOWNLOAD_FOLDER, videoCode)
        val rest = SiteCheckIn.rawCodeOfDishCode(videoCode)
            .split('/')
            .filter { it.isNotBlank() }
            .map { sanitize(it) }
        return listOf(siteFolder) + rest.ifEmpty { listOf(FALLBACK) }
    }

    val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "flv", "mov", "webm", "ts", "m2ts", "mts", "m4v", "mpeg", "mpg")

    fun isVideoFileName(name: String?): Boolean =
        name?.substringAfterLast('.', "")?.lowercase() in VIDEO_EXTENSIONS

    fun videoCodeFromSegments(segments: List<String>): String? {
        val first = segments.firstOrNull() ?: return null
        if (first.equals(VIDEOS_FOLDER, ignoreCase = true)) {
            return DownloadFolderRegistry.codeFor(segments)
        }
        return legacyVideoCodeFromSegments(segments)
    }

    fun legacyVideoCodeFromSegments(segments: List<String>): String? {
        val first = segments.firstOrNull() ?: return null
        if (first.equals(HFileManager.HANIME_DOWNLOAD_FOLDER, ignoreCase = true)) {
            return if (segments.size == 2) segments[1] else null
        }
        val site = SiteType.entries.firstOrNull {
            siteFolderName(it)?.equals(first, ignoreCase = true) == true
        } ?: return null
        val rest = segments.drop(1).filter { it.isNotBlank() }
        if (rest.isEmpty()) return null
        return SiteCheckIn.dishCode(site, rest.joinToString("/"))
    }

    fun fapFolderVideoCode(ownerKind: FapOwnerKind?, ownerName: String?, postId: Long): String {
        val owner = ownerSegments(ownerKind, ownerName)
        return (owner + listOf("Videos", postId.toString())).joinToString("/")
    }

    fun fapFolderPhotoSegments(
        ownerKind: FapOwnerKind?,
        ownerName: String?,
        albumName: String?,
    ): List<String> {
        val base = listOf("FapFolder") + ownerSegments(ownerKind, ownerName) + "Photos"
        return if (albumName.isNullOrBlank()) base else base + sanitize(albumName)
    }

    private fun ownerSegments(kind: FapOwnerKind?, name: String?): List<String> = when {
        kind == null || name.isNullOrBlank() -> listOf("Other")
        kind == FapOwnerKind.GROUP -> listOf("Groups", sanitize(name))
        else -> listOf("Profiles", sanitize(name))
    }
}

object DownloadFolderRegistry {
    private const val PREFS = "video_folder_registry"
    private fun prefs() = com.yenaly.han1meviewer.util.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(videoCode: String): List<String>? =
        prefs().getString(videoCode, null)?.split('/')?.filter { it.isNotEmpty() }?.takeIf { it.size >= 3 }

    fun codeFor(segments: List<String>): String? {
        val path = segments.joinToString("/")
        return prefs().all.entries.firstOrNull { it.value == path }?.key
    }

    fun set(videoCode: String, segments: List<String>) {
        prefs().edit().putString(videoCode, segments.joinToString("/")).apply()
    }

    fun remove(videoCode: String) {
        prefs().edit().remove(videoCode).apply()
    }

    fun ensure(videoCode: String, title: String) {
        if (get(videoCode) != null) return
        val legacy = DownloadLayout.legacySegmentsFor(videoCode)
        if (OfflineFs.find(com.yenaly.han1meviewer.util.applicationContext, legacy) != null) return
        val wanted = DownloadLayout.newSegmentsFor(videoCode, title)
        val taken = prefs().all.filter { it.key != videoCode }.values.toSet()
        var candidate = wanted
        if (candidate.joinToString("/") in taken) {
            val suffix = DownloadLayout.sanitize(SiteCheckIn.rawCodeOfDishCode(videoCode), "x").take(24)
            candidate = wanted.dropLast(1) + (wanted.last() + " [" + suffix + "]")
        }
        set(videoCode, candidate)
    }
}

enum class FapOwnerKind { GROUP, PROFILE }

object FapFolderImageLocation {
    private const val KEY = "fapfolder_image_download_uri"

    @Volatile
    var confirmedThisSession = false

    var uri: String?
        get() = Preferences.preferenceSp.getString(KEY, null)?.takeIf { it.isNotBlank() }
        set(value) {
            Preferences.preferenceSp.edit().apply {
                if (value.isNullOrBlank()) remove(KEY) else putString(KEY, value)
            }.apply()
        }

    fun isReady(context: Context): Boolean {
        val tree = uri?.let { runCatching { it.toUri() }.getOrNull() } ?: return false
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == tree && it.isWritePermission
        }
    }

    fun persist(context: Context, tree: android.net.Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                tree,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        uri = tree.toString()
    }

    fun displayName(context: Context): String {
        val tree = uri?.let { runCatching { it.toUri() }.getOrNull() } ?: return "Not set"
        return runCatching { DocumentFile.fromTreeUri(context, tree)?.name }.getOrNull()
            ?: tree.lastPathSegment
            ?: tree.toString()
    }
}

enum class DownloadFailureKind { TOR, COOKIES, NETWORK, STORAGE, EXPIRED, OTHER }

data class DownloadFailure(val kind: DownloadFailureKind, val message: String)

object DownloadFailureStore {
    private const val PREFS = "download_failures"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun put(context: Context, videoCode: String, failure: DownloadFailure) {
        prefs(context).edit()
            .putString(videoCode, failure.kind.name + "\u001E" + failure.message)
            .apply()
    }

    fun get(context: Context, videoCode: String): DownloadFailure? {
        val raw = prefs(context).getString(videoCode, null) ?: return null
        val kind = runCatching {
            DownloadFailureKind.valueOf(raw.substringBefore('\u001E'))
        }.getOrDefault(DownloadFailureKind.OTHER)
        return DownloadFailure(kind, raw.substringAfter('\u001E', ""))
    }

    fun clear(context: Context, videoCode: String) {
        prefs(context).edit().remove(videoCode).apply()
    }
}

object SiteDownloadClients {

    fun clientFor(videoCode: String): OkHttpClient =
        when (SiteCheckIn.siteOfDishCode(videoCode)) {
            SiteType.ONAIR -> runCatching { OnAirNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.STRAWBERRY -> runCatching { com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.HENTAIMAMA -> runCatching { HentaiMamaNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.FAPFOLDER -> runCatching { FapFolderNetwork.playbackOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.MISSAV -> runCatching { com.yenaly.han1meviewer.SitesHub.MissAV.data.remote.MissAvNetwork.downloadOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.SUPJAV -> runCatching { com.yenaly.han1meviewer.SitesHub.SupJav.data.remote.SupJavNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)

            SiteType.ROUVIDEO -> runCatching { com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoNetwork.streamClient }
                .getOrDefault(ServiceCreator.downloadClient)

            SiteType.XHAMSTER -> runCatching { com.yenaly.han1meviewer.SitesHub.XHamster.data.remote.XHamsterNetwork.streamClient }
                .getOrDefault(ServiceCreator.downloadClient)

            SiteType.BUONDUA -> runCatching { com.yenaly.han1meviewer.SitesHub.Buondua.data.remote.BuonduaNetwork.streamClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.COSPLAYTELE -> runCatching { com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.remote.CosplayteleNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.COOMER -> runCatching { com.yenaly.han1meviewer.SitesHub.Coomer.data.remote.CoomerNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.LEAKGALLERY -> runCatching { com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.COSER -> runCatching { com.yenaly.han1meviewer.SitesHub.Coser.data.remote.CoserNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.FULLVIDEOS -> runCatching { com.yenaly.han1meviewer.SitesHub.FullVideos.data.remote.FullVideosNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.FUCKINGSESSION -> runCatching { com.yenaly.han1meviewer.SitesHub.FuckingSession.data.remote.FuckingSessionNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.WUMAOBI -> runCatching { com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            SiteType.XXXTUBE -> runCatching { com.yenaly.han1meviewer.SitesHub.XXXTube.data.remote.XXXTubeNetwork.sharedOkHttpClient }
                .getOrDefault(ServiceCreator.downloadClient)
            else -> ServiceCreator.downloadClient
        }

    fun useUrlConnectionFallback(videoCode: String): Boolean =
        SiteCheckIn.siteOfDishCode(videoCode) == SiteType.MISSAV

    fun headersFor(videoCode: String, url: String): Map<String, String> {
        val host = runCatching { java.net.URI(url).host }.getOrNull().orEmpty()
        return when (SiteCheckIn.siteOfDishCode(videoCode)) {
            SiteType.MISSAV -> {
                val base = Preferences.missAvBaseUrl.trimEnd('/')
                mapOf(
                    "Referer" to "$base/",
                    "Origin" to base,
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                )
            }
            SiteType.HENTAIMAMA -> {
                val base = runCatching { HentaiMamaNetwork.baseUrl.trimEnd('/') }.getOrDefault("")
                if (base.isBlank()) emptyMap() else mapOf("Referer" to "$base/", "Origin" to base)
            }
            SiteType.ONAIR -> {
                val base = runCatching { OnAirNetwork.baseUrl.trimEnd('/') }.getOrDefault("")
                buildMap {
                    if (base.isNotBlank()) {
                        put("Referer", "$base/")
                        put("Origin", base)
                        val baseHost = runCatching { java.net.URI(base).host }.getOrNull()
                        val cookie = runCatching { OnAirCookieManager.cookiesRaw }.getOrDefault("")
                        if (cookie.isNotBlank() && baseHost != null && baseHost == host) {
                            put("Cookie", cookie)
                        }
                    }
                }
            }
            SiteType.STRAWBERRY -> {
                val base = runCatching { com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork.baseUrl.trimEnd('/') }.getOrDefault("")
                if (base.isBlank()) emptyMap() else mapOf("Referer" to "$base/", "Origin" to base)
            }
            SiteType.FULLVIDEOS -> {
                val referer = com.yenaly.han1meviewer.SitesHub.FullVideos.data.remote.FullVideosNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf("Referer" to referer, "Origin" to referer.trimEnd('/'))
            }
            SiteType.WUMAOBI -> {
                val referer = com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf("Referer" to referer, "Origin" to referer.trimEnd('/'))
            }
            SiteType.FUCKINGSESSION -> {
                val referer = com.yenaly.han1meviewer.SitesHub.FuckingSession.data.remote.FuckingSessionNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf("Referer" to referer)
            }
            SiteType.XXXTUBE -> {
                val referer = com.yenaly.han1meviewer.SitesHub.XXXTube.data.remote.XXXTubeNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf("Referer" to referer)
            }
            SiteType.SUPJAV -> {
                val referer = com.yenaly.han1meviewer.SitesHub.SupJav.data.remote.SupJavNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf("Referer" to referer, "Origin" to referer.trimEnd('/'))
            }
            SiteType.ROUVIDEO -> mapOf(
                "Referer" to com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoConstants.REFERER,
                "Origin" to com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoConstants.ORIGIN,
            )
            SiteType.XHAMSTER -> mapOf(
                "Referer" to com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterConstants.REFERER,
                "Origin" to com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterConstants.ORIGIN,
            )
            SiteType.BUONDUA -> runCatching {
                com.yenaly.han1meviewer.SitesHub.Buondua.data.remote.BuonduaNetwork.mediaHeaders()
            }.getOrDefault(emptyMap())
            SiteType.COSPLAYTELE -> {
                val referer = com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.remote.CosplayteleNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf(
                    "Referer" to referer,
                    "Origin" to com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleConstants.STREAM_ORIGIN,
                )
            }
            SiteType.COOMER -> {
                val referer = com.yenaly.han1meviewer.SitesHub.Coomer.data.remote.CoomerNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf(
                    "Referer" to referer,
                    "Origin" to com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerConstants.BASE_URL,
                )
            }
            SiteType.LEAKGALLERY -> {
                val referer = com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf(
                    "Referer" to referer,
                    "Origin" to com.yenaly.han1meviewer.SitesHub.LeakGallery.common.LeakGalleryConstants.BASE_URL,
                )
            }
            SiteType.COSER -> {
                val referer = com.yenaly.han1meviewer.SitesHub.Coser.data.remote.CoserNetwork
                    .streamRefererFor(SiteCheckIn.rawCodeOfDishCode(videoCode))
                mapOf(
                    "Referer" to referer,
                    "Origin" to com.yenaly.han1meviewer.SitesHub.Coser.common.CoserConstants.STREAM_ORIGIN,
                )
            }
            SiteType.FAPFOLDER -> mapOf(
                "Referer" to FapFolderConstants.HEADER_REFERER,
                "Origin" to FapFolderConstants.HEADER_ORIGIN,
                "User-Agent" to FapFolderConstants.USER_AGENT_FAPFOLDER,
            )
            else -> emptyMap()
        }
    }

    fun Request.Builder.applySiteHeaders(videoCode: String, url: String): Request.Builder {
        headersFor(videoCode, url).forEach { (k, v) -> header(k, v) }
        return this
    }
}

object SiteImageSaver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isDownloadPermissionGranted(context: Context): Boolean = OfflineFs.isReady(context)

    sealed interface SaveResult {
        data class Saved(val count: Int, val skipped: Int, val failed: Int) : SaveResult
        data class Error(val message: String) : SaveResult
    }

    private fun toResult(o: OfflineDownloads.Outcome): SaveResult =
        if (o.saved == 0 && o.skipped == 0 && o.failed > 0) {
            SaveResult.Error(o.error ?: "Download failed.")
        } else {
            SaveResult.Saved(o.saved, o.skipped, o.failed)
        }

    suspend fun saveOne(
        context: Context,
        url: String,
        segments: List<String>,
        baseName: String,
        client: OkHttpClient = FapFolderNetwork.sharedOkHttpClient,
        headers: Map<String, String> = fapHeaders(),
        title: String? = null,
        sourceUrl: String? = null,
    ): SaveResult = saveBatch(
        context, listOf(url), segments, baseName, client, headers,
        onProgress = null, title = title, sourceUrl = sourceUrl,
    )

    suspend fun saveBatch(
        context: Context,
        urls: List<String>,
        segments: List<String>,
        baseName: String,
        client: OkHttpClient = FapFolderNetwork.sharedOkHttpClient,
        headers: Map<String, String> = fapHeaders(),
        onProgress: ((done: Int, total: Int) -> Unit)?,
        title: String? = null,
        sourceUrl: String? = null,
    ): SaveResult {
        if (!isDownloadPermissionGranted(context)) {
            return SaveResult.Error("Choose a download folder first.")
        }
        val layout = OfflineDownloads.layoutFor(segments, baseName)
        val outcome = OfflineDownloads.enqueue(
            OfflineDownloads.Request(
                kind = OfflineKind.PHOTOS,
                siteFolder = layout.siteFolder,
                folder = layout.folder,
                title = title ?: layout.folder,
                channel = layout.channel,
                urls = urls,
                baseName = baseName,
                sourceUrl = sourceUrl,
            ),
            client, headers,
        ).await()
        onProgress?.invoke(urls.size, urls.size)
        return toResult(outcome)
    }

    fun saveBatchInBackground(
        context: Context,
        urls: List<String>,
        segments: List<String>,
        baseName: String,
        client: OkHttpClient = FapFolderNetwork.sharedOkHttpClient,
        headers: Map<String, String> = fapHeaders(),
        onResult: (SaveResult) -> Unit,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            val result = saveBatch(
                appContext, urls, segments, baseName, client, headers, onProgress = null,
            )
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    suspend fun saveGif(
        context: Context,
        site: SiteType,
        url: String,
        creator: String?,
        baseName: String,
        client: OkHttpClient,
        headers: Map<String, String>,
        title: String? = null,
        coverUrl: String? = null,
        pageUrl: String? = null,
        views: Long? = null,
        durationSeconds: Int? = null,
        tags: List<String> = emptyList(),
    ): Result<Unit> {
        if (url.substringBefore('?').lowercase().endsWith(".m3u8")) {
            return Result.failure(IOException("Streaming (HLS) clips can't be saved yet."))
        }
        if (!isDownloadPermissionGranted(context)) {
            return Result.failure(IOException("Choose a download folder first."))
        }
        val folder = DownloadLayout.sanitize(creator, "Unknown")
        val outcome = OfflineDownloads.enqueue(
            OfflineDownloads.Request(
                kind = OfflineKind.SHORTS,
                siteFolder = DownloadLayout.siteFolderName(site) ?: "Hanime",
                folder = folder,
                title = title?.takeIf { it.isNotBlank() } ?: baseName,
                channel = creator?.takeIf { it.isNotBlank() },
                urls = listOf(url),
                baseName = baseName,
                coverUrl = coverUrl,
                sourceUrl = pageUrl,
                views = views,
                durationSeconds = durationSeconds,
                tags = tags,
            ),
            client, headers,
        ).await()
        return when {
            outcome.saved > 0 -> Result.success(Unit)
            outcome.skipped > 0 -> Result.failure(IOException("Already downloaded."))
            outcome.paused -> Result.failure(IOException("Paused."))
            else -> Result.failure(IOException(outcome.error ?: "Download failed."))
        }
    }

    private fun fapHeaders(): Map<String, String> = mapOf(
        "Referer" to FapFolderConstants.HEADER_REFERER,
        "User-Agent" to FapFolderConstants.USER_AGENT_FAPFOLDER,
    )
}
