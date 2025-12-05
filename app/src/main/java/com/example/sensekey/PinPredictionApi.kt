package com.example.sensekey

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface PinPredictionApiService {
    // UPDATED: Endpoint changed to match your request
    @POST("api/v1/predict_pin_with_sensors")
    suspend fun predictPin(@Body request: PinPredictionRequest): PinPredictionResponse
}

/**
 * Request body with full sensor data for ML model
 */
data class PinPredictionRequest(
    @SerializedName("sensor_data") val sensorData: List<SensorSample>,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("actual_pin") val actualPin: String
)

/**
 * Single sensor sample with all motion and temporal data
 */
data class SensorSample(
    @SerializedName("timestamp_ms") val timestampMs: Long,
    @SerializedName("time_from_start_ms") val timeFromStartMs: Long,

    // Accelerometer (m/s²)
    @SerializedName("accel_x") val accelX: Float,
    @SerializedName("accel_y") val accelY: Float,
    @SerializedName("accel_z") val accelZ: Float,
    @SerializedName("accel_magnitude") val accelMagnitude: Float,

    // Gyroscope (rad/s)
    @SerializedName("gyro_x") val gyroX: Float,
    @SerializedName("gyro_y") val gyroY: Float,
    @SerializedName("gyro_z") val gyroZ: Float,
    @SerializedName("gyro_magnitude") val gyroMagnitude: Float,

    // Rotation Vector
    @SerializedName("rot_x") val rotX: Float,
    @SerializedName("rot_y") val rotY: Float,
    @SerializedName("rot_z") val rotZ: Float,
    @SerializedName("rot_scalar") val rotScalar: Float,

    // Event markers
    @SerializedName("event_type") val eventType: String,
    @SerializedName("digit_pressed") val digitPressed: String?,
    @SerializedName("digit_position") val digitPosition: Int?
)

data class PinPredictionResponse(
    @SerializedName("pin") val pin: String?,
    @SerializedName("confidence") val confidence: Float?,
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

object PinPredictionApi {
    // Replace with your actual base URL
    private const val BASE_URL = "https://pinpredictionapi-production.up.railway.app/"
    private const val TAG = "PinPredictionApi"

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)  // Add write timeout for large payloads
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: PinPredictionApiService = retrofit.create(PinPredictionApiService::class.java)
}