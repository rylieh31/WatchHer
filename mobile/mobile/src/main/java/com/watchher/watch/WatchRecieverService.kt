package com.watchher.watch

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import com.watchher.messages.WatchToPhone

class WatchRecieverService : WearableListenerService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WatchHer", "HealthDataService started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onMessageReceived(message: MessageEvent) {
        val healthData = WatchToPhone.decodeJson(message.data.toString())
        Log.d("WatchHer", "Received message: $healthData")

        // todo: process with Java API
        // todo: send cry for help if needed
    }
}