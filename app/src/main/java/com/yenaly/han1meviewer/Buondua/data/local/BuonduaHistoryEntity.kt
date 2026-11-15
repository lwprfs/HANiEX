package com.yenaly.han1meviewer.Buondua.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "buondua_watch_history")
data class BuonduaHistoryEntity(
    @PrimaryKey
    val videoCode: String,
    val title: String,
    val coverUrl: String,
    val watchDate: Long,
    val lastPage: Int = 1,
    val watchCount: Int = 1,
)
