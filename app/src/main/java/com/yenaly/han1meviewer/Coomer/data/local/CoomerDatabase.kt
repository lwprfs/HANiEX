package com.yenaly.han1meviewer.Coomer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CoomerHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CoomerDatabase : RoomDatabase() {
    abstract fun coomerHistoryDao(): CoomerHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CoomerDatabase? = null

        fun getInstance(context: Context): CoomerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoomerDatabase::class.java,
                    "coomer_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
