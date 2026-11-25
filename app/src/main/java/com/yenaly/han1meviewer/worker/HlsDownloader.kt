package com.yenaly.han1meviewer.worker

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.security.GeneralSecurityException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class HlsHttpException(val code: Int, message: String) : IOException(message)

class HlsDownloader(
    private val client: OkHttpClient,
    private val headers: Map<String, String>,

    private val urlConnectionFallback: Boolean = false,
) {

    data class Key(val uri: String, val iv: ByteArray?)

    data class Segment(
        val url: String,
        val sequence: Long,
        val key: Key?,
    )

    data class MediaPlaylist(
        val segments: List<Segment>,
        val initSegmentUrl: String?,
    )

    private fun request(url: String): Request =
        Request.Builder().url(url).get().apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()

    private fun fetchBytes(url: String): ByteArray {
        if (!urlConnectionFallback) return fetchBytesOkHttp(url)
        return try {
            fetchBytesOkHttp(url)
        } catch (e: HlsHttpException) {
            if (e.code == 404 || e.code == 410) throw e
            fetchBytesUrlConnection(url, e)
        } catch (e: IOException) {
            fetchBytesUrlConnection(url, e)
        }
    }

    private fun fetchBytesUrlConnection(url: String, original: Exception): ByteArray {
        var conn: java.net.HttpURLConnection? = null
        try {
            conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.instanceFollowRedirects = true
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (headers.keys.none { it.equals("User-Agent", true) }) {
                conn.setRequestProperty("User-Agent", "ExoPlayerLib/1.4.0")
            }
            val code = conn.responseCode
            if (code !in 200..299) throw HlsHttpException(code, "HTTP $code for $url")
            return conn.inputStream.use { it.readBytes() }
        } catch (e: HlsHttpException) {
            throw e
        } catch (e: IOException) {

            throw if (original is IOException) original else e
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchBytesOkHttp(url: String): ByteArray {
        client.newCall(request(url)).execute().use { response ->
            if (!response.isSuccessful) {
                throw HlsHttpException(response.code, "HTTP ${response.code} for $url")
            }
            val finalPath = response.request.url.encodedPath.lowercase()
            if (finalPath.contains("/login")) {
                throw HlsHttpException(401, "Redirected to login for $url")
            }
            return response.body.bytes()
        }
    }

    private fun fetchText(url: String): String = String(fetchBytes(url), Charsets.UTF_8)

    private fun resolve(base: String, ref: String): String =
        runCatching { URI(base).resolve(ref).toString() }.getOrDefault(ref)

    suspend fun resolvePlaylist(url: String): MediaPlaylist = withContext(Dispatchers.IO) {
        var currentUrl = url
        var text = fetchText(currentUrl)
        if (!text.trimStart().startsWith("#EXTM3U")) {
            throw IOException("Not an HLS playlist")
        }
        var guard = 0
        while (text.contains("#EXT-X-STREAM-INF") && guard < 4) {
            currentUrl = pickBestVariant(currentUrl, text)
                ?: throw IOException("Playlist has no playable variant")
            text = fetchText(currentUrl)
            guard++
        }
        parseMedia(currentUrl, text)
    }

    private fun pickBestVariant(baseUrl: String, text: String): String? {
        val lines = text.lines().map { it.trim() }
        var bestBandwidth = -1L
        var best: String? = null
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val bandwidth = Regex("""BANDWIDTH=(\d+)""").find(line)
                    ?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                var j = i + 1
                while (j < lines.size && (lines[j].isEmpty() || lines[j].startsWith("#"))) j++
                if (j < lines.size && bandwidth >= bestBandwidth) {
                    bestBandwidth = bandwidth
                    best = resolve(baseUrl, lines[j])
                }
                i = j
            }
            i++
        }
        return best
    }

    private fun parseMedia(baseUrl: String, text: String): MediaPlaylist {
        var mediaSequence = 0L
        var currentKey: Key? = null
        var initUrl: String? = null
        val segments = ArrayList<Segment>()
        var expectUri = false
        var sequence = 0L
        for (raw in text.lines()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> {
                    mediaSequence = line.substringAfter(':').trim().toLongOrNull() ?: 0L
                    sequence = mediaSequence
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    val method = Regex("""METHOD=([A-Z0-9-]+)""").find(line)?.groupValues?.get(1)
                    currentKey = when (method) {
                        null, "NONE" -> null
                        "AES-128" -> {
                            val uri = Regex("""URI="([^"]+)"""").find(line)?.groupValues?.get(1)
                                ?: throw IOException("Encrypted playlist without key URI")
                            val iv = Regex("""IV=0[xX]([0-9a-fA-F]+)""").find(line)
                                ?.groupValues?.get(1)?.let(::hexToBytes)
                            Key(resolve(baseUrl, uri), iv)
                        }
                        else -> throw IOException("Unsupported HLS encryption: $method")
                    }
                }
                line.startsWith("#EXT-X-MAP:") -> {
                    Regex("""URI="([^"]+)"""").find(line)?.groupValues?.get(1)
                        ?.let { initUrl = resolve(baseUrl, it) }
                }
                line.startsWith("#EXTINF") -> expectUri = true
                line.startsWith("#") -> Unit
                else -> {
                    segments += Segment(resolve(baseUrl, line), sequence, currentKey)
                    sequence++
                    expectUri = false
                }
            }
        }
        if (segments.isEmpty()) throw IOException("Playlist has no segments")
        return MediaPlaylist(segments, initUrl)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val padded = if (hex.length % 2 == 0) hex else "0$hex"
        val raw = ByteArray(padded.length / 2) {
            padded.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
        if (raw.size >= 16) return raw.copyOfRange(raw.size - 16, raw.size)
        return ByteArray(16 - raw.size) + raw
    }

    private fun sequenceIv(sequence: Long): ByteArray {
        val iv = ByteArray(16)
        for (i in 0 until 8) iv[15 - i] = (sequence shr (8 * i)).toByte()
        return iv
    }

    private val keyCache = HashMap<String, ByteArray>()

    private fun keyBytes(uri: String): ByteArray =
        synchronized(keyCache) { keyCache[uri] } ?: fetchBytes(uri).also {
            if (it.size != 16) throw IOException("Unexpected HLS key length ${it.size}")
            synchronized(keyCache) { keyCache[uri] = it }
        }

    /** A downloaded (and decrypted) segment. Only the first [length] bytes of [data] are valid. */
    private class Chunk(val data: ByteArray, val length: Int)

    private class Completion(val index: Int, val chunk: Chunk?, val error: Throwable?)

    private val activeCancels = ConcurrentHashMap<Any, () -> Unit>()
    private val errorSignals = AtomicInteger(0)

    private fun cancelActiveRequests() {
        activeCancels.values.forEach { runCatching { it() } }
        activeCancels.clear()
    }

    private fun fetchSegmentBytes(url: String): Chunk {
        if (!urlConnectionFallback) return fetchSegmentOkHttp(url)
        return try {
            fetchSegmentOkHttp(url)
        } catch (e: HlsHttpException) {
            if (e.code == 404 || e.code == 410) throw e
            val bytes = fetchBytesUrlConnection(url, e)
            Chunk(bytes, bytes.size)
        } catch (e: IOException) {
            val bytes = fetchBytesUrlConnection(url, e)
            Chunk(bytes, bytes.size)
        }
    }

    private fun fetchSegmentOkHttp(url: String): Chunk {
        val call = client.newCall(request(url))
        val token = Any()
        activeCancels[token] = { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw HlsHttpException(response.code, "HTTP ${response.code} for $url")
                }
                val finalPath = response.request.url.encodedPath.lowercase()
                if (finalPath.contains("/login")) {
                    throw HlsHttpException(401, "Redirected to login for $url")
                }
                val body = response.body
                val len = body.contentLength()
                // Known size: read straight into an exact-size array (no growth copies).
                return if (len in 1..MAX_EXACT_READ_BYTES) {
                    val buf = ByteArray(len.toInt())
                    body.source().readFully(buf)
                    Chunk(buf, buf.size)
                } else {
                    val bytes = body.bytes()
                    Chunk(bytes, bytes.size)
                }
            }
        } finally {
            activeCancels.remove(token)
        }
    }

    private fun decrypt(chunk: Chunk, segment: Segment, key: Key): Chunk {
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes(key.uri), "AES"),
                IvParameterSpec(key.iv ?: sequenceIv(segment.sequence)),
            )
            val plain = cipher.doFinal(chunk.data, 0, chunk.length)
            return Chunk(plain, plain.size)
        } catch (e: GeneralSecurityException) {
            throw IOException("Segment decrypt failed: ${e.message}", e)
        }
    }

    private suspend fun downloadSegment(segment: Segment): Chunk {
        var lastError: Exception? = null
        repeat(MAX_SEGMENT_ATTEMPTS) { attempt ->
            currentCoroutineContext().ensureActive()
            try {
                val chunk = fetchSegmentBytes(segment.url)
                val key = segment.key ?: return chunk
                return decrypt(chunk, segment, key)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HlsHttpException) {
                if (e.code == 401 || e.code == 403 || e.code == 404 || e.code == 410) throw e
                errorSignals.incrementAndGet()
                lastError = e
            } catch (e: IOException) {
                // A call cancelled by our own watchdog must not be retried.
                currentCoroutineContext().ensureActive()
                errorSignals.incrementAndGet()
                lastError = e
            }
            if (attempt < MAX_SEGMENT_ATTEMPTS - 1) delay(600L * (attempt + 1))
        }
        throw lastError ?: IOException("Segment download failed")
    }

    private suspend fun fetchInitSegment(url: String): ByteArray {
        var lastError: Exception? = null
        repeat(MAX_SEGMENT_ATTEMPTS) { attempt ->
            currentCoroutineContext().ensureActive()
            try {
                return fetchBytes(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HlsHttpException) {
                if (e.code == 401 || e.code == 403 || e.code == 404 || e.code == 410) throw e
                lastError = e
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                lastError = e
            }
            if (attempt < MAX_SEGMENT_ATTEMPTS - 1) delay(600L * (attempt + 1))
        }
        throw lastError ?: IOException("Init segment download failed")
    }

    /**
     * Adaptive request concurrency (hill climbing on measured throughput).
     * Only touched from the scheduler coroutine, so it needs no locking.
     */
    private class ConcurrencyController(
        private val min: Int,
        private val max: Int,
        start: Int,
    ) {
        var limit = start.coerceIn(min, max)
            private set
        var peak = limit
            private set

        private var windowStartNs = System.nanoTime()
        private var windowBytes = 0L
        private var lastRate = 0.0
        private var lastAction = 0
        private var hold = 0

        fun addBytes(n: Long) {
            windowBytes += n
        }

        fun tick(errors: Int) {
            val elapsed = (System.nanoTime() - windowStartNs) / 1e9
            if (elapsed < WINDOW_SECONDS) return
            val rate = windowBytes / elapsed
            if (errors > 0) {
                // Server/network pushed back: back off and hold for a while.
                limit = (limit - 1).coerceAtLeast(min)
                lastAction = -1
                hold = 2
            } else if (hold > 0) {
                hold--
            } else if (lastAction == 1 && rate < lastRate * MIN_GAIN) {
                // The last increase didn't buy any throughput: step back.
                limit = (limit - 1).coerceAtLeast(min)
                lastAction = -1
                hold = 3
            } else if (limit < max && windowBytes > 0) {
                limit++
                lastAction = 1
            } else {
                lastAction = 0
            }
            if (limit > peak) peak = limit
            lastRate = rate
            windowStartNs = System.nanoTime()
            windowBytes = 0L
        }

        companion object {
            private const val WINDOW_SECONDS = 3.0
            private const val MIN_GAIN = 1.05
        }
    }

    /**
     * Writes the playlist into [out], in playlist order, using a sliding window of concurrent
     * segment requests: a new request starts the moment another finishes, never waiting for a
     * whole batch. Completed-but-not-yet-writable segments are held in a small reorder buffer
     * whose size is bounded in bytes.
     *
     * [parallelism] is the upper bound (clamped to 1..8); the effective number of simultaneous
     * requests adapts between 4 and that bound to the measured throughput, and is lowered
     * temporarily by [PlaybackPriority] while the player is buffering.
     *
     * To resume, pass the number of segments already written ([startSegment]) and the byte
     * count they occupy ([startBytes]); [out] must already be positioned at [startBytes].
     * [onProgress] gets (finished segments, total segments, bytes written so far) after every
     * segment. Returns the total number of bytes in the file.
     */
    suspend fun download(
        playlist: MediaPlaylist,
        out: OutputStream,
        parallelism: Int = MAX_PARALLELISM,
        startSegment: Int = 0,
        startBytes: Long = 0L,
        onProgress: suspend (done: Int, total: Int, bytes: Long) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        val total = playlist.segments.size
        val begin = startSegment.coerceIn(0, total)
        var bytes = if (begin > 0) startBytes.coerceAtLeast(0L) else 0L
        val maxPar = parallelism.coerceIn(1, MAX_PARALLELISM)
        val minPar = minOf(MIN_PARALLELISM, maxPar)

        if (begin == 0) {
            playlist.initSegmentUrl?.let { initUrl ->
                val data = fetchInitSegment(initUrl)
                out.write(data)
                bytes += data.size
            }
        }

        val controller = ConcurrencyController(minPar, maxPar, minOf(START_PARALLELISM, maxPar))
        val pendingBudget = pendingBudgetBytes()
        val maxAhead = maxPar * 3
        val completions = Channel<Completion>(Channel.UNLIMITED)
        val pending = HashMap<Int, Chunk>()
        var pendingBytes = 0L
        var nextLaunch = begin
        var nextWrite = begin
        var inFlight = 0
        var completed = 0
        var completedBytes = 0L
        val startedAt = System.nanoTime()
        val sessionBytes = AtomicLong(0)

        try {
            coroutineScope {
                val watchdog = launch {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelActiveRequests()
                    }
                }
                try {
                    while (nextWrite < total) {
                        ensureActive()
                        val cap = minOf(controller.limit, PlaybackPriority.concurrencyCap(maxPar))
                            .coerceAtLeast(1)
                        val avg = if (completed > 0) completedBytes / completed else 0L
                        while (inFlight < cap && nextLaunch < total && nextLaunch - nextWrite < maxAhead) {
                            // The head-of-line segment is always allowed; later ones only while
                            // the reorder buffer (plus what is already on the wire) fits the budget.
                            if (nextLaunch != nextWrite &&
                                pendingBytes + (inFlight + 1) * avg > pendingBudget
                            ) break
                            val idx = nextLaunch++
                            inFlight++
                            val segment = playlist.segments[idx]
                            launch(Dispatchers.IO) {
                                val completion = try {
                                    Completion(idx, downloadSegment(segment), null)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    Completion(idx, null, e)
                                }
                                completions.trySend(completion)
                            }
                        }

                        val completion = completions.receive()
                        inFlight--
                        completion.error?.let { throw it }
                        val chunk = completion.chunk ?: throw IOException("Empty segment result")
                        pending[completion.index] = chunk
                        pendingBytes += chunk.length
                        completed++
                        completedBytes += chunk.length
                        sessionBytes.addAndGet(chunk.length.toLong())
                        controller.addBytes(chunk.length.toLong())
                        controller.tick(errorSignals.getAndSet(0))

                        while (true) {
                            val ready = pending.remove(nextWrite) ?: break
                            out.write(ready.data, 0, ready.length)
                            bytes += ready.length
                            pendingBytes -= ready.length
                            nextWrite++
                            onProgress(nextWrite, total, bytes)
                        }
                    }
                } finally {
                    watchdog.cancel()
                }
            }
            out.flush()
        } finally {
            completions.close()
            pending.clear()
            val seconds = (System.nanoTime() - startedAt) / 1e9
            if (seconds > 0.5) {
                Log.i(
                    TAG,
                    "hls session: segments=$completed, bytes=${sessionBytes.get()}, " +
                            "avg=${(sessionBytes.get() / seconds / 1024).toLong()}KiB/s, " +
                            "peakConcurrency=${controller.peak}, finalConcurrency=${controller.limit}"
                )
            }
        }
        bytes
    }

    private fun pendingBudgetBytes(): Long {
        val heap = Runtime.getRuntime().maxMemory()
        return (heap / 16).coerceIn(8L * 1024 * 1024, 48L * 1024 * 1024)
    }

    companion object {
        private const val TAG = "HlsDownloader"
        const val MAX_PARALLELISM = 8
        private const val MIN_PARALLELISM = 4
        private const val START_PARALLELISM = 6
        private const val MAX_SEGMENT_ATTEMPTS = 3
        private const val MAX_EXACT_READ_BYTES = 128L * 1024 * 1024

        fun isHls(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val clean = url.substringBefore('#').lowercase()
            // rou.video's playlist endpoint has no .m3u8 extension (it 302s to a wrapped playlist).
            return clean.contains(".m3u8") || clean.contains("/api/hls/")
        }
    }
}
