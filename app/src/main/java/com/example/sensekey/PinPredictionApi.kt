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
 * UPDATED: Request body matching your exact JSON structure
 */
data class PinPredictionRequest(
    @SerializedName("touch_data") val touchData: List<TouchCoordinate>,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("actual_pin") val actualPin: String
)

/**
 * UPDATED: Data point for a single touch event (X, Y only)
 */
data class TouchCoordinate(
    @SerializedName("abs_touch_x") val absTouchX: Float,
    @SerializedName("abs_touch_y") val absTouchY: Float
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
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: PinPredictionApiService = retrofit.create(PinPredictionApiService::class.java)
}