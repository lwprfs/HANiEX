package com.yenaly.han1meviewer.SitesHub.Strawberry.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "strawberry_watch_history")
data class StrawberryHistoryEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val coverUrl: String,
    val uploaderName: String?,
    val lastPosition: Long,
    val totalDuration: Long,
    val watchDate: Long,
    val watchDuration: Long = 0L,
    val watchCount: Int = 1,
    val completed: Boolean = false,
    val lastQuality: String? = null,
)
