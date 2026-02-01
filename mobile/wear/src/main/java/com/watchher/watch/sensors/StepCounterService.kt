package com.watchher.watch.sensors

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class StepCounterService : Service(), SensorEventListener {

    companion object {
        const val ACTION_STEP_UPDATE = "com.watchher.watch.STEP_UPDATE"
        const val EXTRA_STEP_COUNT = "com.watchher.watch.EXTRA_STEP_COUNT"
    }

    private lateinit var sensorManager: SensorManager
    private var stepCounter: Sensor? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (stepCounter == null) {
            Log.w("StepCounterService", "No step counter available")
            stopSelf()
            return START_NOT_STICKY
        }
        sensorManager.registerListener(
            this,
            stepCounter,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        val steps = event.values.firstOrNull() ?: return
        val update = Intent(ACTION_STEP_UPDATE)
        update.putExtra(EXTRA_STEP_COUNT, steps)
        LocalBroadcastManager.getInstance(this).sendBroadcast(update)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
