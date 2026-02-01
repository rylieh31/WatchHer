package com.watchher.watch

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import com.watchher.messages.WatchMessage

class WatchService : WearableListenerService() {
    override fun onMessageReceived(message: MessageEvent) {
        val watchMessage = WatchMessage.decodeJson(message.data.toString())
        Log.d("WatchHer", "Received message: $watchMessage")
    }
}