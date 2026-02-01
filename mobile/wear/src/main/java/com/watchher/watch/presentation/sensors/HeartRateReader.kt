package com.watchher.watch.presentation.sensors

import android.content.Context
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

    fun heartRateFlow(): Flow<Int> = callbackFlow {

        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                // Not used for this use case
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRate = data.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.toInt()
                if (heartRate != null && heartRate > 0) {
                    trySend(heartRate)
                }
            }
        }

        launch {
            val capabilities = measureClient.getCapabilitiesAsync().await()
            if (DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure) {
                measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
            } else {
                close()
            }
        }

        awaitClose {
            launch {
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback).await()
            }
        }
    }
}