package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): ScanResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResultEntity): Long

    @Query("DELETE FROM scan_results WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scan_results")
    suspend fun clearAllScans()
}

@Dao
interface ParkingSessionDao {
    @Query("SELECT * FROM parking_sessions WHERE isActive = 1 ORDER BY endTime ASC LIMIT 1")
    fun getActiveSession(): Flow<ParkingSessionEntity?>

    @Query("SELECT * FROM parking_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ParkingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ParkingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ParkingSessionEntity)

    @Query("UPDATE parking_sessions SET endTime = endTime + :additionalMillis, allowedUntilTime = :newAllowedUntil WHERE id = :id")
    suspend fun extendSession(id: Long, additionalMillis: Long, newAllowedUntil: String)

    @Query("UPDATE parking_sessions SET reminderMinutesBefore = :minutes WHERE id = :id")
    suspend fun updateReminder(id: Long, minutes: Int)

    @Query("UPDATE parking_sessions SET isActive = 0 WHERE id = :id")
    suspend fun endSession(id: Long)

    @Query("UPDATE parking_sessions SET isActive = 0")
    suspend fun endAllSessions()

    @Query("DELETE FROM parking_sessions")
    suspend fun clearAllSessions()
}

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY timestamp DESC")
    fun getAllSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlaceEntity): Long

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deletePlaceById(id: Long)

    @Query("DELETE FROM saved_places")
    suspend fun clearAllSavedPlaces()
}
