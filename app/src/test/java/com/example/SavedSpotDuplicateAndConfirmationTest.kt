package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SavedPlace
import com.example.data.repository.CurbRepository
import com.example.data.repository.SavePlaceResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
class SavedSpotDuplicateAndConfirmationTest {

    private lateinit var context: Context
    private lateinit var repository: CurbRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = CurbRepository(context)
    }

    @Test
    fun testScenarioA_saveRealValidSign() = runBlocking {
        val place = SavedPlace(
            name = "Main St Spot",
            address = "100 Main St, San Francisco, CA",
            parkingRuleSummary = "2 HR PARKING 8AM-6PM",
            parkingSchedule = "8AM - 6PM",
            parkingVerdict = "ALLOWED"
        )
        val result = repository.saveSavedPlaceWithCheck(place)
        assertTrue(result is SavePlaceResult.Success)
        val inserted = (result as SavePlaceResult.Success).savedPlace
        assertTrue(inserted.id > 0)
    }

    @Test
    fun testScenarioB_duplicateSaveBlocked() = runBlocking {
        val place1 = SavedPlace(
            name = "Unique Main St Spot",
            address = "101 Main St, San Francisco, CA",
            parkingRuleSummary = "2 HR PARKING 8AM-6PM",
            parkingSchedule = "8AM - 6PM",
            parkingVerdict = "ALLOWED"
        )
        val result1 = repository.saveSavedPlaceWithCheck(place1)
        assertTrue(result1 is SavePlaceResult.Success)

        val place2 = SavedPlace(
            name = "Unique Main St Spot",
            address = "101 Main St, San Francisco, CA",
            parkingRuleSummary = "2 HR PARKING 8AM-6PM",
            parkingSchedule = "8AM - 6PM",
            parkingVerdict = "ALLOWED"
        )
        val result2 = repository.saveSavedPlaceWithCheck(place2)
        assertTrue(result2 is SavePlaceResult.Duplicate)
        val duplicate = (result2 as SavePlaceResult.Duplicate).existingPlace
        assertEquals((result1 as SavePlaceResult.Success).savedPlace.id, duplicate.id)
    }

    @Test
    fun testScenarioC_sameSignAtDifferentLocationAllowed() = runBlocking {
        val place1 = SavedPlace(
            name = "Spot Alpha",
            address = "200 First Ave, San Francisco, CA",
            parkingRuleSummary = "2 HR PARKING 8AM-6PM",
            parkingSchedule = "8AM - 6PM",
            parkingVerdict = "ALLOWED"
        )
        val result1 = repository.saveSavedPlaceWithCheck(place1)
        assertTrue(result1 is SavePlaceResult.Success)

        val place2 = SavedPlace(
            name = "Spot Beta",
            address = "900 Ninth Ave, Oakland, CA",
            parkingRuleSummary = "2 HR PARKING 8AM-6PM",
            parkingSchedule = "8AM - 6PM",
            parkingVerdict = "ALLOWED"
        )
        val result2 = repository.saveSavedPlaceWithCheck(place2)
        assertTrue(result2 is SavePlaceResult.Success)
    }

    @Test
    fun testScenarioD_sameLocationChangedRuleAllowed() = runBlocking {
        val place1 = SavedPlace(
            name = "Market Spot",
            address = "300 Market St, San Francisco, CA",
            parkingRuleSummary = "NO PARKING ANYTIME",
            parkingSchedule = "ALL TIMES",
            parkingVerdict = "NOT_ALLOWED"
        )
        val result1 = repository.saveSavedPlaceWithCheck(place1)
        assertTrue(result1 is SavePlaceResult.Success)

        val place2 = SavedPlace(
            name = "Market Spot Updated",
            address = "300 Market St, San Francisco, CA",
            parkingRuleSummary = "2 HR PARKING 8AM-6PM",
            parkingSchedule = "8AM - 6PM",
            parkingVerdict = "ALLOWED"
        )
        val result2 = repository.saveSavedPlaceWithCheck(place2)
        assertTrue(result2 is SavePlaceResult.Success)
    }

    @Test
    fun testScenarioH_I_deleteConfirmationFlow() = runBlocking {
        val place = SavedPlace(
            name = "Delete Test Spot",
            address = "888 Howard St",
            parkingRuleSummary = "NO PARKING",
            parkingSchedule = "MON-FRI",
            parkingVerdict = "NOT_ALLOWED"
        )
        val result = repository.saveSavedPlaceWithCheck(place)
        assertTrue(result is SavePlaceResult.Success)
        val id = (result as SavePlaceResult.Success).savedPlace.id

        val beforeDelete = repository.getSavedPlaceById(id)
        assertNotNull(beforeDelete)

        // Delete step
        repository.deleteSavedPlace(id)
        val afterDelete = repository.getSavedPlaceById(id)
        assertNull(afterDelete)
    }

    @Test
    fun testScenarioJ_rapidDoubleTapSaveOnlyOneRecord() = runBlocking {
        val place1 = SavedPlace(
            name = "Rapid Tap Spot",
            address = "999 Mission St",
            parkingRuleSummary = "2 HR PARKING",
            parkingSchedule = "8AM-6PM",
            parkingVerdict = "ALLOWED"
        )
        val place2 = place1.copy()

        val deferred1 = async { repository.saveSavedPlaceWithCheck(place1) }
        val deferred2 = async { repository.saveSavedPlaceWithCheck(place2) }

        val results = awaitAll(deferred1, deferred2)
        val successCount = results.count { it is SavePlaceResult.Success }
        val duplicateCount = results.count { it is SavePlaceResult.Duplicate }

        assertEquals(1, successCount)
        assertEquals(1, duplicateCount)
    }
}
