package com.yenaly.han1meviewer.Cosplaytele.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CosplayteleHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CosplayteleDatabase : RoomDatabase() {
    abstract fun cosplayteleHistoryDao(): CosplayteleHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CosplayteleDatabase? = null

        fun getInstance(context: Context): CosplayteleDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CosplayteleDatabase::class.java,
                    "cosplaytele_history.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
