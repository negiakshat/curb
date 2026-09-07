package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScanResultEntity::class,
        ParkingSessionEntity::class,
        SavedPlaceEntity::class,
        CurbNoteEntity::class,
        ParkingSpotEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CurbDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun parkingSessionDao(): ParkingSessionDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun noteDao(): NoteDao
    abstract fun parkingSpotDao(): ParkingSpotDao

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
