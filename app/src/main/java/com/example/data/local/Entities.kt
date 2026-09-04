package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.data.model.DetectedSign
import com.example.data.model.ScanVerdict
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val locationName: String,
    val cityState: String,
    val verdict: String, // ALLOWED, RESTRICTED, AMBIGUOUS
    val statusChipText: String,
    val allowedUntilTime: String,
    val timeRemaining: String,
    val parkingRulesJson: String, // List<String> as JSON
    val explanation: String,
    val detectedSignsJson: String, // List<DetectedSign> as JSON
    val zoneType: String,
    val paymentInfo: String,
    val vehicleApplicability: String,
    val imageUri: String? = null
)

@Entity(tableName = "parking_sessions")
data class ParkingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanResultId: Long = 0,
    val locationName: String,
    val startTime: Long,
    val endTime: Long,
    val allowedUntilTime: String,
    val reminderMinutesBefore: Int = 15,
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val parkingNote: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "curb_notes",
    indices = [androidx.room.Index(value = ["targetType", "targetId"], unique = true)]
)
data class CurbNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetType: String,
    val targetId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "parking_spots")
data class ParkingSpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float? = null,
    val locationName: String = "",
    val sessionId: Long? = null,
    val isActive: Boolean = true
)

