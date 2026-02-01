package com.watchher.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WatchToPhone(
    val heartRateMean: Double,
    val heartRateStd: Double,
    val steps: Int,
    val accelRms: Double,
    val accelPeak: Double,
    val ppg: Double,
    val timeOfDay: Double,
    val needsHelp: Boolean
) {
    fun encodeJson(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun decodeJson(json: String): WatchToPhone {
            return Json.decodeFromString(json)
        }
    }
}