package com.yenaly.han1meviewer.SitesHub.Coser.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CoserHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CoserDatabase : RoomDatabase() {
    abstract fun coserHistoryDao(): CoserHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CoserDatabase? = null

        fun getInstance(context: Context): CoserDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoserDatabase::class.java,
                    "coser_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
