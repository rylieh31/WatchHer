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

class AccelerometerService : Service(), SensorEventListener {

    companion object {
        const val ACTION_ACCEL_UPDATE = "com.watchher.watch.ACCEL_UPDATE"
        const val EXTRA_AX = "com.watchher.watch.EXTRA_AX"
        const val EXTRA_AY = "com.watchher.watch.EXTRA_AY"
        const val EXTRA_AZ = "com.watchher.watch.EXTRA_AZ"
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (accelerometer == null) {
            Log.w("AccelerometerService", "No accelerometer available")
            stopSelf()
            return START_NOT_STICKY
        }
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        val update = Intent(ACTION_ACCEL_UPDATE)
        update.putExtra(EXTRA_AX, event.values.getOrNull(0) ?: 0f)
        update.putExtra(EXTRA_AY, event.values.getOrNull(1) ?: 0f)
        update.putExtra(EXTRA_AZ, event.values.getOrNull(2) ?: 0f)
        LocalBroadcastManager.getInstance(this).sendBroadcast(update)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
