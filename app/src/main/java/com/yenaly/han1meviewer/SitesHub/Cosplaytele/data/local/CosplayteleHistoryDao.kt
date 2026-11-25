package com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CosplayteleHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<CosplayteleHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: CosplayteleHistoryEntity)

    @Query("SELECT * FROM cosplaytele_watch_history ORDER BY watchDate DESC")
    fun loadAll(): Flow<List<CosplayteleHistoryEntity>>

    @Query("SELECT * FROM cosplaytele_watch_history ORDER BY watchDate DESC")
    suspend fun getAllHistorySync(): List<CosplayteleHistoryEntity>

    @Query("SELECT * FROM cosplaytele_watch_history ORDER BY watchDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<CosplayteleHistoryEntity>

    @Query("SELECT COUNT(*) FROM cosplaytele_watch_history")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM cosplaytele_watch_history")
    suspend fun deleteAll()

    @Query("DELETE FROM cosplaytele_watch_history WHERE videoCode = :videoCode")
    suspend fun deleteByVideoCode(videoCode: String)

    @Query("SELECT * FROM cosplaytele_watch_history WHERE videoCode = :videoCode LIMIT 1")
    suspend fun getByVideoCode(videoCode: String): CosplayteleHistoryEntity?
}
