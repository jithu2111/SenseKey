package com.example.sensekey

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

class PinPredictionService(private val context: Context) {

    private val sessionId = UUID.randomUUID().toString()
    private var webSocketService: SensorWebSocketService? = null
    private var isStreaming = false

    private val deviceId: String by lazy {
        try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    companion object {
        private const val TAG = "PinPredictionService"
    }

    /**
     * Start WebSocket connection and streaming session
     */
    suspend fun startStreaming(actualPin: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                suspendCancellableCoroutine { continuation ->
                    webSocketService = SensorWebSocketService(sessionId, deviceId, actualPin)

                    webSocketService?.setOnConnected {
                        Log.d(TAG, "WebSocket connected, ready to stream")
                        isStreaming = true
                        continuation.resume(Result.success(Unit))
                    }

                    webSocketService?.setOnError { error ->
                        Log.e(TAG, "WebSocket error: $error")
                        if (!continuation.isCompleted) {
                            continuation.resume(Result.failure(Exception(error)))
                        }
                    }

                    webSocketService?.connect()

                    continuation.invokeOnCancellation {
                        webSocketService?.disconnect()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start streaming", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Stream a sensor sample in real-time via WebSocket
     */
    fun streamSensorSample(sensorData: SensorData) {
        if (!isStreaming) {
            Log.w(TAG, "Not streaming, ignoring sensor sample")
            return
        }

        val sample = SensorSample(
            timestampMs = sensorData.timestamp,
            timeFromStartMs = sensorData.timeFromStart,
            accelX = sensorData.accelX,
            accelY = sensorData.accelY,
            accelZ = sensorData.accelZ,
            accelMagnitude = sensorData.accelMagnitude,
            gyroX = sensorData.gyroX,
            gyroY = sensorData.gyroY,
            gyroZ = sensorData.gyroZ,
            gyroMagnitude = sensorData.gyroMagnitude,
            rotX = sensorData.rotVectorX,
            rotY = sensorData.rotVectorY,
            rotZ = sensorData.rotVectorZ,
            rotScalar = sensorData.rotVectorScalar,
            eventType = sensorData.eventType,
            digitPressed = sensorData.digitPressed,
            digitPosition = sensorData.digitPosition
        )

        webSocketService?.streamSensorSample(sample)
    }

    /**
     * Request prediction from backend (after PIN entry is complete)
     */
    suspend fun requestPrediction(actualPin: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                suspendCancellableCoroutine { continuation ->
                    // Update the actual PIN before requesting prediction
                    webSocketService?.updateActualPin(actualPin)

                    webSocketService?.setOnPredictionReceived { predictedPin ->
                        Log.d(TAG, "Prediction received: $predictedPin")
                        continuation.resume(Result.success(predictedPin))
                    }

                    webSocketService?.setOnError { error ->
                        Log.e(TAG, "Prediction error: $error")
                        if (!continuation.isCompleted) {
                            continuation.resume(Result.failure(Exception(error)))
                        }
                    }

                    webSocketService?.requestPrediction()

                    continuation.invokeOnCancellation {
                        stopStreaming()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Prediction request failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Stop streaming and close WebSocket connection
     */
    fun stopStreaming() {
        isStreaming = false
        webSocketService?.disconnect()
        webSocketService = null
        Log.d(TAG, "Streaming stopped")
    }

    fun clearData() {
        stopStreaming()
        Log.d(TAG, "Cleared service data")
    }
}