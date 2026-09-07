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
import com.example.util.ParkingTimerCalculator
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
        scanResultId: Long = 0,
        locationName: String = "Parked Spot",
        durationMinutes: Int = 0,
        allowedUntilTime: String = "",
        notes: String = "",
        timerBasis: String = "",
        parkingRuleSummary: String = "",
        scanResult: ScanResult? = null,
        maxAllowedEndTimeMillis: Long? = null
    ): Long {
        val now = System.currentTimeMillis()

        // 1. Resolve target scan result
        val targetScan: ScanResult? = scanResult ?: if (scanResultId > 0) {
            getScanById(scanResultId)
        } else null

        var canonicalMaxEndTime: Long? = maxAllowedEndTimeMillis
        var finalTimerBasis = timerBasis
        var finalRuleSummary = parkingRuleSummary
        var finalAllowedUntil = allowedUntilTime

        if (targetScan != null) {
            // Must be ALLOWED verdict
            if (targetScan.verdict != ScanVerdict.ALLOWED) {
                return -1L // Reject AMBIGUOUS or RESTRICTED scans
            }

            val timerConfig = ParkingTimerCalculator.calculateConfig(targetScan, now)
            if (!timerConfig.isValidAllowed) {
                return -1L // Reject if duration/rule is unparseable or unknown
            }

            val computedMax = if (timerConfig.isUnrestricted) {
                Long.MAX_VALUE
            } else {
                now + (timerConfig.calculatedMinutes * 60 * 1000L)
            }

            canonicalMaxEndTime = computedMax
            if (finalTimerBasis.isBlank()) finalTimerBasis = timerConfig.timerBasis
            if (finalRuleSummary.isBlank()) finalRuleSummary = timerConfig.ruleSummary
            if (finalAllowedUntil.isBlank()) finalAllowedUntil = timerConfig.allowedUntilTimeFormatted
        } else {
            // No scan result provided
            if (canonicalMaxEndTime == null) {
                if (durationMinutes > 0) {
                    // Quick timer preset or explicit duration
                    canonicalMaxEndTime = now + (durationMinutes * 60 * 1000L)
                } else {
                    return -1L // No valid scan, no max authority, no duration -> Reject!
                }
            }
        }

        // Determine requested end time
        val requestedEndTime = if (durationMinutes > 0) {
            now + (durationMinutes * 60 * 1000L)
        } else {
            canonicalMaxEndTime ?: (now + 60 * 1000L)
        }

        // Clamp effective end time to canonical max authority
        val effectiveEndTime = if (canonicalMaxEndTime != null) {
            minOf(requestedEndTime, canonicalMaxEndTime)
        } else {
            requestedEndTime
        }

        if (effectiveEndTime <= now) {
            return -1L
        }

        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        if (finalAllowedUntil.isBlank() && effectiveEndTime != Long.MAX_VALUE) {
            finalAllowedUntil = sdf.format(java.util.Date(effectiveEndTime))
        }

        val isDemoSession = (targetScan?.isDemo == true) || (scanResult?.isDemo == true)

        if (isDemoSession) {
            parkingSessionDao.endAllDemoSessions()
        } else {
            parkingSessionDao.endAllRealSessions()
        }

        val markedTimerBasis = if (isDemoSession) {
            if (finalTimerBasis.startsWith("[Demo]")) finalTimerBasis else if (finalTimerBasis.isBlank()) "[Demo] Simulation Timer" else "[Demo] $finalTimerBasis"
        } else {
            finalTimerBasis
        }

        val entity = ParkingSessionEntity(
            scanResultId = scanResultId,
            locationName = if (locationName != "Parked Spot" || targetScan == null) locationName else targetScan.locationName,
            startTime = now,
            endTime = effectiveEndTime,
            allowedUntilTime = finalAllowedUntil,
            reminderMinutesBefore = 15,
            notes = notes,
            timerBasis = markedTimerBasis,
            parkingRuleSummary = finalRuleSummary,
            isActive = true,
            maxAllowedEndTimeMillis = canonicalMaxEndTime,
            isDemo = isDemoSession
        )
        return parkingSessionDao.insertSession(entity)
    }

    suspend fun endActiveSession(id: Long) {
        parkingSessionDao.endSession(id)
    }

    suspend fun extendActiveSession(
        id: Long,
        additionalMinutes: Int,
        currentEndTime: Long = 0
    ): Boolean {
        val session = parkingSessionDao.getSessionById(id) ?: return false
        if (!session.isActive) return false

        val maxAllowed = session.maxAllowedEndTimeMillis ?: return false // No reliable max -> reject extension
        val baseEndTime = if (currentEndTime > 0) currentEndTime else session.endTime

        if (baseEndTime >= maxAllowed) {
            return false // Already at or beyond maximum authority
        }

        val requestedNewEndTime = baseEndTime + (additionalMinutes * 60 * 1000L)
        val clampedEndTime = minOf(requestedNewEndTime, maxAllowed)

        if (clampedEndTime <= session.endTime) {
            return false // Clamping yields no additional time
        }

        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val newAllowedUntil = sdf.format(java.util.Date(clampedEndTime))

        val updatedSession = session.copy(
            endTime = clampedEndTime,
            allowedUntilTime = newAllowedUntil
        )
        parkingSessionDao.updateSession(updatedSession)
        return true
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

        var verdict = try {
            ScanVerdict.valueOf(entity.verdict)
        } catch (e: Exception) {
            ScanVerdict.AMBIGUOUS
        }

        val finalRules = if (rules.isNotEmpty()) rules else listOf("No verified parking rule has been established.")
        val finalAllowedUntil = entity.allowedUntilTime.ifBlank { "Verify physical signage" }
        val finalExplanation = entity.explanation.ifBlank { "Parking rules could not be determined from verified sign evidence." }
        val finalStatusChip = entity.statusChipText.ifBlank { "Signage unclear" }

        if (verdict == ScanVerdict.ALLOWED && (rules.isEmpty() || rules.all { it.contains("No verified parking rule") })) {
            verdict = ScanVerdict.AMBIGUOUS
        }

        return ScanResult(
            id = entity.id,
            timestamp = entity.timestamp,
            locationName = entity.locationName.ifBlank { "Location unavailable" },
            cityState = entity.cityState,
            verdict = verdict,
            statusChipText = finalStatusChip,
            allowedUntilTime = finalAllowedUntil,
            timeRemaining = entity.timeRemaining.ifBlank { "--" },
            parkingRules = finalRules,
            explanation = finalExplanation,
            detectedSigns = signs,
            zoneType = entity.zoneType.ifBlank { "Parking zone" },
            paymentInfo = entity.paymentInfo,
            vehicleApplicability = entity.vehicleApplicability,
            imageUri = entity.imageUri,
            isDemo = entity.isDemo
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
            imageUri = scan.imageUri,
            isDemo = scan.isDemo
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
            timerBasis = entity.timerBasis,
            parkingRuleSummary = entity.parkingRuleSummary,
            isActive = entity.isActive,
            maxAllowedEndTimeMillis = entity.maxAllowedEndTimeMillis,
            isDemo = entity.isDemo
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
