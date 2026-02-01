package com.watchher.watch

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.watchher.messages.PhoneToWatch

class PhoneReceiverService : WearableListenerService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WatchHer", "PhoneReceiverService started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onMessageReceived(message: MessageEvent) {
        val phoneToWatch = PhoneToWatch.decodeJson(message.data.toString())
        Log.d("WatchHer", "Received message: $phoneToWatch")

        // todo: send to UI
    }
}