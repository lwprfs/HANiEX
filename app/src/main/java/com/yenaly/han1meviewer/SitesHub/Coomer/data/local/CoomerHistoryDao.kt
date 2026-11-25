package com.yenaly.han1meviewer.SitesHub.Coomer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CoomerHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<CoomerHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: CoomerHistoryEntity)

    @Query("SELECT * FROM coomer_watch_history ORDER BY watchDate DESC")
    fun loadAll(): Flow<List<CoomerHistoryEntity>>

    @Query("SELECT * FROM coomer_watch_history ORDER BY watchDate DESC")
    suspend fun getAllHistorySync(): List<CoomerHistoryEntity>

    @Query("SELECT * FROM coomer_watch_history ORDER BY watchDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<CoomerHistoryEntity>

    @Query("SELECT COUNT(*) FROM coomer_watch_history")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM coomer_watch_history")
    suspend fun deleteAll()

    @Query("DELETE FROM coomer_watch_history WHERE videoCode = :videoCode")
    suspend fun deleteByVideoCode(videoCode: String)

    @Query("SELECT * FROM coomer_watch_history WHERE videoCode = :videoCode LIMIT 1")
    suspend fun getByVideoCode(videoCode: String): CoomerHistoryEntity?
}
