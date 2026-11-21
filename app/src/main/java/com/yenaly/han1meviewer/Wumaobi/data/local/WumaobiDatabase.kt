package com.yenaly.han1meviewer.Wumaobi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WumaobiHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WumaobiDatabase : RoomDatabase() {
    abstract fun wumaobiHistoryDao(): WumaobiHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: WumaobiDatabase? = null

        fun getInstance(context: Context): WumaobiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WumaobiDatabase::class.java,
                    "wumaobi_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
