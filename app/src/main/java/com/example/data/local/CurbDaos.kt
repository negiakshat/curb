package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_results WHERE isDemo = 0 ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE isDemo = 1 ORDER BY timestamp DESC")
    fun getDemoScans(): Flow<List<ScanResultEntity>>

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
    @Query("SELECT * FROM parking_sessions WHERE isActive = 1 AND isDemo = 0 ORDER BY endTime ASC LIMIT 1")
    fun getActiveSession(): Flow<ParkingSessionEntity?>

    @Query("SELECT * FROM parking_sessions WHERE isActive = 1 AND isDemo = 1 ORDER BY endTime ASC LIMIT 1")
    fun getDemoActiveSession(): Flow<ParkingSessionEntity?>

    @Query("SELECT * FROM parking_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): ParkingSessionEntity?

    @Query("SELECT * FROM parking_sessions WHERE isDemo = 0 ORDER BY startTime DESC")
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

    @Query("UPDATE parking_sessions SET isActive = 0 WHERE isDemo = 0")
    suspend fun endAllRealSessions()

    @Query("UPDATE parking_sessions SET isActive = 0 WHERE isDemo = 1")
    suspend fun endAllDemoSessions()

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

@Dao
interface NoteDao {
    @Query("SELECT * FROM curb_notes")
    fun getAllNotes(): Flow<List<CurbNoteEntity>>

    @Query("SELECT * FROM curb_notes WHERE targetType = :targetType AND targetId = :targetId LIMIT 1")
    fun getNoteFlow(targetType: String, targetId: Long): Flow<CurbNoteEntity?>

    @Query("SELECT * FROM curb_notes WHERE targetType = :targetType AND targetId = :targetId LIMIT 1")
    suspend fun getNote(targetType: String, targetId: Long): CurbNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: CurbNoteEntity): Long

    @Query("DELETE FROM curb_notes WHERE targetType = :targetType AND targetId = :targetId")
    suspend fun deleteNoteByTarget(targetType: String, targetId: Long)

    @Query("DELETE FROM curb_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM curb_notes WHERE targetType = 'SAVED_PLACE' AND targetId = :placeId")
    suspend fun deleteNoteForSavedPlace(placeId: Long)

    @Query("DELETE FROM curb_notes WHERE targetType = 'SCAN_RESULT' AND targetId = :scanId")
    suspend fun deleteNoteForScanResult(scanId: Long)

    @Query("DELETE FROM curb_notes")
    suspend fun clearAllNotes()
}

@Dao
interface ParkingSpotDao {
    @Query("SELECT * FROM parking_spots WHERE isActive = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getActiveParkingSpot(): Flow<ParkingSpotEntity?>

    @Query("SELECT * FROM parking_spots WHERE isActive = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getActiveParkingSpotDirect(): ParkingSpotEntity?

    @Query("SELECT * FROM parking_spots WHERE sessionId = :sessionId AND isActive = 1 LIMIT 1")
    fun getParkingSpotForSession(sessionId: Long): Flow<ParkingSpotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParkingSpot(spot: ParkingSpotEntity): Long

    @Query("UPDATE parking_spots SET isActive = 0")
    suspend fun clearActiveSpots()

    @Query("DELETE FROM parking_spots")
    suspend fun clearAllSpots()
}

