package com.example.sensekey

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.util.UUID

/**
 * Manages sensor data collection for PIN entry research
 * Collects data from: Accelerometer, Gyroscope, Rotation Vector
 */
class SensorDataCollector(context: Context) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Sensors
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Current sensor values
    private var accelValues = FloatArray(3) { 0f }
    private var gyroValues = FloatArray(3) { 0f }
    private var rotVectorValues = FloatArray(4) { 0f }

    // Recording state
    private var isRecording = false
    private var recordingStartTime: Long = 0
    private val sensorDataBuffer = mutableListOf<SensorData>()

    // Logging control to prevent redundant samples
    private var lastLogTime: Long = 0
    private val LOG_INTERVAL_MS = 5  // 200Hz effective sampling rate

    // Session information
    private var currentSessionId: String = ""
    private var currentTrialNumber: Int = 0
    private var currentTargetPin: String = ""
    private var currentPin: String = ""

    // Callback for data updates
    var onDataCollected: ((SensorData) -> Unit)? = null

    /**
     * Check if all required sensors are available
     */
    fun areSensorsAvailable(): Boolean {
        return accelerometer != null && gyroscope != null && rotationVector != null
    }

    /**
     * Get list of missing sensors
     */
    fun getMissingSensors(): List<String> {
        val missing = mutableListOf<String>()
        if (accelerometer == null) missing.add("Accelerometer")
        if (gyroscope == null) missing.add("Gyroscope")
        if (rotationVector == null) missing.add("Rotation Vector")
        return missing
    }

    /**
     * Start recording sensor data
     */
    fun startRecording(trialNumber: Int = 1, targetPin: String = "") {
        if (!areSensorsAvailable()) {
            Log.e(TAG, "Cannot start recording: Missing sensors ${getMissingSensors()}")
            return
        }

        isRecording = true
        recordingStartTime = System.currentTimeMillis()
        lastLogTime = 0  // Reset log time for new recording
        currentSessionId = UUID.randomUUID().toString()
        currentTrialNumber = trialNumber
        currentTargetPin = targetPin
        currentPin = ""
        sensorDataBuffer.clear()

        // Register sensor listeners with SENSOR_DELAY_FASTEST for maximum sampling rate
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }

        // Log recording start event
        logEvent("recording_start")

        Log.d(TAG, "Recording started - Session: $currentSessionId, Trial: $currentTrialNumber")
    }

    /**
     * Stop recording sensor data
     */
    fun stopRecording(): List<SensorData> {
        if (!isRecording) {
            return emptyList()
        }

        // Log recording stop event
        logEvent("recording_stop")

        isRecording = false
        sensorManager.unregisterListener(this)

        val collectedData = sensorDataBuffer.toList()
        Log.d(TAG, "Recording stopped - Collected ${collectedData.size} samples")

        return collectedData
    }

    /**
     * Log a button press event
     */
    fun logButtonPress(digit: String, position: Int) {
        currentPin += digit
        logEvent("button_press", digit, position)
        Log.d(TAG, "Button press logged: digit=$digit, position=$position")
    }

    /**
     * Update the current PIN being entered
     */
    fun updateCurrentPin(pin: String) {
        currentPin = pin
    }

    /**
     * Log an event (idle, button_press, etc.)
     */
    private fun logEvent(eventType: String, digitPressed: String? = null, digitPosition: Int? = null) {
        if (!isRecording) return

        val now = System.currentTimeMillis()

        // Calculate if current PIN matches target
        val isCorrect = if (currentPin.length == 4) {
            if (currentPin == currentTargetPin) 1 else 0
        } else {
            0  // Incomplete PIN
        }

        val sensorData = SensorData(
            timestamp = now,
            timeFromStart = now - recordingStartTime,
            sessionId = currentSessionId,
            trialNumber = currentTrialNumber,
            targetPin = currentTargetPin,
            pinEntered = currentPin,
            isCorrect = isCorrect,
            eventType = eventType,
            digitPressed = digitPressed,
            digitPosition = digitPosition,
            accelX = accelValues[0],
            accelY = accelValues[1],
            accelZ = accelValues[2],
            gyroX = gyroValues[0],
            gyroY = gyroValues[1],
            gyroZ = gyroValues[2],
            rotVectorX = rotVectorValues[0],
            rotVectorY = rotVectorValues[1],
            rotVectorZ = rotVectorValues[2],
            rotVectorScalar = rotVectorValues.getOrElse(3) { 0f }
        )

        sensorDataBuffer.add(sensorData)
        onDataCollected?.invoke(sensorData)
    }

    /**
     * Get current buffer size
     */
    fun getBufferSize(): Int = sensorDataBuffer.size

    /**
     * Get current session ID
     */
    fun getCurrentSessionId(): String = currentSessionId

    /**
     * Check if currently recording
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRecording) return

        // Update sensor values (all sensors update these arrays)
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelValues = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroValues = event.values.clone()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                rotVectorValues = event.values.clone()
            }
        }

        // Only log at fixed intervals to avoid redundant samples
        // This prevents logging the same data 3x (once per sensor update)
        val now = System.currentTimeMillis()
        if (now - lastLogTime >= LOG_INTERVAL_MS) {
            logEvent("idle")
            lastLogTime = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor.name}, accuracy: $accuracy")
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        if (isRecording) {
            stopRecording()
        }
        sensorManager.unregisterListener(this)
    }

    companion object {
        private const val TAG = "SensorDataCollector"
    }
}