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

internal class ShortsWindowEntry(val offset: Int, val key: String?, val url: String?)

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

    private val PRELOAD_CAP_BYTES = longArrayOf(10 * MIB, 6 * MIB, 4 * MIB)

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

    fun endSession() {
        val s = synchronized(lock) {
            val old = session
            session = null
            old
        } ?: return
        s.scope.cancel()
        s.signal.close()

        mainHandler.post {
            cleanupScope.launch {
                runCatching { s.cache.release() }
                runCatching { s.dir.deleteRecursively() }
                Log.d(TAG, "session cache cleared")
            }
        }
    }

    fun isCacheable(url: String): Boolean {
        val u = url.trim()
        if (!(u.startsWith("http://", true) || u.startsWith("https://", true))) return false
        val path = u.substringBefore('?').lowercase()
        return !(path.endsWith(".m3u8") || path.endsWith(".mpd") || path.endsWith(".ism") ||
                path.contains(".m3u8/") || path.contains(".ism/"))
    }

    fun cacheKey(site: String, id: String, url: String): String =
        "$site:$id:${url.trim().substringBefore('?')}"

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

                        if (s.downloadingKey == key) s.downloadJob?.cancel()
                        if (reserveLocked(s, key, current = true, requesterDistance = 0)) {
                            1
                        } else {

                            pending = s.downloadJob
                            pending?.cancel()
                            0
                        }
                    }
                }
                when (outcome) {
                    1 -> granted = true
                    0 -> if (pending == null) break else pending.join()
                    else -> break
                }
            }
            granted
        }

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

                s.downloadJob?.cancel()
            }
            s.signal.trySend(Unit)
        }
    }

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

    private fun reserveLocked(s: Session, key: String, current: Boolean, requesterDistance: Int): Boolean {
        s.slots[key]?.let {
            it.lastUsed = ++s.clock
            return true
        }
        while (s.slots.size >= MAX_ITEMS) {
            val victim = pickVictimLocked(s, key) ?: return false

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

                runInterruptible { priorityTaskManager.proceed(C.PRIORITY_DOWNLOAD) }
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
                if (isPriorityTooLow(e)) continue
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
