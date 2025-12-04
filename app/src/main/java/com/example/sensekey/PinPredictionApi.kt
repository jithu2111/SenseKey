package com.example.sensekey

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * API service for PIN prediction backend
 */
interface PinPredictionApiService {
    @POST("predictPin")
    suspend fun predictPin(@Body request: PinPredictionRequest): PinPredictionResponse
    
    @retrofit2.http.GET("health")
    suspend fun healthCheck(): HealthCheckResponse
}

/**
 * Request body for PIN prediction
 */
data class PinPredictionRequest(
    val touchData: List<TouchDataPoint>,
    val sensorData: List<SensorDataPoint>,
    val keystrokes: List<KeystrokeEvent>? = null,  // Press/release timestamps for temporal features
    val metadata: PredictionMetadata,
    val actual_pin: String? = null  // For testing: send the actual typed PIN
)

/**
 * Keystroke event with press and release timestamps
 * Critical for calculating temporal features: button_duration_ms, inter_keystroke_time
 */
data class KeystrokeEvent(
    val digit: String,
    val pressTimestamp: Long,
    val releaseTimestamp: Long
)

data class TouchDataPoint(
    val x: Float?,
    val y: Float?,
    val pressure: Float?,
    val size: Float?,
    val timestamp: Long
)

data class SensorDataPoint(
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val rotX: Float,
    val rotY: Float,
    val rotZ: Float,
    val rotScalar: Float,
    val timestamp: Long
)

data class PredictionMetadata(
    val deviceId: String,          // Unique device identifier
    val deviceModel: String,       // Device model info
    val sessionId: String,         // Session identifier
    val androidId: String?,        // Android secure ID
    val buildFingerprint: String   // Build fingerprint for uniqueness
)

/**
 * Response from PIN prediction API
 */
data class PinPredictionResponse(
    val predictedPin: String,
    val confidence: Float,
    val success: Boolean,
    val message: String? = null
)

/**
 * Response from health check API
 */
data class HealthCheckResponse(
    val status: String,
    val service: String,
    val timestamp: Double
)

/**
 * API client singleton
 */
object PinPredictionApi {
    private const val BASE_URL = "https://pinpredictionapi-production.up.railway.app/"
    private const val TAG = "PinPredictionApi"
    
    // Create logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY // Log request/response body
    }
    
    // Create connection monitoring interceptor
    private val connectionInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        Log.d(TAG, "=== Starting Request ===")
        Log.d(TAG, "URL: ${request.url}")
        Log.d(TAG, "Method: ${request.method}")
        Log.d(TAG, "Headers: ${request.headers}")
        
        try {
            val startTime = System.currentTimeMillis()
            val response = chain.proceed(request)
            val endTime = System.currentTimeMillis()
            
            Log.d(TAG, "=== Response Received ===")
            Log.d(TAG, "Status: ${response.code} ${response.message}")
            Log.d(TAG, "Time: ${endTime - startTime}ms")
            Log.d(TAG, "Response headers: ${response.headers}")
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "=== Request Failed ===", e)
            throw e
        }
    }
    
    // Create OkHttp client with timeouts and logging
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(connectionInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(45, TimeUnit.SECONDS) // Increased for SSL handshake (Android may need more time)
        .readTimeout(60, TimeUnit.SECONDS)     // Read timeout
        .writeTimeout(30, TimeUnit.SECONDS)   // Write timeout
        .retryOnConnectionFailure(true)       // Retry on connection failure
        .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1)) // Support both HTTP/2 and HTTP/1.1
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: PinPredictionApiService = retrofit.create(PinPredictionApiService::class.java)
    
    init {
        Log.d(TAG, "Initialized API client with base URL: $BASE_URL")
    }
}