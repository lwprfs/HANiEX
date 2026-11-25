package com.yenaly.han1meviewer.SitesHub.LeakGallery.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LeakGalleryHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LeakGalleryDatabase : RoomDatabase() {
    abstract fun leakgalleryHistoryDao(): LeakGalleryHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: LeakGalleryDatabase? = null

        fun getInstance(context: Context): LeakGalleryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LeakGalleryDatabase::class.java,
                    "leakgallery_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
