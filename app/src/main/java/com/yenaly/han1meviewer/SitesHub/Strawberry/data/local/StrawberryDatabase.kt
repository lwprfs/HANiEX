package com.yenaly.han1meviewer.SitesHub.Strawberry.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StrawberryHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class StrawberryDatabase : RoomDatabase() {

    abstract fun strawberryHistoryDao(): StrawberryHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: StrawberryDatabase? = null

        fun getInstance(context: Context): StrawberryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrawberryDatabase::class.java,
                    "strawberry_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
