package com.watchher.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WatchMessage(
    val heartRateMean: Double,
    val heartRateStd: Double,
    val steps: Int,
    val accelRms: Double,
    val accelPeak: Double,
    val ppg: Double,
    val timeOfDay: Double
) {
    fun encodeJson(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun decodeJson(json: String): WatchMessage {
            return Json.decodeFromString(json)
        }
    }
}