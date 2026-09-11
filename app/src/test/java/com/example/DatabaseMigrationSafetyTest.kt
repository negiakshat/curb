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
    private val dbName = "test_curb_migration.db"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `migrate from version 1 to current version 7 preserves data and safety defaults`() = runBlocking {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
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
                            timerBasis TEXT NOT NULL DEFAULT '',
                            parkingRuleSummary TEXT NOT NULL DEFAULT '',
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

        // Open database via Room applying all migrations (1..7) without destructive fallback
        val migratedDb = Room.databaseBuilder(context, CurbDatabase::class.java, dbName)
            .addMigrations(
                CurbDatabase.MIGRATION_1_2,
                CurbDatabase.MIGRATION_2_3,
                CurbDatabase.MIGRATION_3_4,
                CurbDatabase.MIGRATION_4_5,
                CurbDatabase.MIGRATION_5_6,
                CurbDatabase.MIGRATION_6_7
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
    }
}
