package com.watchher.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PhoneToWatch(val confidencePercentage: Double) {
    fun encodeJson(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun decodeJson(json: String): PhoneToWatch {
            return Json.decodeFromString(json)
        }
    }
}