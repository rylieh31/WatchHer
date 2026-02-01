package com.watchher.watch

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import com.watchher.messages.WatchToPhone

class WatchReceiverService : WearableListenerService() {
    companion object {
        const val ACTION_UPDATE_SAFETY_STATUS = "com.watchher.watch.ACTION_UPDATE_SAFETY_STATUS"
        const val ACTION_UPDATE_BIOMETRICS = "com.watchher.watch.ACTION_UPDATE_BIOMETRICS"
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WatchReceiverService", "WatchReceiverService started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onMessageReceived(message: MessageEvent) {
        val watchData = WatchToPhone.decodeJson(String(message.data))
        Log.d("WatchReceiverService", "Received message: $watchData")

        // todo: process with Java API
        // todo: send cry for help if needed

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
}