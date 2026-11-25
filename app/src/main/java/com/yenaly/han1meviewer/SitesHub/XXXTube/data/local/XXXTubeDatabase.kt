package com.yenaly.han1meviewer.SitesHub.XXXTube.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [XXXTubeHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class XXXTubeDatabase : RoomDatabase() {
    abstract fun xxxTubeHistoryDao(): XXXTubeHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: XXXTubeDatabase? = null

        fun getInstance(context: Context): XXXTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XXXTubeDatabase::class.java,
                    "xxxtube_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
