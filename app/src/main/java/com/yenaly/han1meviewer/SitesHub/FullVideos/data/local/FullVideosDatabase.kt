package com.yenaly.han1meviewer.SitesHub.FullVideos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FullVideosHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FullVideosDatabase : RoomDatabase() {
    abstract fun fullVideosHistoryDao(): FullVideosHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: FullVideosDatabase? = null

        fun getInstance(context: Context): FullVideosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FullVideosDatabase::class.java,
                    "fullvideos_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
