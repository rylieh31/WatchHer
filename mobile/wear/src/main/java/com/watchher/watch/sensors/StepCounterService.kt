package com.watchher.watch.sensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.core.app.NotificationCompat
import com.watchher.watch.R

class StepCounterService : Service(), SensorEventListener {

    companion object {
        const val ACTION_STEP_UPDATE = "com.watchher.watch.STEP_UPDATE"
        const val EXTRA_STEP_COUNT = "com.watchher.watch.EXTRA_STEP_COUNT"

        private const val CHANNEL_ID = "watchher_steps"
        private const val NOTIFICATION_ID = 1002
    }

    private lateinit var sensorManager: SensorManager
    private var stepCounter: Sensor? = null
    private var stepDetector: Sensor? = null
    private var detectorSteps: Float = 0f

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("StepCounterService", "onStartCommand")
        if (!hasRequiredPermissions()) {
            Log.w("StepCounterService", "Missing ACTIVITY_RECOGNITION permission")
            stopSelf()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: SecurityException) {
            Log.e("StepCounterService", "FGS start denied", e)
            stopSelf()
            return START_NOT_STICKY
        }
        when {
            stepCounter != null -> {
                Log.d("StepCounterService", "Using TYPE_STEP_COUNTER")
                try {
                    sensorManager.registerListener(
                        this,
                        stepCounter,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                } catch (e: SecurityException) {
                    Log.e("StepCounterService", "Step counter denied", e)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            stepDetector != null -> {
                Log.d("StepCounterService", "Using TYPE_STEP_DETECTOR")
                try {
                    sensorManager.registerListener(
                        this,
                        stepDetector,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                } catch (e: SecurityException) {
                    Log.e("StepCounterService", "Step detector denied", e)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            else -> {
                Log.w("StepCounterService", "No step sensors available")
                stopSelf()
                return START_NOT_STICKY
            }
        }
        Log.d("StepCounterService", "Step sensor registered")
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        val raw = event.values.firstOrNull() ?: return
        val steps = if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            detectorSteps += raw
            detectorSteps
        } else {
            raw
        }
        Log.d(
            "StepCounterService",
            "Step update type=${event.sensor.type} raw=$raw steps=$steps"
        )
        val update = Intent(ACTION_STEP_UPDATE)
        update.putExtra(EXTRA_STEP_COUNT, steps)
        LocalBroadcastManager.getInstance(this).sendBroadcast(update)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Step monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun hasRequiredPermissions(): Boolean {
        val pm = android.content.pm.PackageManager.PERMISSION_GRANTED
        return checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == pm
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WatchHer")
            .setContentText("Monitoring steps")
            .setSmallIcon(R.drawable.watchher_logo)
            .setOngoing(true)
            .build()
    }
}
