package com.yenaly.han1meviewer.FullVideos.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FullVideosHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<FullVideosHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: FullVideosHistoryEntity)

    @Query("SELECT * FROM fullvideos_watch_history ORDER BY watchDate DESC")
    fun loadAll(): Flow<List<FullVideosHistoryEntity>>

    @Query("SELECT * FROM fullvideos_watch_history ORDER BY watchDate DESC")
    suspend fun getAllHistorySync(): List<FullVideosHistoryEntity>

    @Query("SELECT * FROM fullvideos_watch_history ORDER BY watchDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<FullVideosHistoryEntity>

    @Query("SELECT COUNT(*) FROM fullvideos_watch_history")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM fullvideos_watch_history")
    suspend fun deleteAll()

    @Query("DELETE FROM fullvideos_watch_history WHERE videoCode = :videoCode")
    suspend fun deleteByVideoCode(videoCode: String)

    @Query("SELECT * FROM fullvideos_watch_history WHERE videoCode = :videoCode LIMIT 1")
    suspend fun getByVideoCode(videoCode: String): FullVideosHistoryEntity?
}
