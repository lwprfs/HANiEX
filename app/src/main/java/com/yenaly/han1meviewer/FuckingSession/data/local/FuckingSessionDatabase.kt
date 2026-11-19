package com.yenaly.han1meviewer.FuckingSession.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FuckingSessionHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FuckingSessionDatabase : RoomDatabase() {
    abstract fun fuckingSessionHistoryDao(): FuckingSessionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: FuckingSessionDatabase? = null

        fun getInstance(context: Context): FuckingSessionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FuckingSessionDatabase::class.java,
                    "fuckingsession_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
