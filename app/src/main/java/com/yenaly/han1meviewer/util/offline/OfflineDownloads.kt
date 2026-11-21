package com.yenaly.han1meviewer.util.offline

import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.job

/**
 * Downloader for photos and shorts / gifs (the video downloader has its own, Room-based one).
 *
 * Files go to `Photos/<Site>/<channel or title>/` or `Shorts/<Site>/<channel or title>/` inside the
 * download path, each folder with a `.nomedia` and json metadata.
 */
object OfflineDownloads {

    enum class State { QUEUED, DOWNLOADING, PAUSED, FAILED, DONE }

    data class Request(
        val kind: OfflineKind,
        val siteFolder: String,
        /** Folder name: channel name or title. Already sanitized. */
        val folder: String,
        val title: String,
        val channel: String? = null,
        val urls: List<String>,
        val baseName: String,
        val coverUrl: String? = null,
        val sourceUrl: String? = null,
        val views: Long? = null,
        val likes: Long? = null,
        val durationSeconds: Int? = null,
        val tags: List<String> = emptyList(),
        val description: String? = null,
    )

    data class Task(
        val id: Long,
        val request: Request,
        val state: State,
        val done: Int,
        val total: Int,
        val progress: Float,
        val saved: Int = 0,
        val skipped: Int = 0,
        val failed: Int = 0,
        val error: String? = null,
        val createdAt: Long = System.currentTimeMillis(),
    ) {
        val isActive get() = state == State.QUEUED || state == State.DOWNLOADING
    }

    data class Outcome(
        val saved: Int,
        val skipped: Int,
        val failed: Int,
        val error: String?,
        val paused: Boolean = false,
    )

    private class Runtime(
        val client: OkHttpClient,
        val headers: Map<String, String>,
        val waiter: CompletableDeferred<Outcome>,
    ) {
        @Volatile var job: Job? = null
        val created = ArrayList<String>()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(3)
    private val ids = AtomicLong(0)
    private val runtimes = ConcurrentHashMap<Long, Runtime>()
    private val json = Json { ignoreUnknownKeys = true }

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    /** Bumped whenever something finished, so screens know to rescan. */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    // ---------------------------------------------------------------- layout helpers

    data class Layout(val siteFolder: String, val folder: String, val channel: String?)

    private val GENERIC_SEGMENTS = setOf("photos", "gifs", "groups", "profiles", "other", "videos", "shorts")

    /** Maps the `segments` the site screens pass (e.g. `["Coser", title]`) to the new folder layout. */
    fun layoutFor(segments: List<String>, fallbackFolder: String): Layout {
        val site = DownloadLayout.sanitize(segments.firstOrNull(), "Other")
        val rest = segments.drop(1).filter { it.isNotBlank() && it.lowercase() !in GENERIC_SEGMENTS }
        return when {
            rest.isEmpty() -> Layout(site, DownloadLayout.sanitize(fallbackFolder, "Unsorted"), null)
            rest.size == 1 -> Layout(site, DownloadLayout.sanitize(rest[0]), null)
            else -> Layout(
                site,
                DownloadLayout.sanitize(rest.first() + " - " + rest.last()),
                rest.first(),
            )
        }
    }

    // ---------------------------------------------------------------- queue control

    fun enqueue(request: Request, client: OkHttpClient, headers: Map<String, String>): Deferred<Outcome> {
        val id = ids.incrementAndGet()
        val rt = Runtime(client, headers, CompletableDeferred())
        runtimes[id] = rt
        _tasks.update {
            it + Task(
                id = id,
                request = request,
                state = State.QUEUED,
                done = 0,
                total = if (request.kind == OfflineKind.PHOTOS) request.urls.size else 1,
                progress = 0f,
            )
        }
        start(id)
        return rt.waiter
    }

    private fun start(id: Long) {
        val rt = runtimes[id] ?: return
        rt.job?.cancel()
        rt.job = scope.launch { gate.withPermit { execute(id) } }
    }

    fun pause(id: Long) {
        val rt = runtimes[id] ?: return
        val task = taskOf(id) ?: return
        if (task.state != State.QUEUED && task.state != State.DOWNLOADING) return
        setTask(id) { it.copy(state = State.PAUSED) }
        rt.job?.cancel()
    }

    fun resume(id: Long) {
        val task = taskOf(id) ?: return
        if (task.state != State.PAUSED && task.state != State.FAILED) return
        setTask(id) { it.copy(state = State.QUEUED, error = null) }
        start(id)
    }

    fun pauseAll(kind: OfflineKind) = _tasks.value.filter { it.request.kind == kind && it.isActive }.forEach { pause(it.id) }

    fun resumeAll(kind: OfflineKind) = _tasks.value
        .filter { it.request.kind == kind && (it.state == State.PAUSED || it.state == State.FAILED) }
        .forEach { resume(it.id) }

    /** Cancels a task and removes what it has written so far. */
    fun cancel(id: Long) {
        val rt = runtimes.remove(id)
        val task = taskOf(id)
        rt?.job?.cancel()
        _tasks.update { list -> list.filterNot { it.id == id } }
        rt?.waiter?.complete(Outcome(0, 0, 0, "Cancelled", paused = true))
        if (rt != null && task != null && task.state != State.DONE) {
            scope.launch {
                val ctx = applicationContext
                synchronized(rt.created) { rt.created.toList() }.forEach { OfflineFs.delete(ctx, it) }
                val req = task.request
                OfflineFs.deleteIfEmpty(ctx, listOf(req.kind.folder, req.siteFolder, req.folder))
                _revision.update { it + 1 }
            }
        }
    }

    private fun taskOf(id: Long) = _tasks.value.firstOrNull { it.id == id }

    private fun setTask(id: Long, block: (Task) -> Task) {
        _tasks.update { list -> list.map { if (it.id == id) block(it) else it } }
    }

    // ---------------------------------------------------------------- running a task

    private fun extensionOf(url: String, allowed: Set<String>, fallback: String): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        val ext = path.substringAfterLast('.', "")
        return if (ext in allowed) ext else fallback
    }

