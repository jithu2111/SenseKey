package com.example.sensekey

/**
 * Data class representing a single sensor reading sample
 */
data class SensorData(
    val timestamp: Long,              // System timestamp in milliseconds
    val timeFromStart: Long,          // Time from recording start in milliseconds
    val sessionId: String,            // Unique ID for this PIN entry session
    val trialNumber: Int,             // Trial number in this session
    val participantId: String,        // Participant ID
    val handMode: String,             // Hand holding mode (Left, Right, Both, etc.)
    val targetPin: String,            // Target PIN for research mode (what should be entered)
    val pinEntered: String,           // The actual PIN being entered
    val isCorrect: Int,               // 1 if correct, 0 if incorrect

    // Event information
    val eventType: String,            // "recording_start", "idle", "button_press", "recording_stop"
    val digitPressed: String?,        // The digit that was pressed (null if not a button press)
    val digitPosition: Int?,          // Position in PIN (0-3, null if not a button press)

    // Accelerometer data (m/s²)
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,

    // Gyroscope data (rad/s)
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,

    // Rotation Vector data (unitless)
    val rotVectorX: Float,
    val rotVectorY: Float,
    val rotVectorZ: Float,
    val rotVectorScalar: Float,

    // Derived features (calculated at collection time)
    val accelMagnitude: Float,        // Overall acceleration magnitude
    val gyroMagnitude: Float,         // Overall rotation magnitude
    val rotMagnitude: Float,          // Overall rotation vector magnitude

    // Touch interaction data (only for button_press events)
    val touchX: Float?,               // X coordinate of touch (pixels)
    val touchY: Float?,               // Y coordinate of touch (pixels)
    val touchPressure: Float?,        // Touch pressure (0.0-1.0, if available)
    val touchSize: Float?             // Touch contact area (0.0-1.0, if available)
) {
    /**
     * Convert to CSV row format
     */
    fun toCsvRow(): String {
        return "$sessionId,$trialNumber,$participantId,$handMode,$targetPin,$pinEntered,$isCorrect,$timestamp,$timeFromStart," +
                "$accelX,$accelY,$accelZ," +
                "$gyroX,$gyroY,$gyroZ," +
                "$rotVectorX,$rotVectorY,$rotVectorZ,$rotVectorScalar," +
                "$eventType,${digitPressed ?: ""},${digitPosition ?: ""}," +
                "$accelMagnitude,$gyroMagnitude,$rotMagnitude," +
                "${touchX ?: ""},${touchY ?: ""},${touchPressure ?: ""},${touchSize ?: ""}"
    }

    companion object {
        /**
         * CSV header row
         */
        fun getCsvHeader(): String {
            return "session_id,trial_number,participant_id,hand_mode,target_pin,pin_entered,is_correct,timestamp_ms,time_from_start_ms," +
                    "accel_x,accel_y,accel_z," +
                    "gyro_x,gyro_y,gyro_z," +
                    "rot_x,rot_y,rot_z,rot_scalar," +
                    "event_type,digit_pressed,digit_position," +
                    "accel_magnitude,gyro_magnitude,rot_magnitude," +
                    "touch_x,touch_y,touch_pressure,touch_size"
        }
    }
}