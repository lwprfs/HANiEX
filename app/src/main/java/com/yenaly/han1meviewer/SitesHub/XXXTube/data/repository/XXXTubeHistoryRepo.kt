package com.yenaly.han1meviewer.SitesHub.XXXTube.data.repository
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

import com.yenaly.han1meviewer.SitesHub.XXXTube.data.local.XXXTubeDatabase
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.local.XXXTubeHistoryDao
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.local.XXXTubeHistoryEntity
object XXXTubeHistoryRepo {

    @Volatile private var isInitialized = false
    @Volatile private lateinit var appContext: Context

    private val writeMutex = Mutex()

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            appContext = context.applicationContext
            isInitialized = true
            android.util.Log.d("XXXTubeHistoryRepo", "Initialized")
        }
    }

    private val dao: XXXTubeHistoryDao by lazy {
        check(isInitialized) { "XXXTubeHistoryRepo not initialized" }
        XXXTubeDatabase.getInstance(appContext).xxxTubeHistoryDao()
    }

    suspend fun insertOrUpdate(history: XXXTubeHistoryEntity) {
        withContext(Dispatchers.IO) { dao.insertOrUpdate(history) }
    }

    suspend fun getAllHistory(): List<XXXTubeHistoryEntity> = withContext(Dispatchers.IO) {
        runCatching { dao.getAllHistorySync() }
            .onFailure { android.util.Log.e("XXXTubeHistoryRepo", "Error getting history", it) }
            .getOrDefault(emptyList())
    }

    fun loadAll(): Flow<List<XXXTubeHistoryEntity>> = dao.loadAll()

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) { dao.deleteAll() }
    }

    suspend fun deleteByVideoCode(videoCode: String) {
        withContext(Dispatchers.IO) { dao.deleteByVideoCode(videoCode) }
    }

    suspend fun getByVideoCode(videoCode: String): XXXTubeHistoryEntity? =
        withContext(Dispatchers.IO) {
            runCatching { dao.getByVideoCode(videoCode) }.getOrNull()
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
                val newWatchCount =
                    if (wasPlayed) existing.watchCount + 1 else existing.watchCount
                val newPlayCount =
                    if (wasPlayed && isPlaying) existing.playCount + 1 else existing.playCount
                val newWatchDuration = if (isPlaying) {
                    existing.watchDuration +
                            (currentPosition - existing.lastPosition).coerceAtLeast(0)
                } else existing.watchDuration

                existing.copy(
                    watchDate = now,
                    watchDuration = newWatchDuration,
                    lastPosition = currentPosition,
                    totalDuration = totalDuration,
                    watchCount = newWatchCount,
                    playCount = newPlayCount,
                    isPlayed = wasPlayed || existing.isPlayed,
                    lastPlayedDate = if (wasPlayed && isPlaying) now else existing.lastPlayedDate,
                )
            } else {
                XXXTubeHistoryEntity(
                    videoCode = videoCode,
                    title = title,
                    coverUrl = coverUrl,
                    watchDate = now,
                    watchDuration = 0,
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