    private val PHOTO_EXT = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val CLIP_EXT = setOf("mp4", "webm", "mkv", "mov", "m4v")

    private suspend fun execute(id: Long) {
        val rt = runtimes[id] ?: return
        val task = taskOf(id) ?: return
        if (task.state == State.PAUSED) return
        setTask(id) { it.copy(state = State.DOWNLOADING, error = null) }

        val ctx = applicationContext
        val req = task.request
        val segments = listOf(req.kind.folder, req.siteFolder, req.folder)
        var saved = 0
        var skipped = 0
        var failed = 0
        var lastError: String? = null

        try {
            if (!OfflineFs.isReady(ctx)) throw IOException("Choose a download folder first.")
            if (!OfflineFs.ensureDir(ctx, segments)) throw IOException("Can't create the download folder.")
            val existing = HashMap<String, Long>()
            OfflineFs.find(ctx, segments)?.children()?.forEach { if (!it.isDirectory) existing[it.name] = it.length }

            if (req.kind == OfflineKind.PHOTOS) {
                val pad = req.urls.size.toString().length.coerceAtLeast(2)
                req.urls.forEachIndexed { index, url ->
                    currentCoroutineContext().ensureActive()
                    val ext = extensionOf(url, PHOTO_EXT, "jpg")
                    val name = DownloadLayout.sanitize(req.baseName, "photo") +
                            (if (req.urls.size > 1) "_" + (index + 1).toString().padStart(pad, '0') else "") +
                            "." + ext
                    if ((existing[name] ?: 0L) > 0L) {
                        skipped++
                    } else {
                        try {
                            download(rt, url, segments, name, null)
                            existing[name] = 1L
                            saved++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            failed++
                            lastError = e.message
                        }
                    }
                    setTask(id) {
                        it.copy(
                            done = index + 1,
                            progress = (index + 1f) / req.urls.size,
                            saved = saved, skipped = skipped, failed = failed,
                        )
                    }
                }
                if (saved + skipped > 0) writePhotoInfo(ctx, segments, req)
            } else {
                val url = req.urls.first()
                if (url.substringBefore('?').lowercase().endsWith(".m3u8")) {
                    throw IOException("Streaming (HLS) clips can't be saved yet.")
                }
                val base = DownloadLayout.sanitize(req.baseName, "short")
                val ext = extensionOf(url, CLIP_EXT, "mp4")
                val videoName = "$base.$ext"
                if ((existing[videoName] ?: 0L) > 0L) {
                    skipped++
                } else {
                    download(rt, url, segments, videoName) { fraction ->
                        setTask(id) { it.copy(progress = fraction) }
                    }
                    saved++
                }
                setTask(id) { it.copy(done = 1, progress = 1f) }
                val cover = req.coverUrl
                if (!cover.isNullOrBlank() && existing.keys.none { it.substringBeforeLast('.').equals(base, true) && OfflineRepository.isImage(it) }) {
                    runCatching {
                        val thumbExt = extensionOf(cover, setOf("jpg", "jpeg", "png", "webp"), "jpg")
                        download(rt, cover, segments, "$base.$thumbExt", null)
                    }.onFailure { if (it is CancellationException) throw it }
                }
                writeShortInfo(ctx, segments, req, base, videoName)
            }
        } catch (e: CancellationException) {
            val state = taskOf(id)?.state
            if (state == State.PAUSED) {
                rt.waiter.complete(Outcome(saved, skipped, failed, null, paused = true))
            }
            throw e
        } catch (e: Exception) {
            failed++
            lastError = e.message ?: "Download failed."
        }

        val success = saved + skipped > 0
        val error = if (success) null else (lastError ?: "Download failed.")
        setTask(id) {
            it.copy(
                state = if (success) State.DONE else State.FAILED,
                saved = saved, skipped = skipped, failed = failed, error = error,
                done = if (success && req.kind == OfflineKind.SHORTS) 1 else it.done,
                progress = if (success) 1f else it.progress,
            )
        }
        _revision.update { it + 1 }
        rt.waiter.complete(Outcome(saved, skipped, failed, error))
        if (success) {
            delay(1500)
            runtimes.remove(id)
            _tasks.update { list -> list.filterNot { it.id == id } }
        }
    }

