package com.yenaly.han1meviewer.SitesHub.FuckingSession.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FuckingSessionHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<FuckingSessionHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: FuckingSessionHistoryEntity)

    @Query("SELECT * FROM fuckingsession_watch_history ORDER BY watchDate DESC")
    fun loadAll(): Flow<List<FuckingSessionHistoryEntity>>

    @Query("SELECT * FROM fuckingsession_watch_history ORDER BY watchDate DESC")
    suspend fun getAllHistorySync(): List<FuckingSessionHistoryEntity>

    @Query("SELECT * FROM fuckingsession_watch_history ORDER BY watchDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<FuckingSessionHistoryEntity>

    @Query("SELECT COUNT(*) FROM fuckingsession_watch_history")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM fuckingsession_watch_history")
    suspend fun deleteAll()

    @Query("DELETE FROM fuckingsession_watch_history WHERE videoCode = :videoCode")
    suspend fun deleteByVideoCode(videoCode: String)

    @Query("SELECT * FROM fuckingsession_watch_history WHERE videoCode = :videoCode LIMIT 1")
    suspend fun getByVideoCode(videoCode: String): FuckingSessionHistoryEntity?
}
