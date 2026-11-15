package com.yenaly.han1meviewer.Buondua.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BuonduaHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: BuonduaHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<BuonduaHistoryEntity>)

    @Query("SELECT * FROM buondua_watch_history ORDER BY watchDate DESC")
    suspend fun getAllHistorySync(): List<BuonduaHistoryEntity>

    @Query("SELECT * FROM buondua_watch_history ORDER BY watchDate DESC")
    fun loadAll(): Flow<List<BuonduaHistoryEntity>>

    @Query("SELECT * FROM buondua_watch_history ORDER BY watchDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<BuonduaHistoryEntity>

    @Query("DELETE FROM buondua_watch_history")
    suspend fun deleteAll()

    @Query("DELETE FROM buondua_watch_history WHERE videoCode = :videoCode")
    suspend fun deleteByVideoCode(videoCode: String)

    @Query("SELECT * FROM buondua_watch_history WHERE videoCode = :videoCode LIMIT 1")
    suspend fun getByVideoCode(videoCode: String): BuonduaHistoryEntity?
}
