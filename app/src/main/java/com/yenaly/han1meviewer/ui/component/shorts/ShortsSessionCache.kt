package com.yenaly.han1meviewer.ui.component.shorts

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PriorityTaskManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.PriorityDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/**
 * One entry of the sliding window the short-video pager reports to [ShortsSessionCache].
 *
 * @param offset position relative to the item currently on screen (0 = current, 1 = next, -1 = previous).
 * @param key cache key of the media, or null when it can not (or can not yet) be cached.
 * @param url resolved stream url, only set together with [key].
 */
internal class ShortsWindowEntry(val offset: Int, val key: String?, val url: String?)

/**
 * Session-only disk cache + background preloader for GIFs / short videos.
 *
 * Policy (see the feature request):
 * - at most [MAX_ITEMS] cache entries exist at any moment (current + previous + preloaded + partial);
 * - the entry for the current item is always kept; the entry farthest from the current position is
 *   evicted first, and an entry that is being downloaded is never evicted;
 * - the next [PRELOAD_AHEAD] items are preloaded one at a time, strictly after the current item is
 *   ready, and every preload read goes through a [PriorityDataSource] so it yields to the player's
 *   own loading (the player registers [priorityTaskManager] with `PRIORITY_PLAYBACK` while it loads);
 * - everything lives in a dedicated directory that is wiped on app start (covers crashes / kills) and
 *   when the last activity of the app finishes.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Suppress("DEPRECATION")
internal object ShortsSessionCache {

    private const val TAG = "ShortsSessionCache"

    const val MAX_ITEMS = 6
    const val PRELOAD_AHEAD = 3

    private const val ROOT_DIR = "shorts_session"
    private const val FAILURE_BACKOFF_MS = 15_000L
    private const val NO_SLOT_BACKOFF_MS = 2_000L
    private const val UNKNOWN_DISTANCE = 100
    private const val MIB = 1024L * 1024L

    /** Head size preloaded per upcoming item (index 0 = next item). The player streams the rest. */
    private val PRELOAD_CAP_BYTES = longArrayOf(10 * MIB, 6 * MIB, 4 * MIB)

    /** Shared with every short-video ExoPlayer: while any of them loads, preloading is held back. */
    val priorityTaskManager = PriorityTaskManager()

    private class Slot(val key: String) {
        var lastUsed = 0L
        var complete = false
        var downloading = false
    }

    private class Target(val key: String, val url: String, val offset: Int)

    private class Session(val dir: File, val cache: SimpleCache) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val signal = Channel<Unit>(Channel.CONFLATED)
        val slots = LinkedHashMap<String, Slot>()
        val blockedUntil = HashMap<String, Long>()
        var clock = 0L
        var activeKey: String? = null
        var positions: Map<String, Int> = emptyMap()
        var targets: List<Target> = emptyList()
        var upstream: DataSource.Factory? = null
        var allowPreload = false
        var owner: Any? = null
        var downloadJob: Job? = null
        var downloadingKey: String? = null
    }

    private val lock = Any()
    private var appContext: Context? = null
    private var session: Session? = null
    private var aliveActivities = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---------------------------------------------------------------------------------------------
    // Application level lifecycle
    // ---------------------------------------------------------------------------------------------

    /**
     * Call once from `Application.onCreate` of the main process.
     * Deletes everything a previous (possibly killed) session left behind and arms the exit cleanup.
     */
    fun onAppStart(app: Application) {
        appContext = app.applicationContext
        runCatching { File(app.cacheDir, ROOT_DIR).deleteRecursively() }
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                aliveActivities++
            }

            override fun onActivityDestroyed(activity: Activity) {
                aliveActivities = (aliveActivities - 1).coerceAtLeast(0)
                if (aliveActivities == 0 && activity.isFinishing && !activity.isChangingConfigurations) {
                    endSession()
                }
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        })
    }

    /**
     * Ends the current session: cancels every preload, then (after the UI had a chance to release its
     * players) releases the cache and deletes all session files, including partial downloads.
     */
    fun endSession() {
        val s = synchronized(lock) {
            val old = session
            session = null
            old
        } ?: return
        s.scope.cancel()
        s.signal.close()
        // Posted behind the pending destroy work so the players are released before the cache is.
        mainHandler.post {
            cleanupScope.launch {
                runCatching { s.cache.release() }
                runCatching { s.dir.deleteRecursively() }
                Log.d(TAG, "session cache cleared")
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers used by the player
    // ---------------------------------------------------------------------------------------------

    /** Only plain progressive http(s) files are cached; HLS/DASH and local files are not. */
    fun isCacheable(url: String): Boolean {
        val u = url.trim()
        if (!(u.startsWith("http://", true) || u.startsWith("https://", true))) return false
        val path = u.substringBefore('?').lowercase()
        return !(path.endsWith(".m3u8") || path.endsWith(".mpd") || path.endsWith(".ism") ||
                path.contains(".m3u8/") || path.contains(".ism/"))
    }

    /** Stable key per media: site + id + url without the (often expiring) query string. */
    fun cacheKey(site: String, id: String, url: String): String =
        "$site:$id:${url.trim().substringBefore('?')}"

    /**
     * Data source for the player. It resolves the cache lazily so a player created before a session
     * restart never keeps writing into a released cache.
     */
    fun playerDataSourceFactory(upstream: DataSource.Factory): DataSource.Factory =
        DataSource.Factory {
            val cache = synchronized(lock) { session?.cache }
            val ds: DataSource = if (cache == null) {
                upstream.createDataSource()
            } else {
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstream)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()
            }
            ds
        }

    /**
     * Reserves a cache slot for the item that is about to start playing, evicting the least useful
     * entry first if all [MAX_ITEMS] slots are taken. Must complete before the player starts reading.
     *
     * @return false when no slot could be reserved (the caller should then play uncached).
     */
    suspend fun beginCurrent(owner: Any, key: String, entries: List<ShortsWindowEntry>): Boolean =
        withContext(Dispatchers.IO) {
            var attempts = 0
            var granted = false
            while (attempts < 2 && !granted) {
                attempts++
                var pending: Job? = null
                val outcome = synchronized(lock) {
                    val s = ensureSessionLocked()
                    if (s == null) {
                        -1
                    } else {
                        s.owner = owner
                        applyWindowLocked(s, key, entries)
                        // The item became current while it was being preloaded: the player takes over,
                        // never let both write the same entry.
                        if (s.downloadingKey == key) s.downloadJob?.cancel()
                        if (reserveLocked(s, key, current = true, requesterDistance = 0)) {
                            1
                        } else {
                            // Only an in-flight preload can be in the way: stop it, wait, retry once.
                            pending = s.downloadJob
                            pending?.cancel()
                            0
                        }
                    }
                }
                when (outcome) {
                    1 -> granted = true
                    0 -> if (pending == null) break else pending?.join()
                    else -> break
                }
            }
            granted
        }

    /**
     * Reports the current window. Cheap and non-blocking; safe to call from the main thread.
     *
     * @param allowPreload true only once the current item is ready to play (or has failed), so the
     * current item always gets the network first.
     */
    fun update(
        owner: Any,
        activeKey: String?,
        entries: List<ShortsWindowEntry>,
        upstream: DataSource.Factory,
        allowPreload: Boolean,
    ) {
        synchronized(lock) {
            val s = session ?: return@synchronized
            s.owner = owner
            applyWindowLocked(s, activeKey, entries)
            s.targets = entries
                .filter { it.offset in 1..PRELOAD_AHEAD && it.key != null && it.url != null }
                .sortedBy { it.offset }
                .map { Target(it.key!!, it.url!!, it.offset) }
            s.upstream = upstream
            s.allowPreload = allowPreload

            val downloading = s.downloadingKey
            if (downloading != null &&
                (!allowPreload || downloading == activeKey || s.targets.none { it.key == downloading })
            ) {
                // Not wanted any more (scrolled past / became current / current needs the network).
                s.downloadJob?.cancel()
            }
            s.signal.trySend(Unit)
        }
    }

    /** Stops background work of [owner] (e.g. the pager left composition). Cached data is kept. */
    fun detach(owner: Any) {
        synchronized(lock) {
            val s = session ?: return@synchronized
            if (s.owner !== owner) return@synchronized
            s.allowPreload = false
            s.targets = emptyList()
            s.upstream = null
            s.downloadJob?.cancel()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Internals (everything below runs under [lock] unless stated otherwise)
    // ---------------------------------------------------------------------------------------------

    private fun ensureSessionLocked(): Session? {
        session?.let { return it }
        val ctx = appContext ?: return null
        return try {
            val dir = File(File(ctx.cacheDir, ROOT_DIR), System.nanoTime().toString())
            if (!dir.exists() && !dir.mkdirs()) return null
            val s = Session(dir, SimpleCache(dir, NoOpCacheEvictor()))
            session = s
            s.scope.launch { pumpLoop(s) }
            s
        } catch (e: Exception) {
            Log.e(TAG, "cannot create session cache", e)
            null
        }
    }

    private fun applyWindowLocked(s: Session, activeKey: String?, entries: List<ShortsWindowEntry>) {
        s.activeKey = activeKey
        val map = HashMap<String, Int>()
        entries.forEach { e -> e.key?.let { map[it] = e.offset } }
        s.positions = map
    }

    private fun distanceOf(s: Session, key: String): Int =
        s.positions[key]?.let { abs(it) } ?: UNKNOWN_DISTANCE

    /**
     * Makes sure [key] owns a slot, evicting if needed so that [MAX_ITEMS] is never exceeded, not
     * even transiently (the victim's files are deleted before the new slot is handed out).
     */
    private fun reserveLocked(s: Session, key: String, current: Boolean, requesterDistance: Int): Boolean {
        s.slots[key]?.let {
            it.lastUsed = ++s.clock
            return true
        }
        while (s.slots.size >= MAX_ITEMS) {
            val victim = pickVictimLocked(s, key) ?: return false
            // A preload must never push out something that is more useful than itself.
            if (!current && distanceOf(s, victim.key) < requesterDistance) return false
            evictLocked(s, victim)
        }
        s.slots[key] = Slot(key).also { it.lastUsed = ++s.clock }
        return true
    }

    private fun pickVictimLocked(s: Session, requesterKey: String): Slot? {
        var best: Slot? = null
        for (slot in s.slots.values) {
            if (slot.key == requesterKey || slot.key == s.activeKey || slot.downloading) continue
            val b = best
            if (b == null) {
                best = slot
                continue
            }
            val ds = distanceOf(s, slot.key)
            val db = distanceOf(s, b.key)
            if (ds > db || (ds == db && slot.lastUsed < b.lastUsed)) best = slot
        }
        return best
    }

    private fun evictLocked(s: Session, slot: Slot) {
        try {
            s.cache.removeResource(slot.key)
        } catch (e: Exception) {
            Log.w(TAG, "evict failed", e)
        }
        s.slots.remove(slot.key)
        s.blockedUntil.remove(slot.key)
    }

    private fun nextTargetLocked(s: Session): Pair<Target, DataSource.Factory>? {
        if (!s.allowPreload) return null
        val upstream = s.upstream ?: return null
        val now = SystemClock.elapsedRealtime()
        for (t in s.targets) {
            if (s.slots[t.key]?.complete == true) continue
            if ((s.blockedUntil[t.key] ?: 0L) > now) continue
            return t to upstream
        }
        return null
    }

    /** Single worker: at most one preload download runs at a time. */
    private suspend fun pumpLoop(s: Session) {
        for (ignored in s.signal) {
            while (true) {
                val next = synchronized(lock) {
                    if (session !== s) null else nextTargetLocked(s)
                } ?: break
                runPreload(s, next.first, next.second)
            }
        }
    }

    private suspend fun runPreload(s: Session, t: Target, upstream: DataSource.Factory) {
        val reserved = synchronized(lock) {
            if (session !== s) {
                false
            } else {
                val ok = reserveLocked(s, t.key, current = false, requesterDistance = t.offset)
                if (!ok) s.blockedUntil[t.key] = SystemClock.elapsedRealtime() + NO_SLOT_BACKOFF_MS
                ok
            }
        }
        if (!reserved) return

        val cap = PRELOAD_CAP_BYTES[(t.offset - 1).coerceIn(0, PRELOAD_CAP_BYTES.lastIndex)]
        val job = s.scope.launch(start = CoroutineStart.LAZY) { download(s, t, upstream, cap) }
        val started = synchronized(lock) {
            if (session !== s) {
                false
            } else {
                s.downloadJob = job
                s.downloadingKey = t.key
                s.slots[t.key]?.downloading = true
                true
            }
        }
        if (!started) {
            job.cancel()
            return
        }
        try {
            job.start()
            job.join()
        } finally {
            synchronized(lock) {
                if (s.downloadJob === job) {
                    s.downloadJob = null
                    s.downloadingKey = null
                }
                s.slots[t.key]?.downloading = false
            }
        }
    }

    private suspend fun download(s: Session, t: Target, upstream: DataSource.Factory, cap: Long) {
        val job = currentCoroutineContext().job
        val spec = DataSpec.Builder().setUri(t.url).setKey(t.key).build()
        while (true) {
            currentCoroutineContext().ensureActive()
            val dataSource = CacheDataSource.Factory()
                .setCache(s.cache)
                .setUpstreamDataSourceFactory(
                    DataSource.Factory {
                        PriorityDataSource(upstream.createDataSource(), priorityTaskManager, C.PRIORITY_DOWNLOAD)
                    }
                )
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSource()
            var capped = false
            var writerRef: CacheWriter? = null
            val writer = CacheWriter(dataSource, spec, null) { _, bytesCached, _ ->
                val w = writerRef
                if (!job.isActive) {
                    w?.cancel()
                } else if (bytesCached >= cap) {
                    capped = true
                    w?.cancel()
                }
            }
            writerRef = writer
            try {
                // Wait while the player is loading (current video first), then download.
                runInterruptible { priorityTaskManager.block(C.PRIORITY_DOWNLOAD) }
                runInterruptible { writer.cache() }
                markComplete(s, t.key)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (capped) {
                    markComplete(s, t.key)
                    return
                }
                currentCoroutineContext().ensureActive()
                if (isPriorityTooLow(e)) continue // the player started loading: wait and resume
                Log.w(TAG, "preload failed for ${t.key.hashCode()}", e)
                synchronized(lock) {
                    s.blockedUntil[t.key] = SystemClock.elapsedRealtime() + FAILURE_BACKOFF_MS
                }
                return
            }
        }
    }

    private fun markComplete(s: Session, key: String) {
        synchronized(lock) { s.slots[key]?.complete = true }
    }

    private fun isPriorityTooLow(e: Throwable): Boolean =
        generateSequence(e) { it.cause }.any { it is PriorityTaskManager.PriorityTooLowException }
}