    private suspend fun download(
        rt: Runtime,
        url: String,
        segments: List<String>,
        name: String,
        onProgress: ((Float) -> Unit)?,
    ) {
        val ctx = applicationContext
        val request = okhttp3.Request.Builder().url(url).get().apply {
            rt.headers.forEach { (k, v) -> header(k, v) }
        }.build()
        val call = rt.client.newCall(request)
        val handle = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body
                val total = body.contentLength()
                val (out, uri) = OfflineFs.openOutput(ctx, segments, name, overwrite = true)
                    ?: throw IOException("Can't write $name")
                synchronized(rt.created) { rt.created += uri }
                try {
                    out.use { copy(body.byteStream(), it, total, onProgress) }
                } catch (e: Throwable) {
                    OfflineFs.delete(ctx, uri)
                    synchronized(rt.created) { rt.created -= uri }
                    throw e
                }
            }
        } finally {
            handle.dispose()
        }
    }

    private suspend fun copy(input: InputStream, output: OutputStream, total: Long, onProgress: ((Float) -> Unit)?) {
        val buffer = ByteArray(64 * 1024)
        var written = 0L
        var lastReport = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val n = input.read(buffer)
            if (n < 0) break
            output.write(buffer, 0, n)
            written += n
            if (onProgress != null && total > 0) {
                val now = System.currentTimeMillis()
                if (now - lastReport > 200) {
                    lastReport = now
                    onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
    }

    // ---------------------------------------------------------------- metadata

    private fun writePhotoInfo(ctx: android.content.Context, segments: List<String>, req: Request) {
        val node = OfflineFs.find(ctx, segments)
        val kids = node?.children().orEmpty()
        val images = kids.filter { !it.isDirectory && OfflineRepository.isImage(it.name) }
            .sortedWith(compareBy({ it.name.length }, { it.name.lowercase() }))
        val oldAdded = kids.firstOrNull { it.name.equals(OfflineRepository.INFO_JSON, true) }
            ?.let { OfflineFs.readText(it) }
            ?.let { runCatching { json.parseToJsonElement(it).jsonObject.num("addedAt") }.getOrNull() }
        val now = System.currentTimeMillis()
        val text = buildJsonObject {
            put("version", 3)
            put("kind", "photos")
            put("site", req.siteFolder)
            put("title", req.title)
            req.channel?.let { put("channel", it) }
            req.sourceUrl?.let { put("sourceUrl", it) }
            req.views?.let { put("views", it) }
            req.likes?.let { put("likes", it) }
            req.description?.let { put("description", it) }
            put("tags", buildJsonArray { req.tags.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            put("addedAt", oldAdded ?: now)
            put("updatedAt", now)
            put("count", images.size)
            put("photos", buildJsonArray { images.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.name)) } })
        }.toString()
        OfflineFs.writeText(ctx, segments, OfflineRepository.INFO_JSON, text)
    }

    private fun writeShortInfo(ctx: android.content.Context, segments: List<String>, req: Request, base: String, videoName: String) {
        val text = buildJsonObject {
            put("version", 3)
            put("kind", "shorts")
            put("site", req.siteFolder)
            put("title", req.title)
            put("channel", req.channel ?: req.folder)
            req.sourceUrl?.let { put("pageUrl", it) }
            put("videoFile", videoName)
            req.views?.let { put("views", it) }
            req.likes?.let { put("likes", it) }
            req.durationSeconds?.let { put("durationSeconds", it) }
            req.description?.let { put("description", it) }
            put("tags", buildJsonArray { req.tags.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            put("addedAt", System.currentTimeMillis())
        }.toString()
        OfflineFs.writeText(ctx, segments, "$base.json", text)
    }
}
