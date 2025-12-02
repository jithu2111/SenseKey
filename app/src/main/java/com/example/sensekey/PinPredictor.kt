package com.example.sensekey

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * PIN prediction using ONNX model
 * Expects 17 features per button press
 */
class PinPredictor(context: Context) {

    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession

    init {
        // Load model from assets
        val modelBytes = context.assets.open("pin_model.onnx").readBytes()
        ortSession = ortEnvironment.createSession(modelBytes)
    }

    /**
     * Feature buffer to collect sensor samples for averaging
     */
    data class SensorBuffer(
        val accelX: MutableList<Float> = mutableListOf(),
        val accelY: MutableList<Float> = mutableListOf(),
        val accelZ: MutableList<Float> = mutableListOf(),
        val gyroX: MutableList<Float> = mutableListOf(),
        val gyroY: MutableList<Float> = mutableListOf(),
        val gyroZ: MutableList<Float> = mutableListOf()
    ) {
        fun add(
            accelX: Float, accelY: Float, accelZ: Float,
            gyroX: Float, gyroY: Float, gyroZ: Float
        ) {
            this.accelX.add(accelX)
            this.accelY.add(accelY)
            this.accelZ.add(accelZ)
            this.gyroX.add(gyroX)
            this.gyroY.add(gyroY)
            this.gyroZ.add(gyroZ)

            // Keep only last 8 samples
            if (this.accelX.size > 8) {
                this.accelX.removeAt(0)
                this.accelY.removeAt(0)
                this.accelZ.removeAt(0)
                this.gyroX.removeAt(0)
                this.gyroY.removeAt(0)
                this.gyroZ.removeAt(0)
            }
        }

        fun clear() {
            accelX.clear()
            accelY.clear()
            accelZ.clear()
            gyroX.clear()
            gyroY.clear()
            gyroZ.clear()
        }

        fun size(): Int = accelX.size
    }

    /**
     * Extract 17 features from sensor data and touch coordinates
     *
     * Feature order:
     * 1. touch_x (Raw)
     * 2. touch_y (Raw)
     * 3. rot_x (Raw)
     * 4. rot_y (Raw)
     * 5. rot_z (Raw)
     * 6. rot_scalar (Raw)
     * 7. accel_x_mean (Average of last 8 samples)
     * 8. accel_y_mean (Average of last 8 samples)
     * 9. accel_z_mean (Average of last 8 samples)
     * 10. gyro_x_mean (Average of last 8 samples)
     * 11. gyro_y_mean (Average of last 8 samples)
     * 12. gyro_z_mean (Average of last 8 samples)
     * 13. accel_mag_mean (Average Total Acceleration)
     * 14. gyro_mag_mean (Average Total Rotation)
     * 15. accel_z_nograv_mean (Average Accel Z minus 9.8)
     * 16. accel_mag_std (Stability/Shake of hand)
     * 17. gyro_mag_std (Stability/Shake of rotation)
     */
    fun extractFeatures(
        touchX: Float?,
        touchY: Float?,
        rotX: Float,
        rotY: Float,
        rotZ: Float,
        rotScalar: Float,
        sensorBuffer: SensorBuffer
    ): FloatArray {
        if (sensorBuffer.size() == 0) {
            // Return zeros if no sensor data available
            return FloatArray(17) { 0f }
        }

        // Features 1-6: Raw touch and rotation data
        val features = FloatArray(17)
        features[0] = touchX ?: 0f
        features[1] = touchY ?: 0f
        features[2] = rotX
        features[3] = rotY
        features[4] = rotZ
        features[5] = rotScalar

        // Features 7-12: Mean of accelerometer and gyroscope (last 8 samples)
        features[6] = sensorBuffer.accelX.average().toFloat()  // accel_x_mean
        features[7] = sensorBuffer.accelY.average().toFloat()  // accel_y_mean
        features[8] = sensorBuffer.accelZ.average().toFloat()  // accel_z_mean
        features[9] = sensorBuffer.gyroX.average().toFloat()   // gyro_x_mean
        features[10] = sensorBuffer.gyroY.average().toFloat()  // gyro_y_mean
        features[11] = sensorBuffer.gyroZ.average().toFloat()  // gyro_z_mean

        // Feature 13: accel_mag_mean (Average Total Acceleration)
        val accelMagnitudes = sensorBuffer.accelX.indices.map { i ->
            sqrt(
                sensorBuffer.accelX[i] * sensorBuffer.accelX[i] +
                sensorBuffer.accelY[i] * sensorBuffer.accelY[i] +
                sensorBuffer.accelZ[i] * sensorBuffer.accelZ[i]
            )
        }
        features[12] = accelMagnitudes.average().toFloat()

        // Feature 14: gyro_mag_mean (Average Total Rotation)
        val gyroMagnitudes = sensorBuffer.gyroX.indices.map { i ->
            sqrt(
                sensorBuffer.gyroX[i] * sensorBuffer.gyroX[i] +
                sensorBuffer.gyroY[i] * sensorBuffer.gyroY[i] +
                sensorBuffer.gyroZ[i] * sensorBuffer.gyroZ[i]
            )
        }
        features[13] = gyroMagnitudes.average().toFloat()

        // Feature 15: accel_z_nograv_mean (Average Accel Z minus 9.8)
        val accelZNoGrav = sensorBuffer.accelZ.map { it - 9.8f }
        features[14] = accelZNoGrav.average().toFloat()

        // Feature 16: accel_mag_std (Stability/Shake of hand)
        features[15] = calculateStdDev(accelMagnitudes)

        // Feature 17: gyro_mag_std (Stability/Shake of rotation)
        features[16] = calculateStdDev(gyroMagnitudes)

        return features
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStdDev(values: List<Float>): Float {
        if (values.isEmpty()) return 0f

        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    /**
     * Predict digit from features
     * @return Predicted digit (0-9) or null if prediction fails
     */
    fun predict(features: FloatArray): String? {
        try {
            // Create input tensor (1 x 17)
            val inputShape = longArrayOf(1, 17)
            val inputBuffer = FloatBuffer.wrap(features)
            val inputTensor = OnnxTensor.createTensor(ortEnvironment, inputBuffer, inputShape)

            // Run inference
            val results = ortSession.run(mapOf("float_input" to inputTensor))

            // Get output tensor and convert to predicted digit
            val outputValue = results[0].value
            val predictedDigit = when (outputValue) {
                is LongArray -> outputValue[0].toInt().toString()
                is FloatArray -> outputValue[0].toInt().toString() // Model outputs float
                is Array<*> -> (outputValue[0] as? Long)?.toInt()?.toString()
                    ?: (outputValue[0] as? Float)?.toInt()?.toString()
                    ?: outputValue[0].toString()
                else -> outputValue.toString()
            }

            android.util.Log.d("PinPredictor", "Output type: ${outputValue.javaClass.simpleName}, value: $outputValue, digit: $predictedDigit")

            inputTensor.close()
            results.close()

            return predictedDigit

        } catch (e: Exception) {
            android.util.Log.e("PinPredictor", "Prediction error: ${e.message}", e)
            return null
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        ortSession.close()
    }
}