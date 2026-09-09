package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScanResultEntity::class,
        ParkingSessionEntity::class,
        SavedPlaceEntity::class,
        CurbNoteEntity::class,
        ParkingSpotEntity::class
    ],
    version = 7,
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE parking_spots ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): CurbDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CurbDatabase::class.java,
                    "curb_parking_database"
                )
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
