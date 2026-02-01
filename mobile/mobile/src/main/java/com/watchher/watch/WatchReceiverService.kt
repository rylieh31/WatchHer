package com.watchher.watch

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

import com.watchher.messages.WatchToPhone
import com.watchher.messages.PhoneToWatch
import kotlin.math.roundToInt

class WatchReceiverService : WearableListenerService() {
    companion object {
        const val ACTION_UPDATE_SAFETY_STATUS = "com.watchher.watch.ACTION_UPDATE_SAFETY_STATUS"
        const val ACTION_UPDATE_BIOMETRICS = "com.watchher.watch.ACTION_UPDATE_BIOMETRICS"
        const val ACTION_CONFIDENCE_UPDATE = "com.watchher.watch.HEART_RATE_UPDATE"
        const val EXTRA_CONFIDENCE = "com.watchher.watch.EXTRA_HEART_RATE"
        const val SAFETY_STATUS = "com.watchher.watch.SAFETY_STATUS"
        const val HR_MEAN = "com.watchher.watch.HR_MEAN"
        const val HR_STD = "com.watchher.watch.HR_STD"
        const val HR_SLOPE = "com.watchher.watch.HR_SLOPE"
        const val STEPS_20S = "com.watchher.watch.STEPS_20S"
        const val ACCEL_RMS = "com.watchher.watch.ACCEL_RMS"
        const val ACCEL_PEAK = "com.watchher.watch.ACCEL_PEAK"
        const val PPG_STD = "com.watchher.watch.PPG_STD"
        const val TIME_OF_DAY = "com.watchher.watch.TIME_OF_DAY"
        const val PACKAGE = "com.watchher.watch"
    }

    private val modelInference: ModelInference? by lazy {
        try {
            assets.open("rf_model.json").use { stream ->
                ModelInference(stream)
            }
        } catch (e: Exception) {
            Log.e("WatchReceiverService", "Model load failed", e)
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WatchReceiverService", "WatchReceiverService started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onMessageReceived(message: MessageEvent) {
        val watchData = WatchToPhone.decodeJson(String(message.data))
        Log.d("WatchReceiverService", "Received message: $watchData")

        val features = doubleArrayOf(
            watchData.hrMean,
            watchData.hrStd,
            watchData.hrSlope,
            watchData.steps20s.toDouble(),
            watchData.accelRms,
            watchData.accelPeak,
            watchData.ppgStd,
            watchData.timeOfDay
        )
        val confidence = modelInference
            ?.predict(features)
            ?.let { (it * 100.0).roundToInt().coerceIn(0, 100) }
            ?: 0

        val mobileUiUpdateIntent = Intent(ACTION_UPDATE_SAFETY_STATUS).apply {
            putExtra(
                SAFETY_STATUS, if (watchData.needsHelp) {
                    "unsafe"
                } else {
                    "safe"
                }
            )
            setPackage(PACKAGE)
        }
        sendBroadcast(mobileUiUpdateIntent)

        val confidenceIntent = Intent(ACTION_CONFIDENCE_UPDATE).apply {
            putExtra(EXTRA_CONFIDENCE, confidence)
            setPackage(PACKAGE)
        }
        sendBroadcast(confidenceIntent)

        sendConfidenceToWatch(confidence)

        val biometricsIntent = Intent(ACTION_UPDATE_BIOMETRICS).apply {
            putExtra(HR_MEAN, watchData.hrMean)
            putExtra(HR_STD, watchData.hrStd)
            putExtra(HR_SLOPE, watchData.hrSlope)
            putExtra(STEPS_20S, watchData.steps20s)
            putExtra(ACCEL_RMS, watchData.accelRms)
            putExtra(ACCEL_PEAK, watchData.accelPeak)
            putExtra(PPG_STD, watchData.ppgStd)
            putExtra(TIME_OF_DAY, watchData.timeOfDay)
            setPackage(PACKAGE)
        }
        sendBroadcast(biometricsIntent)
    }

    private fun sendConfidenceToWatch(confidence: Int) {
        Wearable.getNodeClient(this)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    val data = PhoneToWatch(confidence.toDouble())
                    Wearable.getMessageClient(this)
                        .sendMessage(
                            node.id,
                            "/watch_her/phone_to_watch",
                            data.encodeJson().toByteArray()
                        )
                }
            }
    }
}