package com.example.sensekey

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * WebSocket service for streaming sensor data to the backend in real-time
 */
class SensorWebSocketService(
    private val sessionId: String,
    private val deviceId: String,
    private val actualPin: String
) {

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private var onPredictionReceived: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onConnected: (() -> Unit)? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)  // No timeout for WebSocket
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    // WebSocket URL (wss:// for secure, ws:// for local testing)
    private val wsUrl = "ws://10.0.0.40:8000/ws/predict"  // LOCAL TESTING - Your Mac's IP
    // For production: "wss://pinpredictionapi-production.up.railway.app/ws/predict"
    // For emulator: "ws://10.0.2.2:8000/ws/predict"

    companion object {
        private const val TAG = "SensorWebSocketService"
    }

    /**
     * Connect to WebSocket server
     */
    fun connect() {
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully")

                // Send session initialization message
                val initMessage = mapOf(
                    "type" to "init",
                    "session_id" to sessionId,
                    "device_id" to deviceId,
                    "actual_pin" to actualPin
                )
                sendMessage(initMessage)

                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received bytes: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}", t)
                onError?.invoke(t.message ?: "WebSocket connection failed")
            }
        })
    }

    /**
     * Stream a single sensor sample to the backend
     */
    fun streamSensorSample(sensorSample: SensorSample) {
        val message = mapOf(
            "type" to "sensor_data",
            "data" to sensorSample
        )
        sendMessage(message)
    }

    /**
     * Update the actual PIN (called before requesting prediction)
     */
    fun updateActualPin(newActualPin: String) {
        Log.d(TAG, "Updating actual PIN to: $newActualPin")
        val message = mapOf(
            "type" to "update_pin",
            "actual_pin" to newActualPin
        )
        sendMessage(message)
    }

    /**
     * Signal that PIN entry is complete and request prediction
     */
    fun requestPrediction() {
        Log.d(TAG, "Requesting prediction...")
        val message = mapOf(
            "type" to "predict",
            "session_id" to sessionId
        )
        sendMessage(message)
    }

    /**
     * Send a message to the WebSocket server
     */
    private fun sendMessage(message: Map<String, Any?>) {
        val json = gson.toJson(message)
        val success = webSocket?.send(json) ?: false
        if (!success) {
            Log.e(TAG, "Failed to send message: $json")
        }
    }

    /**
     * Handle incoming messages from server
     */
    private fun handleMessage(text: String) {
        try {
            val response = gson.fromJson(text, Map::class.java) as Map<*, *>
            val type = response["type"] as? String

            when (type) {
                "init_ack" -> {
                    Log.d(TAG, "Session initialized successfully")
                }
                "sensor_ack" -> {
                    // Acknowledgment that sensor data was received
                    // Can track progress here if needed
                }
                "prediction" -> {
                    val predictedPin = response["pin"] as? String
                    val confidence = response["confidence"] as? Double
                    Log.d(TAG, "Prediction received: PIN=$predictedPin, confidence=$confidence")

                    if (predictedPin != null) {
                        onPredictionReceived?.invoke(predictedPin)
                    } else {
                        onError?.invoke("Invalid prediction response")
                    }
                }
                "error" -> {
                    val errorMessage = response["message"] as? String ?: "Unknown error"
                    Log.e(TAG, "Server error: $errorMessage")
                    onError?.invoke(errorMessage)
                }
                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $text", e)
            onError?.invoke("Failed to parse server response")
        }
    }

    /**
     * Set callback for when prediction is received
     */
    fun setOnPredictionReceived(callback: (String) -> Unit) {
        onPredictionReceived = callback
    }

    /**
     * Set callback for errors
     */
    fun setOnError(callback: (String) -> Unit) {
        onError = callback
    }

    /**
     * Set callback for successful connection
     */
    fun setOnConnected(callback: () -> Unit) {
        onConnected = callback
    }

    /**
     * Close WebSocket connection
     */
    fun disconnect() {
        webSocket?.close(1000, "Client closing connection")
        webSocket = null
    }

    /**
     * Check if WebSocket is connected
     */
    fun isConnected(): Boolean {
        return webSocket != null
    }
}