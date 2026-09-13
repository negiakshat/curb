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
                db.execSQL("ALTER TABLE scan_results ADD COLUMN imageUri TEXT")
                db.execSQL("ALTER TABLE scan_results ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE scan_results SET verdict = 'AMBIGUOUS' WHERE verdict NOT IN ('ALLOWED', 'RESTRICTED', 'AMBIGUOUS')")
                rebuildScanResults(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE parking_sessions ADD COLUMN maxAllowedEndTimeMillis INTEGER")
                db.execSQL("ALTER TABLE parking_sessions ADD COLUMN timerMode TEXT NOT NULL DEFAULT 'TIMED_LIMIT'")
                db.execSQL("ALTER TABLE parking_sessions ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                rebuildParkingSessions(db)
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
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_curb_notes_targetType_targetId " +
                        "ON curb_notes (targetType, targetId)"
                )
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
                        locationName TEXT NOT NULL,
                        sessionId INTEGER,
                        isActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE parking_spots ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                rebuildParkingSpots(db)
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

        private fun rebuildScanResults(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE scan_results_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    locationName TEXT NOT NULL,
                    cityState TEXT NOT NULL,
                    verdict TEXT NOT NULL,
                    statusChipText TEXT NOT NULL,
                    allowedUntilTime TEXT NOT NULL,
                    timeRemaining TEXT NOT NULL,
                    parkingRulesJson TEXT NOT NULL,
                    explanation TEXT NOT NULL,
                    detectedSignsJson TEXT NOT NULL,
                    zoneType TEXT NOT NULL,
                    paymentInfo TEXT NOT NULL,
                    vehicleApplicability TEXT NOT NULL,
                    imageUri TEXT,
                    isDemo INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO scan_results_new (
                    id, timestamp, locationName, cityState, verdict, statusChipText,
                    allowedUntilTime, timeRemaining, parkingRulesJson, explanation,
                    detectedSignsJson, zoneType, paymentInfo, vehicleApplicability,
                    imageUri, isDemo
                )
                SELECT id, timestamp, locationName, cityState, verdict, statusChipText,
                       allowedUntilTime, timeRemaining, parkingRulesJson, explanation,
                       detectedSignsJson, zoneType, paymentInfo, vehicleApplicability,
                       imageUri, isDemo
                FROM scan_results
                """.trimIndent()
            )
            db.execSQL("DROP TABLE scan_results")
            db.execSQL("ALTER TABLE scan_results_new RENAME TO scan_results")
        }

        private fun rebuildParkingSessions(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE parking_sessions_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    scanResultId INTEGER NOT NULL,
                    locationName TEXT NOT NULL,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER NOT NULL,
                    allowedUntilTime TEXT NOT NULL,
                    reminderMinutesBefore INTEGER NOT NULL,
                    notes TEXT NOT NULL,
                    timerBasis TEXT NOT NULL,
                    parkingRuleSummary TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    maxAllowedEndTimeMillis INTEGER,
                    timerMode TEXT NOT NULL,
                    isDemo INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO parking_sessions_new (
                    id, scanResultId, locationName, startTime, endTime, allowedUntilTime,
                    reminderMinutesBefore, notes, timerBasis, parkingRuleSummary, isActive,
                    maxAllowedEndTimeMillis, timerMode, isDemo
                )
                SELECT id, scanResultId, locationName, startTime, endTime, allowedUntilTime,
                       reminderMinutesBefore, notes, timerBasis, parkingRuleSummary, isActive,
                       maxAllowedEndTimeMillis, timerMode, isDemo
                FROM parking_sessions
                """.trimIndent()
            )
            db.execSQL("DROP TABLE parking_sessions")
            db.execSQL("ALTER TABLE parking_sessions_new RENAME TO parking_sessions")
        }

        private fun rebuildParkingSpots(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE parking_spots_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    timestamp INTEGER NOT NULL,
                    accuracy REAL,
                    locationName TEXT NOT NULL,
                    sessionId INTEGER,
                    isActive INTEGER NOT NULL,
                    isDemo INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO parking_spots_new (
                    id, latitude, longitude, timestamp, accuracy, locationName,
                    sessionId, isActive, isDemo
                )
                SELECT id, latitude, longitude, timestamp, accuracy, locationName,
                       sessionId, isActive, isDemo
                FROM parking_spots
                """.trimIndent()
            )
            db.execSQL("DROP TABLE parking_spots")
            db.execSQL("ALTER TABLE parking_spots_new RENAME TO parking_spots")
        }
    }
}
