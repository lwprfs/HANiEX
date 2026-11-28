package com.yenaly.han1meviewer.SitesHub.Strawberry.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StrawberryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<StrawberryHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: StrawberryHistoryEntity)

    @Query("SELECT * FROM strawberry_watch_history ORDER BY watchDate DESC")
    fun loadAll(): Flow<List<StrawberryHistoryEntity>>

    @Query("SELECT * FROM strawberry_watch_history ORDER BY watchDate DESC")
    suspend fun getAll(): List<StrawberryHistoryEntity>

    @Query("SELECT * FROM strawberry_watch_history ORDER BY watchDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<StrawberryHistoryEntity>

    @Query("SELECT COUNT(*) FROM strawberry_watch_history")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM strawberry_watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getByVideoId(videoId: String): StrawberryHistoryEntity?

    @Query("UPDATE strawberry_watch_history SET lastPosition = :position WHERE videoId = :videoId")
    suspend fun updatePosition(videoId: String, position: Long)

    @Query("DELETE FROM strawberry_watch_history WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: String)

    @Query("DELETE FROM strawberry_watch_history")
    suspend fun deleteAll()
}
