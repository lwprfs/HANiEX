package com.yenaly.han1meviewer.Buondua.data.repository

import android.content.Context
import com.yenaly.han1meviewer.Buondua.data.local.BuonduaDatabase
import com.yenaly.han1meviewer.Buondua.data.local.BuonduaHistoryDao
import com.yenaly.han1meviewer.Buondua.data.local.BuonduaHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object BuonduaHistoryRepo {

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

    private val dao: BuonduaHistoryDao by lazy {
        check(isInitialized) { "BuonduaHistoryRepo not initialized" }
        BuonduaDatabase.getInstance(appContext).buonduaHistoryDao()
    }

    fun loadAll(): Flow<List<BuonduaHistoryEntity>> = dao.loadAll()

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) { dao.deleteAll() }
    }

    suspend fun deleteByVideoCode(videoCode: String) {
        withContext(Dispatchers.IO) { dao.deleteByVideoCode(videoCode) }
    }

    suspend fun getByVideoCode(videoCode: String): BuonduaHistoryEntity? =
        withContext(Dispatchers.IO) { runCatching { dao.getByVideoCode(videoCode) }.getOrNull() }

    /** Records that an album was opened (and which page the reader reached). */
    suspend fun recordVisit(code: String, title: String, coverUrl: String, page: Int) =
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val existing = dao.getByVideoCode(code)
                val now = System.currentTimeMillis()
                dao.insertOrUpdate(
                    existing?.copy(
                        title = title,
                        coverUrl = coverUrl.ifBlank { existing.coverUrl },
                        watchDate = now,
                        lastPage = maxOf(existing.lastPage, page),
                        watchCount = existing.watchCount + 1,
                    ) ?: BuonduaHistoryEntity(
                        videoCode = code, title = title, coverUrl = coverUrl,
                        watchDate = now, lastPage = page, watchCount = 1,
                    )
                )
            }
        }
}
