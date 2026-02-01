package com.watchher.watch

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import com.watchher.messages.WatchToPhone

class WatchReceiverService : WearableListenerService() {
    companion object {
        const val ACTION_UPDATE_SAFETY_STATUS = "com.watchher.watch.ACTION_UPDATE_SAFETY_STATUS"
        const val SAFETY_STATUS = "com.watchher.watch.SAFETY_STATUS"
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
    }
}