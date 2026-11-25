package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeakGalleryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<LeakGalleryHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: LeakGalleryHistoryEntity)

    @Query("SELECT * FROM leakgallery_watch_history ORDER BY watchDate DESC")
    fun loadAll(): Flow<List<LeakGalleryHistoryEntity>>

    @Query("SELECT * FROM leakgallery_watch_history ORDER BY watchDate DESC")
    suspend fun getAllHistorySync(): List<LeakGalleryHistoryEntity>

    @Query("SELECT * FROM leakgallery_watch_history ORDER BY watchDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<LeakGalleryHistoryEntity>

    @Query("SELECT COUNT(*) FROM leakgallery_watch_history")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM leakgallery_watch_history")
    suspend fun deleteAll()

    @Query("DELETE FROM leakgallery_watch_history WHERE videoCode = :videoCode")
    suspend fun deleteByVideoCode(videoCode: String)

    @Query("SELECT * FROM leakgallery_watch_history WHERE videoCode = :videoCode LIMIT 1")
    suspend fun getByVideoCode(videoCode: String): LeakGalleryHistoryEntity?
}
