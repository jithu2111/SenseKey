package com.example.sensekey

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class PinPredictionService(private val context: Context) {

    private val sessionId = UUID.randomUUID().toString()

    // UPDATED: Buffer now holds the specific TouchCoordinate objects
    private var touchDataBuffer = mutableListOf<TouchCoordinate>()

    private val deviceId: String by lazy {
        try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    /**
     * Add a touch event to the buffer
     */
    fun addTouchData(x: Float, y: Float) {
        touchDataBuffer.add(TouchCoordinate(x, y))
        Log.d("PinPredictionService", "Added touch: x=$x, y=$y")
    }

    fun clearData() {
        touchDataBuffer.clear()
        Log.d("PinPredictionService", "Cleared touch data buffer")
    }

    /**
     * Send the buffered data to the API
     */
    suspend fun predictPin(actualPin: String): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                // Construct the exact JSON body you requested
                val request = PinPredictionRequest(
                    touchData = touchDataBuffer.toList(),
                    deviceId = deviceId,
                    sessionId = sessionId,
                    actualPin = actualPin
                )

                Log.d("PinPredictionService", "Sending request with ${touchDataBuffer.size} touch points")

                val response = PinPredictionApi.service.predictPin(request)

                // Handle response (adjust based on your API's actual success criteria)
                if (response.success || response.pin != null) {
                    Result.success(response.pin ?: "Unknown")
                } else {
                    Result.failure(Exception(response.message ?: "Prediction failed"))
                }
            }
        } catch (e: Exception) {
            Log.e("PinPredictionService", "API call failed", e)
            Result.failure(e)
        }
    }
}