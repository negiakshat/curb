package com.example.util

import com.example.data.model.SavedPlace
import java.security.MessageDigest
import java.util.Locale

object SavedSpotFingerprint {

    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun isSameLocation(
        addr1: String, name1: String, lat1: Double?, lng1: Double?,
        addr2: String, name2: String, lat2: Double?, lng2: Double?
    ): Boolean {
        val normAddr1 = normalize(addr1)
        val normAddr2 = normalize(addr2)
        if (normAddr1.isNotBlank() && normAddr2.isNotBlank() && normAddr1 == normAddr2) {
            return true
        }

        val normName1 = normalize(name1)
        val normName2 = normalize(name2)
        if (normName1.isNotBlank() && normName2.isNotBlank() && normName1 == normName2) {
            return true
        }

        if (lat1 != null && lng1 != null && lat2 != null && lng2 != null) {
            val dLat = Math.abs(lat1 - lat2)
            val dLng = Math.abs(lng1 - lng2)
            if (dLat < 0.0003 && dLng < 0.0003) {
                return true
            }
        }

        if (normAddr1.length >= 5 && normAddr2.length >= 5 && (normAddr1.contains(normAddr2) || normAddr2.contains(normAddr1))) {
            return true
        }

        return false
    }

    fun computeSignContentHash(
        ruleSummary: String,
        schedule: String,
        verdict: String
    ): String {
        val normRule = normalize(ruleSummary)
        val normSchedule = normalize(schedule)
        val normVerdict = normalize(verdict)

        val combined = "$normRule|$normSchedule|$normVerdict"
        return sha256(combined)
    }

    fun computeCompositeFingerprint(place: SavedPlace): String {
        val loc = if (place.address.isNotBlank()) normalize(place.address) else normalize(place.name)
        val contentHash = computeSignContentHash(
            place.parkingRuleSummary,
            place.parkingSchedule,
            place.parkingVerdict
        )
        return "$loc#$contentHash"
    }

    private fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input
        }
    }

    fun isDuplicate(candidate: SavedPlace, existing: SavedPlace): Boolean {
        if (candidate.id > 0 && candidate.id == existing.id) return false

        if (candidate.scanResultId != null && existing.scanResultId != null &&
            candidate.scanResultId > 0 && candidate.scanResultId == existing.scanResultId) {
            return true
        }

        val sameLoc = isSameLocation(
            candidate.address, candidate.name, candidate.latitude, candidate.longitude,
            existing.address, existing.name, existing.latitude, existing.longitude
        )
        if (!sameLoc) return false

        val candidateContent = computeSignContentHash(
            candidate.parkingRuleSummary,
            candidate.parkingSchedule,
            candidate.parkingVerdict
        )
        val existingContent = computeSignContentHash(
            existing.parkingRuleSummary,
            existing.parkingSchedule,
            existing.parkingVerdict
        )

        return candidateContent == existingContent
    }
}
