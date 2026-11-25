package com.yenaly.han1meviewer.SitesHub.Coser.data.repository

import android.content.Context
import com.yenaly.han1meviewer.SitesHub.Coser.data.local.CoserDatabase
import com.yenaly.han1meviewer.SitesHub.Coser.data.local.CoserHistoryDao
import com.yenaly.han1meviewer.SitesHub.Coser.data.local.CoserHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object CoserHistoryRepo {

    @Volatile private var isInitialized = false
    @Volatile private lateinit var appContext: Context
    private val writeMutex = Mutex()

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            appContext = context.applicationContext
            isInitialized = true
        }
    }

    private val dao: CoserHistoryDao by lazy {
        check(isInitialized) { "CoserHistoryRepo not initialized" }
        CoserDatabase.getInstance(appContext).coserHistoryDao()
    }

    fun loadAll(): Flow<List<CoserHistoryEntity>> = dao.loadAll()

    suspend fun getAllHistory(): List<CoserHistoryEntity> = withContext(Dispatchers.IO) {
        runCatching { dao.getAllHistorySync() }.getOrDefault(emptyList())
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { dao.deleteAll() }

    suspend fun deleteByVideoCode(videoCode: String) =
        withContext(Dispatchers.IO) { dao.deleteByVideoCode(videoCode) }

    suspend fun getByVideoCode(videoCode: String): CoserHistoryEntity? =
        withContext(Dispatchers.IO) { runCatching { dao.getByVideoCode(videoCode) }.getOrNull() }

    suspend fun touch(slug: String, title: String, coverUrl: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val now = System.currentTimeMillis()
            val existing = dao.getByVideoCode(slug)
            dao.insertOrUpdate(
                existing?.copy(
                    title = title.ifBlank { existing.title },
                    coverUrl = coverUrl.ifBlank { existing.coverUrl },
                    watchDate = now,
                ) ?: CoserHistoryEntity(
                    videoCode = slug, title = title, coverUrl = coverUrl, watchDate = now,
                    watchCount = 0,
                )
            )
        }
    }

    suspend fun upsertWatchProgress(
        videoCode: String,
        title: String,
        coverUrl: String,
        currentPosition: Long,
        totalDuration: Long,
        isPlaying: Boolean,
        wasPlayed: Boolean,
    ) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val existing = dao.getByVideoCode(videoCode)
            val now = System.currentTimeMillis()
            val history = if (existing != null) {
                existing.copy(
                    watchDate = now,
                    watchDuration = if (isPlaying) {
                        existing.watchDuration + (currentPosition - existing.lastPosition).coerceAtLeast(0)
                    } else existing.watchDuration,
                    lastPosition = currentPosition,
                    totalDuration = totalDuration,
                    watchCount = if (wasPlayed) existing.watchCount + 1 else existing.watchCount,
                    playCount = if (wasPlayed && isPlaying) existing.playCount + 1 else existing.playCount,
                    isPlayed = wasPlayed || existing.isPlayed,
                    lastPlayedDate = if (wasPlayed && isPlaying) now else existing.lastPlayedDate,
                )
            } else {
                CoserHistoryEntity(
                    videoCode = videoCode,
                    title = title,
                    coverUrl = coverUrl,
                    watchDate = now,
                    lastPosition = currentPosition,
                    totalDuration = totalDuration,
                    watchCount = if (wasPlayed) 1 else 0,
                    playCount = if (wasPlayed && isPlaying) 1 else 0,
                    isPlayed = wasPlayed,
                    lastPlayedDate = if (wasPlayed && isPlaying) now else null,
                )
            }
            dao.insertOrUpdate(history)
        }
    }
}
