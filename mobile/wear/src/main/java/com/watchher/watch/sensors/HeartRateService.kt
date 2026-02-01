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
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.watchher.watch.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.concurrent.futures.await

class HeartRateService : Service(), SensorEventListener {

    companion object {
        const val ACTION_HEART_RATE_SENSOR_UPDATE = "com.watchher.watch.HEART_RATE_SENSOR_UPDATE"
        const val EXTRA_HEART_RATE = "com.watchher.watch.EXTRA_HEART_RATE"

        private const val CHANNEL_ID = "watchher_heart_rate"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val measureClient by lazy { HealthServices.getClient(this).measureClient }
    private var usingMeasureClient = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("HeartRateService", "onStartCommand")
        if (!hasRequiredPermissions()) {
            Log.w("HeartRateService", "Missing required permissions")
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            Log.d("HeartRateService", "Foreground started")
        } catch (e: SecurityException) {
            Log.e("HeartRateService", "FGS start denied", e)
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                val capabilities = measureClient.getCapabilitiesAsync().await()
                if (DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure) {
                    usingMeasureClient = true
                    Log.d("HeartRateService", "Using Health Services measure client")
                    measureClient.registerMeasureCallback(
                        DataType.HEART_RATE_BPM,
                        measureCallback
                    )
                    Log.d("HeartRateService", "Measure callback registered")
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("HeartRateService", "Health Services failed, fallback to sensor", e)
            }

            if (heartRateSensor == null) {
                Log.w("HeartRateService", "No heart rate sensor available")
                stopSelf()
                return@launch
            }

            Log.d(
                "HeartRateService",
                "Using SensorManager heart rate: ${heartRateSensor?.name}"
            )
            sensorManager.registerListener(
                this@HeartRateService,
                heartRateSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        if (usingMeasureClient) {
            serviceScope.launch {
                measureClient.unregisterMeasureCallbackAsync(
                    DataType.HEART_RATE_BPM,
                    measureCallback
                ).await()
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        val heartRate = event.values.firstOrNull()?.toInt() ?: return
        if (heartRate <= 0) return
        Log.d("HeartRateService", "Sensor HR=$heartRate")
        val update = Intent(ACTION_HEART_RATE_SENSOR_UPDATE)
        update.putExtra(EXTRA_HEART_RATE, heartRate)
        LocalBroadcastManager.getInstance(this).sendBroadcast(update)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            // Not used
        }

        override fun onDataReceived(data: DataPointContainer) {
            val heartRate = data.getData(DataType.HEART_RATE_BPM)
                .lastOrNull()?.value?.toInt()
            if (heartRate != null && heartRate > 0) {
                Log.d("HeartRateService", "Measure HR=$heartRate")
                val update = Intent(ACTION_HEART_RATE_SENSOR_UPDATE)
                update.putExtra(EXTRA_HEART_RATE, heartRate)
                LocalBroadcastManager.getInstance(this@HeartRateService)
                    .sendBroadcast(update)
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val pm = android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasBodySensors =
            checkSelfPermission(android.Manifest.permission.BODY_SENSORS) == pm
        val hasActivity =
            checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == pm
        val hasHealthRead = if (Build.VERSION.SDK_INT >= 34) {
            checkSelfPermission("android.permission.health.READ_HEART_RATE") == pm
        } else {
            true
        }
        Log.d(
            "HeartRateService",
            "perm bodySensors=$hasBodySensors activity=$hasActivity healthRead=$hasHealthRead"
        )
        return hasBodySensors && (hasActivity || hasHealthRead)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Heart rate monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WatchHer")
            .setContentText("Monitoring heart rate")
            .setSmallIcon(R.drawable.watchher_logo)
            .setOngoing(true)
            .build()
    }
}
