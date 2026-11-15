package com.yenaly.han1meviewer.Buondua.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BuonduaHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class BuonduaDatabase : RoomDatabase() {
    abstract fun buonduaHistoryDao(): BuonduaHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: BuonduaDatabase? = null

        fun getInstance(context: Context): BuonduaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BuonduaDatabase::class.java,
                    "buondua_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
