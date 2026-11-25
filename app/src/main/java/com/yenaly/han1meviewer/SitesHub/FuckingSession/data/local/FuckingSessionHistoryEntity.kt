package com.yenaly.han1meviewer.SitesHub.FuckingSession.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "fuckingsession_watch_history")
data class FuckingSessionHistoryEntity(
    @PrimaryKey
    val videoCode: String,
    val title: String,
    val coverUrl: String,
    val watchDate: Long,
    val watchDuration: Long = 0,
    val lastPosition: Long = 0,
    val totalDuration: Long = 0,
    val watchCount: Int = 1,
    val playCount: Int = 0,
    val isPlayed: Boolean = false,
    val lastPlayedDate: Long? = null
)
