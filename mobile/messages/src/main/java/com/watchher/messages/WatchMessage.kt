package com.watchher.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WatchMessage(
    val heartRate: Double,
    val steps: Int,
    val acceleration: Double,
    val ppg: Double,
    val timeOfDat: Double
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