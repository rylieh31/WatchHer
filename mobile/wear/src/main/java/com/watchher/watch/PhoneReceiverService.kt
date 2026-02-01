package com.watchher.watch

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.watchher.messages.PhoneToWatch
import java.nio.charset.StandardCharsets

class PhoneReceiverService : WearableListenerService() {

    companion object {
        const val ACTION_HEART_RATE_UPDATE = "com.watchher.watch.HEART_RATE_UPDATE"
        const val EXTRA_HEART_RATE = "com.watchher.watch.EXTRA_HEART_RATE"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WatchHer", "PhoneReceiverService started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onMessageReceived(message: MessageEvent) {
        val json = String(message.data, StandardCharsets.UTF_8)
        val phoneToWatch = PhoneToWatch.decodeJson(json)
        Log.d("WatchHer", "Received message: $phoneToWatch")

        val intent = Intent(ACTION_HEART_RATE_UPDATE)
        intent.putExtra(EXTRA_HEART_RATE, phoneToWatch.confidencePercentage)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}