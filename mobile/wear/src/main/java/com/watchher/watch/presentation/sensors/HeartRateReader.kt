package com.watchher.watch.presentation.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class HeartRateReader(context: Context) {

    private val measureClient = HealthServices.getClient(context).measureClient
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    fun heartRateFlow(): Flow<Int> = callbackFlow {

        var usingMeasureClient = false

        val measureCallback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                // Not used for this use case
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRate = data.getData(DataType.HEART_RATE_BPM)
                    .lastOrNull()?.value?.toInt()
                if (heartRate != null && heartRate > 0) {
                    trySend(heartRate)
                }
            }
        }

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val heartRate = event.values.firstOrNull()?.toInt()
                if (heartRate != null && heartRate > 0) {
                    trySend(heartRate)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun registerSensorFallback() {
            if (heartRateSensor == null) {
                Log.w("HeartRateReader", "No heart rate sensor available")
                close()
                return
            }
            Log.d("HeartRateReader", "Using SensorManager heart rate fallback")
            sensorManager.registerListener(
                sensorListener,
                heartRateSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        launch {
            try {
                val capabilities = measureClient.getCapabilitiesAsync().await()
                if (DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure) {
                    usingMeasureClient = true
                    Log.d("HeartRateReader", "Using Health Services measure client")
                    measureClient.registerMeasureCallback(
                        DataType.HEART_RATE_BPM,
                        measureCallback
                    )
                } else {
                    Log.w("HeartRateReader", "Health Services does not support HR measure")
                    registerSensorFallback()
                }
            } catch (e: Exception) {
                Log.e("HeartRateReader", "Health Services failed, fallback to sensor", e)
                registerSensorFallback()
            }
        }

        awaitClose {
            sensorManager.unregisterListener(sensorListener)
            if (usingMeasureClient) {
                launch {
                    measureClient.unregisterMeasureCallbackAsync(
                        DataType.HEART_RATE_BPM,
                        measureCallback
                    ).await()
                }
            }
        }
    }
}