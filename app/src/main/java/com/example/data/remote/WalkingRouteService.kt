package com.example.data.remote

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit

data class WalkingRoute(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val isRealFootRoute: Boolean
)

class WalkingRouteService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun getWalkingRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): WalkingRoute = withContext(Dispatchers.IO) {
        val fallbackDistance = calculateDirectDistance(startLat, startLng, endLat, endLng)
        val fallbackPoints = listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
        // Average walking speed ~1.33 m/s (~80 meters/minute)
        val fallbackDuration = if (fallbackDistance > 0) fallbackDistance / 1.33 else 0.0

        try {
            val url = "https://router.project-osrm.org/route/v1/foot/$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Curb-Android-App/1.0")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string()

            if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                val json = JSONObject(bodyString)
                if (json.optString("code") == "Ok") {
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val route0 = routes.getJSONObject(0)
                        val dist = route0.optDouble("distance", fallbackDistance)
                        val dur = route0.optDouble("duration", fallbackDuration)
                        val geometry = route0.optJSONObject("geometry")
                        val coords = geometry?.optJSONArray("coordinates")

                        if (coords != null && coords.length() > 0) {
                            val routePoints = mutableListOf<GeoPoint>()
                            for (i in 0 until coords.length()) {
                                val pair = coords.getJSONArray(i)
                                val lng = pair.getDouble(0)
                                val lat = pair.getDouble(1)
                                routePoints.add(GeoPoint(lat, lng))
                            }
                            if (routePoints.size >= 2) {
                                return@withContext WalkingRoute(
                                    points = routePoints,
                                    distanceMeters = dist,
                                    durationSeconds = dur,
                                    isRealFootRoute = true
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Graceful fallback on network/timeout issue
        }

        return@withContext WalkingRoute(
            points = fallbackPoints,
            distanceMeters = fallbackDistance,
            durationSeconds = fallbackDuration,
            isRealFootRoute = false
        )
    }

    private fun calculateDirectDistance(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0].toDouble()
    }
}
