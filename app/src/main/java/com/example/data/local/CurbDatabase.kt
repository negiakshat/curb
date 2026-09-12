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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_results ADD COLUMN imageUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE scan_results ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE scan_results SET verdict = 'AMBIGUOUS' WHERE verdict NOT IN ('ALLOWED', 'RESTRICTED', 'AMBIGUOUS')")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE parking_sessions ADD COLUMN maxAllowedEndTimeMillis INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE parking_sessions ADD COLUMN timerMode TEXT NOT NULL DEFAULT 'TIMED_LIMIT'")
                db.execSQL("ALTER TABLE parking_sessions ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_places (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        address TEXT NOT NULL,
                        parkingNote TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS curb_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        targetType TEXT NOT NULL,
                        targetId INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_curb_notes_targetType_targetId ON curb_notes (targetType, targetId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS parking_spots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        accuracy REAL,
                        locationName TEXT NOT NULL DEFAULT '',
                        sessionId INTEGER,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
            }
        }

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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
