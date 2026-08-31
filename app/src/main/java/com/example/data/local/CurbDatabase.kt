package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScanResultEntity::class,
        ParkingSessionEntity::class,
        SavedPlaceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CurbDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun parkingSessionDao(): ParkingSessionDao
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        @Volatile
        private var INSTANCE: CurbDatabase? = null

        fun getDatabase(context: Context): CurbDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CurbDatabase::class.java,
                    "curb_parking_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
