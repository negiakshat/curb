package com.example.data.repository

import android.content.Context
import com.example.data.local.CurbDatabase
import com.example.data.local.CurbNoteEntity
import com.example.data.local.ParkingSessionEntity
import com.example.data.local.ParkingSpotEntity
import com.example.data.local.SavedPlaceEntity
import com.example.data.local.ScanResultEntity
import com.example.data.model.ActiveParkingSession
import com.example.data.model.CurbNote
import com.example.data.model.DetectedSign
import com.example.data.model.ParkingSpot
import com.example.data.model.SavedPlace
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CurbRepository(context: Context) {
    private val database = CurbDatabase.getDatabase(context)
    private val scanDao = database.scanDao()
    private val parkingSessionDao = database.parkingSessionDao()
    private val savedPlaceDao = database.savedPlaceDao()
    private val noteDao = database.noteDao()
    private val parkingSpotDao = database.parkingSpotDao()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    private val signListType = Types.newParameterizedType(List::class.java, DetectedSign::class.java)
    private val signListAdapter = moshi.adapter<List<DetectedSign>>(signListType)

    val allScans: Flow<List<ScanResult>> = scanDao.getAllScans().map { entities ->
        entities.map { entityToScanResult(it) }
    }

    val activeSession: Flow<ActiveParkingSession?> = parkingSessionDao.getActiveSession().map { entity ->
        entity?.let { entityToParkingSession(it) }
    }

    val savedParkingSpot: Flow<ParkingSpot?> = parkingSpotDao.getActiveParkingSpot().map { entity ->
        entity?.let { entityToParkingSpot(it) }
    }

    val savedPlaces: Flow<List<SavedPlace>> = savedPlaceDao.getAllSavedPlaces().map { entities ->
        entities.map { entityToSavedPlace(it) }
    }

    val allNotes: Flow<List<CurbNote>> = noteDao.getAllNotes().map { entities ->
        entities.map { entityToCurbNote(it) }
    }

    fun getNoteFlow(targetType: String, targetId: Long): Flow<CurbNote?> {
        return noteDao.getNoteFlow(targetType, targetId).map { it?.let { entityToCurbNote(it) } }
    }

    suspend fun getNote(targetType: String, targetId: Long): CurbNote? {
        val entity = noteDao.getNote(targetType, targetId)
        return entity?.let { entityToCurbNote(it) }
    }

    suspend fun saveNote(targetType: String, targetId: Long, text: String): Long {
        val existing = noteDao.getNote(targetType, targetId)
        val now = System.currentTimeMillis()
        val entity = CurbNoteEntity(
            id = existing?.id ?: 0,
            targetType = targetType,
            targetId = targetId,
            text = text.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        return noteDao.insertOrUpdateNote(entity)
    }

    suspend fun deleteNote(targetType: String, targetId: Long) {
        noteDao.deleteNoteByTarget(targetType, targetId)
    }

    suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
    }

    suspend fun getScanById(id: Long): ScanResult? {
        val entity = scanDao.getScanById(id)
        return entity?.let { entityToScanResult(it) }
    }

    suspend fun saveScan(scan: ScanResult): Long {
        val entity = scanResultToEntity(scan)
        return scanDao.insertScan(entity)
    }

    suspend fun deleteScan(id: Long) {
        scanDao.deleteScanById(id)
        noteDao.deleteNoteForScanResult(id)
    }

    suspend fun startParkingSession(
        scanResultId: Long,
        locationName: String,
        durationMinutes: Int,
        allowedUntilTime: String,
        notes: String
    ): Long {
        // End any existing session
        parkingSessionDao.endAllSessions()
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60 * 1000L)
        val entity = ParkingSessionEntity(
            scanResultId = scanResultId,
            locationName = locationName,
            startTime = now,
            endTime = endTime,
            allowedUntilTime = allowedUntilTime,
            reminderMinutesBefore = 15,
            notes = notes,
            isActive = true
        )
        return parkingSessionDao.insertSession(entity)
    }

    suspend fun endActiveSession(id: Long) {
        parkingSessionDao.endSession(id)
    }

    suspend fun extendActiveSession(id: Long, additionalMinutes: Int, currentEndTime: Long) {
        val newEndTime = currentEndTime + (additionalMinutes * 60 * 1000L)
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val newAllowedUntil = sdf.format(java.util.Date(newEndTime))
        parkingSessionDao.extendSession(id, additionalMinutes * 60 * 1000L, newAllowedUntil)
    }

    suspend fun updateSessionReminder(id: Long, reminderMinutes: Int) {
        parkingSessionDao.updateReminder(id, reminderMinutes)
    }

    suspend fun addSavedPlace(place: SavedPlace): Long {
        return savedPlaceDao.insertPlace(
            SavedPlaceEntity(
                name = place.name,
                address = place.address,
                parkingNote = place.parkingNote,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSavedPlace(id: Long) {
        savedPlaceDao.deletePlaceById(id)
        noteDao.deleteNoteForSavedPlace(id)
    }

    suspend fun saveParkingSpot(
        latitude: Double,
        longitude: Double,
        accuracy: Float? = null,
        timestamp: Long = System.currentTimeMillis(),
        locationName: String = "",
        sessionId: Long? = null
    ): Long {
        parkingSpotDao.clearActiveSpots()
        val entity = ParkingSpotEntity(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            timestamp = timestamp,
            locationName = locationName,
            sessionId = sessionId,
            isActive = true
        )
        return parkingSpotDao.insertParkingSpot(entity)
    }

    suspend fun clearActiveParkingSpots() {
        parkingSpotDao.clearActiveSpots()
    }

    suspend fun getActiveParkingSpotDirect(): ParkingSpot? {
        return parkingSpotDao.getActiveParkingSpotDirect()?.let { entityToParkingSpot(it) }
    }

    suspend fun clearAllData() {
        scanDao.clearAllScans()
        parkingSessionDao.clearAllSessions()
        savedPlaceDao.clearAllSavedPlaces()
        noteDao.clearAllNotes()
        parkingSpotDao.clearAllSpots()
    }

    private fun entityToScanResult(entity: ScanResultEntity): ScanResult {
        val rules: List<String> = try {
            stringListAdapter.fromJson(entity.parkingRulesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val signs: List<DetectedSign> = try {
            signListAdapter.fromJson(entity.detectedSignsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val verdict = try {
            ScanVerdict.valueOf(entity.verdict)
        } catch (e: Exception) {
            ScanVerdict.ALLOWED
        }

        return ScanResult(
            id = entity.id,
            timestamp = entity.timestamp,
            locationName = entity.locationName,
            cityState = entity.cityState,
            verdict = verdict,
            statusChipText = entity.statusChipText,
            allowedUntilTime = entity.allowedUntilTime,
            timeRemaining = entity.timeRemaining,
            parkingRules = rules,
            explanation = entity.explanation,
            detectedSigns = signs,
            zoneType = entity.zoneType,
            paymentInfo = entity.paymentInfo,
            vehicleApplicability = entity.vehicleApplicability,
            imageUri = entity.imageUri
        )
    }

    private fun scanResultToEntity(scan: ScanResult): ScanResultEntity {
        return ScanResultEntity(
            id = scan.id,
            timestamp = scan.timestamp,
            locationName = scan.locationName,
            cityState = scan.cityState,
            verdict = scan.verdict.name,
            statusChipText = scan.statusChipText,
            allowedUntilTime = scan.allowedUntilTime,
            timeRemaining = scan.timeRemaining,
            parkingRulesJson = stringListAdapter.toJson(scan.parkingRules),
            explanation = scan.explanation,
            detectedSignsJson = signListAdapter.toJson(scan.detectedSigns),
            zoneType = scan.zoneType,
            paymentInfo = scan.paymentInfo,
            vehicleApplicability = scan.vehicleApplicability,
            imageUri = scan.imageUri
        )
    }

    private fun entityToParkingSession(entity: ParkingSessionEntity): ActiveParkingSession {
        return ActiveParkingSession(
            id = entity.id,
            scanResultId = entity.scanResultId,
            locationName = entity.locationName,
            startTime = entity.startTime,
            endTime = entity.endTime,
            allowedUntilTime = entity.allowedUntilTime,
            reminderMinutesBefore = entity.reminderMinutesBefore,
            notes = entity.notes,
            isActive = entity.isActive
        )
    }

    private fun entityToSavedPlace(entity: SavedPlaceEntity): SavedPlace {
        return SavedPlace(
            id = entity.id,
            name = entity.name,
            address = entity.address,
            parkingNote = entity.parkingNote,
            timestamp = entity.timestamp
        )
    }

    private fun entityToCurbNote(entity: CurbNoteEntity): CurbNote {
        return CurbNote(
            id = entity.id,
            targetType = entity.targetType,
            targetId = entity.targetId,
            text = entity.text,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun entityToParkingSpot(entity: ParkingSpotEntity): ParkingSpot {
        return ParkingSpot(
            id = entity.id,
            latitude = entity.latitude,
            longitude = entity.longitude,
            timestamp = entity.timestamp,
            accuracy = entity.accuracy,
            locationName = entity.locationName,
            sessionId = entity.sessionId,
            isActive = entity.isActive
        )
    }
}
