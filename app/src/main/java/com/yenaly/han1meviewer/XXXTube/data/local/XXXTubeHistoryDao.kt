package com.yenaly.han1meviewer.XXXTube.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface XXXTubeHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<XXXTubeHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: XXXTubeHistoryEntity)

    @Query("SELECT * FROM xxxtube_watch_history ORDER BY watchDate DESC")
    fun loadAll(): Flow<List<XXXTubeHistoryEntity>>

    @Query("SELECT * FROM xxxtube_watch_history ORDER BY watchDate DESC")
    suspend fun getAllHistorySync(): List<XXXTubeHistoryEntity>

    @Query("SELECT * FROM xxxtube_watch_history ORDER BY watchDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<XXXTubeHistoryEntity>

    @Query("SELECT COUNT(*) FROM xxxtube_watch_history")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM xxxtube_watch_history")
    suspend fun deleteAll()

    @Query("DELETE FROM xxxtube_watch_history WHERE videoCode = :videoCode")
    suspend fun deleteByVideoCode(videoCode: String)

    @Query("SELECT * FROM xxxtube_watch_history WHERE videoCode = :videoCode LIMIT 1")
    suspend fun getByVideoCode(videoCode: String): XXXTubeHistoryEntity?
}
