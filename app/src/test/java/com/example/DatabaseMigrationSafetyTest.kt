package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CurbDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationSafetyTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testMigrateFromV1ToV7PreservesData() {
        runBlocking {
            val helperFactory = FrameworkSQLiteOpenHelperFactory()
            val dbName = "test_curb_migration.db"
            context.deleteDatabase(dbName)
            val configuration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE scan_results (
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
                                vehicleApplicability TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE parking_sessions (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                scanResultId INTEGER NOT NULL DEFAULT 0,
                                locationName TEXT NOT NULL,
                                startTime INTEGER NOT NULL,
                                endTime INTEGER NOT NULL,
                                allowedUntilTime TEXT NOT NULL,
                                reminderMinutesBefore INTEGER NOT NULL DEFAULT 15,
                                notes TEXT NOT NULL DEFAULT '',
                                isActive INTEGER NOT NULL DEFAULT 1
                            )
                            """.trimIndent()
                        )

                        // Insert real scan 1 with valid RESTRICTED verdict
                        db.execSQL(
                            """
                            INSERT INTO scan_results (
                                timestamp, locationName, cityState, verdict, statusChipText,
                                allowedUntilTime, timeRemaining, parkingRulesJson, explanation,
                                detectedSignsJson, zoneType, paymentInfo, vehicleApplicability
                            ) VALUES (
                                1000, 'Main St', 'San Francisco, CA', 'RESTRICTED', 'No Parking',
                                '18:00', '0m', '[]', 'No parking allowed', '[]', 'Zone A', 'None', 'All'
                            )
                            """.trimIndent()
                        )

                        // Insert real scan 2 with invalid legacy verdict to verify safety normalization
                        db.execSQL(
                            """
                            INSERT INTO scan_results (
                                timestamp, locationName, cityState, verdict, statusChipText,
                                allowedUntilTime, timeRemaining, parkingRulesJson, explanation,
                                detectedSignsJson, zoneType, paymentInfo, vehicleApplicability
                            ) VALUES (
                                2000, 'Broadway', 'San Francisco, CA', 'INVALID_CORRUPT_STATUS', 'Unknown',
                                '12:00', '1h', '[]', 'Corrupt row', '[]', 'Zone B', 'None', 'All'
                            )
                            """.trimIndent()
                        )

                        // Insert real session
                        db.execSQL(
                            """
                            INSERT INTO parking_sessions (
                                scanResultId, locationName, startTime, endTime, allowedUntilTime
                            ) VALUES (
                                1, 'Main St', 1000, 5000, '18:00'
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()

            val oldDb = helperFactory.create(configuration).writableDatabase
            oldDb.close()

            // Open database via Room applying all migrations (1..9) without destructive fallback
            val migratedDb = Room.databaseBuilder(context, CurbDatabase::class.java, dbName)
                .addMigrations(
                    CurbDatabase.MIGRATION_1_2,
                    CurbDatabase.MIGRATION_2_3,
                    CurbDatabase.MIGRATION_3_4,
                    CurbDatabase.MIGRATION_4_5,
                    CurbDatabase.MIGRATION_5_6,
                    CurbDatabase.MIGRATION_6_7,
                    CurbDatabase.MIGRATION_7_8,
                    CurbDatabase.MIGRATION_8_9
                )
                .allowMainThreadQueries()
                .build()

            val scans = migratedDb.scanDao().getAllScans().first()
            assertEquals(2, scans.size)

            val scan1 = scans.find { it.id == 1L }
            assertNotNull(scan1)
            assertEquals("Main St", scan1?.locationName)
            assertEquals("RESTRICTED", scan1?.verdict)
            assertNull(scan1?.imageUri)
            assertFalse(scan1?.isDemo ?: true)

            val scan2 = scans.find { it.id == 2L }
            assertNotNull(scan2)
            assertEquals("Broadway", scan2?.locationName)
            assertEquals("AMBIGUOUS", scan2?.verdict) // Normalized to AMBIGUOUS, NOT ALLOWED

            val session = migratedDb.parkingSessionDao().getSessionById(1L)
            assertNotNull(session)
            assertEquals("Main St", session?.locationName)
            assertEquals("TIMED_LIMIT", session?.timerMode)
            assertNull(session?.maxAllowedEndTimeMillis)
            assertFalse(session?.isDemo ?: true)

            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE scan_results (
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
                vehicleApplicability TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE parking_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                scanResultId INTEGER NOT NULL DEFAULT 0,
                locationName TEXT NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL,
                allowedUntilTime TEXT NOT NULL,
                reminderMinutesBefore INTEGER NOT NULL DEFAULT 15,
                notes TEXT NOT NULL DEFAULT '',
                isActive INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
    }

    @Test
    fun testFreshDatabaseHasCurrentSchema() {
        val freshDb = Room.inMemoryDatabaseBuilder(context, CurbDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val db = freshDb.openHelper.writableDatabase

        assertNull(columnDefault(db, "scan_results", "imageUri"))
        assertNull(columnDefault(db, "scan_results", "isDemo"))
        assertNull(columnDefault(db, "parking_sessions", "timerMode"))
        assertNull(columnDefault(db, "parking_sessions", "isDemo"))
        assertNull(columnDefault(db, "parking_spots", "locationName"))
        assertNull(columnDefault(db, "parking_spots", "isActive"))
        assertNull(columnDefault(db, "parking_spots", "isDemo"))
        assertEquals(listOf("targetType", "targetId"), uniqueIndexColumns(db, "curb_notes"))
        freshDb.close()
    }

    @Test
    fun testExplicitMigrationsApplySequentially() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val stepDbName = "test_curb_stepwise.db"
        context.deleteDatabase(stepDbName)
        val configuration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(stepDbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = createVersion1Schema(db)
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = helperFactory.create(configuration)
        val db = helper.writableDatabase

        CurbDatabase.MIGRATION_1_2.migrate(db)
        assertNull(columnDefault(db, "scan_results", "imageUri"))
        assertNull(columnDefault(db, "scan_results", "isDemo"))

        CurbDatabase.MIGRATION_2_3.migrate(db)
        assertNull(columnDefault(db, "parking_sessions", "timerMode"))
        assertNull(columnDefault(db, "parking_sessions", "isDemo"))

        CurbDatabase.MIGRATION_3_4.migrate(db)
        assertTrue(tableExists(db, "saved_places"))

        CurbDatabase.MIGRATION_4_5.migrate(db)
        assertEquals(listOf("targetType", "targetId"), uniqueIndexColumns(db, "curb_notes"))

        CurbDatabase.MIGRATION_5_6.migrate(db)
        assertNull(columnDefault(db, "parking_spots", "locationName"))
        assertNull(columnDefault(db, "parking_spots", "isActive"))

        CurbDatabase.MIGRATION_6_7.migrate(db)
        assertNull(columnDefault(db, "parking_spots", "isDemo"))

        CurbDatabase.MIGRATION_7_8.migrate(db)
        assertTrue(hasColumn(db, "parking_sessions", "timerBasis"))
        assertTrue(hasColumn(db, "parking_sessions", "parkingRuleSummary"))

        CurbDatabase.MIGRATION_8_9.migrate(db)
        assertTrue(hasColumn(db, "saved_places", "parkingRuleSummary"))
        assertTrue(hasColumn(db, "saved_places", "lastCheckedAt"))
        helper.close()
        context.deleteDatabase(stepDbName)
    }

    @Test
    fun testMigrateV7WithoutParkingRuleSummaryToV8() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val dbName = "test_curb_v7_without_summary.db"
        context.deleteDatabase(dbName)
        val configuration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE parking_sessions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            scanResultId INTEGER NOT NULL DEFAULT 0,
                            locationName TEXT NOT NULL,
                            startTime INTEGER NOT NULL,
                            endTime INTEGER NOT NULL,
                            allowedUntilTime TEXT NOT NULL,
                            reminderMinutesBefore INTEGER NOT NULL DEFAULT 15,
                            notes TEXT NOT NULL DEFAULT '',
                            timerBasis TEXT NOT NULL DEFAULT '',
                            isActive INTEGER NOT NULL DEFAULT 1,
                            maxAllowedEndTimeMillis INTEGER,
                            timerMode TEXT NOT NULL DEFAULT 'TIMED_LIMIT',
                            isDemo INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO parking_sessions (
                            id, scanResultId, locationName, startTime, endTime, allowedUntilTime,
                            reminderMinutesBefore, notes, timerBasis, isActive, maxAllowedEndTimeMillis,
                            timerMode, isDemo
                        ) VALUES (
                            1, 42, 'Market St', 100000, 200000, '17:00',
                            10, 'Parked near cafe', '2 Hour Limit', 1, 200000,
                            'TIMED_LIMIT', 0
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = helperFactory.create(configuration)
        val db = helper.writableDatabase

        // Assert column does NOT exist in legacy v7 schema
        assertFalse(hasColumn(db, "parking_sessions", "parkingRuleSummary"))

        // Run migration 7 -> 8
        CurbDatabase.MIGRATION_7_8.migrate(db)

        // Assert column exists after migration
        assertTrue(hasColumn(db, "parking_sessions", "parkingRuleSummary"))
        assertTrue(hasColumn(db, "parking_sessions", "timerBasis"))

        // Assert data is preserved and parkingRuleSummary migrated as empty string
        db.query("SELECT id, scanResultId, locationName, startTime, endTime, allowedUntilTime, reminderMinutesBefore, notes, timerBasis, parkingRuleSummary, isActive, maxAllowedEndTimeMillis, timerMode, isDemo FROM parking_sessions WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals(42L, cursor.getLong(1))
            assertEquals("Market St", cursor.getString(2))
            assertEquals(100000L, cursor.getLong(3))
            assertEquals(200000L, cursor.getLong(4))
            assertEquals("17:00", cursor.getString(5))
            assertEquals(10, cursor.getInt(6))
            assertEquals("Parked near cafe", cursor.getString(7))
            assertEquals("2 Hour Limit", cursor.getString(8))
            assertEquals("", cursor.getString(9)) // migrated default value is ""
            assertEquals(1, cursor.getInt(10))
            assertEquals(200000L, cursor.getLong(11))
            assertEquals("TIMED_LIMIT", cursor.getString(12))
            assertEquals(0, cursor.getInt(13))
        }

        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigrateFromV1WithPartialSchema() {
        runBlocking {
            val helperFactory = FrameworkSQLiteOpenHelperFactory()
            val partialDbName = "test_curb_partial.db"
            context.deleteDatabase(partialDbName)
            val configuration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(partialDbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Create v1 schema PLUS imageUri and isDemo columns manually (the crash scenario)
                        db.execSQL(
                            """
                            CREATE TABLE scan_results (
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
                                isDemo INTEGER NOT NULL DEFAULT 0
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE parking_sessions (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                scanResultId INTEGER NOT NULL DEFAULT 0,
                                locationName TEXT NOT NULL,
                                startTime INTEGER NOT NULL,
                                endTime INTEGER NOT NULL,
                                allowedUntilTime TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        // Insert data with imageUri and isDemo to ensure they're preserved
                        db.execSQL(
                            """
                            INSERT INTO scan_results (
                                timestamp, locationName, cityState, verdict, statusChipText,
                                allowedUntilTime, timeRemaining, parkingRulesJson, explanation,
                                detectedSignsJson, zoneType, paymentInfo, vehicleApplicability,
                                imageUri, isDemo
                            ) VALUES (
                                1000, 'Main St', 'San Francisco, CA', 'ALLOWED', 'Parking OK',
                                '20:00', '2h', '[]', 'All good', '[]', 'Zone A', 'None', 'All',
                                'content://media/external/images/media/1', 1
                            )
                            """.trimIndent()
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()

            val helper = helperFactory.create(configuration)
            val db = helper.writableDatabase

            // This should not crash even though imageUri and isDemo already exist
            CurbDatabase.MIGRATION_1_2.migrate(db)

            // Verify imageUri and isDemo were preserved through the rebuild
            db.query("SELECT imageUri, isDemo FROM scan_results WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("content://media/external/images/media/1", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }

            helper.close()
            context.deleteDatabase(partialDbName)
        }
    }

    private fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    return true
                }
            }
        }
        return false
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun columnDefault(db: SupportSQLiteDatabase, table: String, column: String): String? {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    return if (cursor.isNull(defaultIndex)) null else cursor.getString(defaultIndex)
                }
            }
        }
        return null
    }

    private fun uniqueIndexColumns(db: SupportSQLiteDatabase, table: String): List<String> {
        var indexName: String? = null
        db.query("PRAGMA index_list(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val uniqueIndex = cursor.getColumnIndexOrThrow("unique")
            while (cursor.moveToNext()) {
                if (cursor.getInt(uniqueIndex) == 1) {
                    indexName = cursor.getString(nameIndex)
                    break
                }
            }
        }
        val selectedIndex = indexName ?: return emptyList()
        db.query("PRAGMA index_info(`$selectedIndex`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
            return columns
        }
    }
}
