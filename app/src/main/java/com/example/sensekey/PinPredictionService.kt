package com.example.sensekey

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Service to handle PIN prediction pipeline
 */
class PinPredictionService(private val context: Context) {
    
    private val sessionId = UUID.randomUUID().toString()
    private var touchDataBuffer = mutableListOf<TouchDataPoint>()
    private var sensorDataBuffer = mutableListOf<SensorDataPoint>()
    private var keystrokeEvents = mutableListOf<KeystrokeEvent>()  // Track press/release for each digit
    
    // Generate unique device ID
    private val deviceId: String by lazy {
        generateDeviceId()
    }
    
    /**
     * Generate a unique device identifier using multiple device properties
     */
    private fun generateDeviceId(): String {
        return try {
            // Get Android ID (most reliable)
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            
            // Fallback: Create hash from device properties
            val deviceInfo = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.SERIAL}_${androidId}"
            
            // Create SHA-256 hash for privacy
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(deviceInfo.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.take(16) // First 16 chars
            
        } catch (e: Exception) {
            Log.w("PinPredictionService", "Could not generate device ID, using random UUID", e)
            UUID.randomUUID().toString().replace("-", "").take(16)
        }
    }
    
    /**
     * Add touch data point (deprecated - use addKeystrokeEvent instead)
     */
    fun addTouchData(x: Float?, y: Float?, pressure: Float?, size: Float?) {
        touchDataBuffer.add(
            TouchDataPoint(
                x = x,
                y = y,
                pressure = pressure,
                size = size,
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Keep only last 10 touch points to avoid memory issues
        if (touchDataBuffer.size > 10) {
            touchDataBuffer = touchDataBuffer.takeLast(10).toMutableList()
        }
    }
    
    /**
     * Add keystroke event with press and release timestamps
     * This is critical for calculating temporal features (button_duration_ms, inter_keystroke_time)
     */
    fun addKeystrokeEvent(digit: String, pressTimestamp: Long, releaseTimestamp: Long) {
        keystrokeEvents.add(
            KeystrokeEvent(
                digit = digit,
                pressTimestamp = pressTimestamp,
                releaseTimestamp = releaseTimestamp
            )
        )
        Log.d("PinPredictionService", "Keystroke event: digit=$digit, press=$pressTimestamp, release=$releaseTimestamp, duration=${releaseTimestamp - pressTimestamp}ms")
    }
    
    /**
     * Add sensor data point
     */
    fun addSensorData(
        accelX: Float, accelY: Float, accelZ: Float,
        gyroX: Float, gyroY: Float, gyroZ: Float,
        rotX: Float, rotY: Float, rotZ: Float, rotScalar: Float
    ) {
        sensorDataBuffer.add(
            SensorDataPoint(
                accelX = accelX,
                accelY = accelY, 
                accelZ = accelZ,
                gyroX = gyroX,
                gyroY = gyroY,
                gyroZ = gyroZ,
                rotX = rotX,
                rotY = rotY,
                rotZ = rotZ,
                rotScalar = rotScalar,
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Keep only last 100 sensor points
        if (sensorDataBuffer.size > 100) {
            sensorDataBuffer = sensorDataBuffer.takeLast(100).toMutableList()
        }
    }
    
    /**
     * Call backend API to predict PIN
     */
    suspend fun predictPin(actualPin: String? = null): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                val androidId = try {
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                } catch (e: Exception) {
                    null
                }
                
                val request = PinPredictionRequest(
                    touchData = touchDataBuffer.toList(),
                    sensorData = sensorDataBuffer.toList(),
                    keystrokes = keystrokeEvents.toList(),  // Include keystroke events with press/release
                    metadata = PredictionMetadata(
                        deviceId = deviceId,
                        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                        sessionId = sessionId,
                        androidId = androidId,
                        buildFingerprint = Build.FINGERPRINT
                    ),
                    actual_pin = actualPin  // Include the actual typed PIN
                )
                
                Log.d("PinPredictionService", "Keystroke events: ${keystrokeEvents.size}")
                keystrokeEvents.forEachIndexed { index, event ->
                    Log.d("PinPredictionService", "  [$index] digit=${event.digit}, press=${event.pressTimestamp}, release=${event.releaseTimestamp}, duration=${event.releaseTimestamp - event.pressTimestamp}ms")
                }
                
                Log.d("PinPredictionService", "=== API Request Details ===")
                Log.d("PinPredictionService", "URL: https://pinpredictionapi-production.up.railway.app/predictPin")
                Log.d("PinPredictionService", "Touch points: ${touchDataBuffer.size}")
                Log.d("PinPredictionService", "Sensor points: ${sensorDataBuffer.size}")
                Log.d("PinPredictionService", "Actual PIN: $actualPin")
                Log.d("PinPredictionService", "Device ID: $deviceId")
                Log.d("PinPredictionService", "Session ID: $sessionId")
                Log.d("PinPredictionService", "Sending request...")
                
                val response = PinPredictionApi.service.predictPin(request)
                
                Log.d("PinPredictionService", "=== API Response Received ===")
                Log.d("PinPredictionService", "Success: ${response.success}")
                Log.d("PinPredictionService", "Predicted PIN: ${response.predictedPin}")
                Log.d("PinPredictionService", "Confidence: ${response.confidence}")
                Log.d("PinPredictionService", "Message: ${response.message}")
                
                if (response.success) {
                    Result.success(response.predictedPin)
                } else {
                    Log.e("PinPredictionService", "Prediction failed: ${response.message}")
                    Result.failure(Exception(response.message ?: "Prediction failed"))
                }
            }
        } catch (e: Exception) {
            Log.e("PinPredictionService", "=== API Call Failed ===")
            Log.e("PinPredictionService", "Exception type: ${e.javaClass.simpleName}")
            Log.e("PinPredictionService", "Exception message: ${e.message}")
            Log.e("PinPredictionService", "Stack trace:", e)
            
            // Provide more specific error messages
            val errorMessage = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    "Request timed out. The server may be processing the request. Check server logs."
                e.message?.contains("connect", ignoreCase = true) == true -> 
                    "Failed to connect to server. Check network connection and server status."
                e.message?.contains("SSL", ignoreCase = true) == true -> 
                    "SSL/TLS error. Check server certificate configuration."
                else -> 
                    "API call failed: ${e.message ?: "Unknown error"}"
            }
            
            Log.e("PinPredictionService", "User-friendly error: $errorMessage")
            Result.failure(Exception(errorMessage))
        }
    }
    
    /**
     * Clear all buffered data
     */
    fun clearData() {
        touchDataBuffer.clear()
        sensorDataBuffer.clear()
        keystrokeEvents.clear()
        Log.d("PinPredictionService", "Cleared all buffered data")
    }
    
    /**
     * Get current buffer sizes for debugging
     */
    fun getBufferSizes(): Pair<Int, Int> {
        return Pair(touchDataBuffer.size, sensorDataBuffer.size)
    }
}