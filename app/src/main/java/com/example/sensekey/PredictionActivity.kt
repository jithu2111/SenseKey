package com.example.sensekey

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensekey.ui.theme.SenseKeyTheme
import kotlinx.coroutines.delay

class PredictionActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var rotationVector: Sensor? = null

    private lateinit var pinPredictor: PinPredictor
    private val sensorBuffer = PinPredictor.SensorBuffer()

    // Current sensor values
    private var accelValues = FloatArray(3) { 0f }
    private var gyroValues = FloatArray(3) { 0f }
    private var rotVectorValues = FloatArray(4) { 0f }

    // Prediction state
    private var isPredicting = mutableStateOf(false)
    private var predictedPin = mutableStateOf("")
    private var resultMessage = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Initialize predictor
        pinPredictor = PinPredictor(this)

        // Start sensors
        startSensors()

        setContent {
            SenseKeyTheme {
                PredictionScreen(
                    isPredicting = isPredicting.value,
                    predictedPin = predictedPin.value,
                    resultMessage = resultMessage.value,
                    onButtonClick = { number, touchX, touchY ->
                        handleButtonClick(number, touchX, touchY)
                    },
                    onDeleteClick = {
                        if (predictedPin.value.isNotEmpty()) {
                            predictedPin.value = predictedPin.value.dropLast(1)
                            resultMessage.value = ""
                        }
                    },
                    onStartPredicting = {
                        startPredicting()
                    },
                    onReset = {
                        resetPrediction()
                    }
                )
            }
        }
    }

    private fun startSensors() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    private fun startPredicting() {
        isPredicting.value = true
        predictedPin.value = ""
        resultMessage.value = ""
        sensorBuffer.clear()
    }

    private fun handleButtonClick(number: String, touchX: Float?, touchY: Float?) {
        if (!isPredicting.value) return
        if (predictedPin.value.length >= 4) return

        android.util.Log.d("PredictionActivity", "Buffer size at button press: ${sensorBuffer.size()}")

        // Extract features with LAST 8 samples in buffer (snapshot at moment of touch)
        val features = pinPredictor.extractFeatures(
            touchX = touchX,
            touchY = touchY,
            rotX = rotVectorValues[0],
            rotY = rotVectorValues[1],
            rotZ = rotVectorValues[2],
            rotScalar = rotVectorValues.getOrElse(3) { 0f },
            sensorBuffer = sensorBuffer
        )

        // Predict digit
        val prediction = pinPredictor.predict(features)

        android.util.Log.d("PredictionActivity", "Button pressed: $number, Predicted: $prediction")
        android.util.Log.d("PredictionActivity", "Features: ${features.joinToString()}")

        if (prediction != null) {
            predictedPin.value += prediction

            // Check if PIN is complete
            if (predictedPin.value.length == 4) {
                resultMessage.value = "Entered PIN is: ${predictedPin.value}"
                isPredicting.value = false
            }
        } else {
            resultMessage.value = "Prediction failed"
        }

        // DO NOT clear buffer - it continues rolling for next button press
        // The buffer automatically maintains only last 8 samples
    }

    private fun resetPrediction() {
        isPredicting.value = false
        predictedPin.value = ""
        resultMessage.value = ""
        sensorBuffer.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelValues = event.values.clone()
                // Always maintain rolling buffer of last 8 samples (even when not predicting)
                // This ensures we have fresh data ready when user taps
                sensorBuffer.add(
                    accelValues[0], accelValues[1], accelValues[2],
                    gyroValues[0], gyroValues[1], gyroValues[2]
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroValues = event.values.clone()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                rotVectorValues = event.values.clone()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not needed
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        pinPredictor.close()
    }
}

@Composable
fun PredictionScreen(
    isPredicting: Boolean,
    predictedPin: String,
    resultMessage: String,
    onButtonClick: (String, Float?, Float?) -> Unit,
    onDeleteClick: () -> Unit,
    onStartPredicting: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Title and PIN Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            // App Title
            Text(
                text = "SenseKey",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "PIN Prediction Mode",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Start Predicting Button or Status
            if (!isPredicting && predictedPin.isEmpty()) {
                Button(
                    onClick = onStartPredicting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Start Predicting", fontSize = 16.sp)
                }
            } else if (isPredicting) {
                Text(
                    text = "🔴 Predicting...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (!isPredicting && predictedPin.isNotEmpty()) {
                // Prediction complete - show reset button
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Reset", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // PIN Dots Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(PinConfig.PIN_LENGTH) { index ->
                    PredictionPinDot(filled = index < predictedPin.length)
                }
            }

            // Result Message
            if (resultMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resultMessage,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Section: Number Pad
        PredictionNumberPad(
            onNumberClick = { number, touchX, touchY, touchPressure, touchSize ->
                onButtonClick(number, touchX, touchY)
            },
            onDeleteClick = onDeleteClick,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
fun PredictionPinDot(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )
    )
}

@Composable
fun PredictionNumberPad(
    onNumberClick: (String, Float?, Float?, Float?, Float?) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rows 1-3 (numbers 1-9)
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (col in 1..3) {
                    val number = (row * 3 + col).toString()
                    val label = when(number) {
                        "2" -> "ABC"
                        "3" -> "DEF"
                        "4" -> "GHI"
                        "5" -> "JKL"
                        "6" -> "MNO"
                        "7" -> "PQRS"
                        "8" -> "TUV"
                        "9" -> "WXYZ"
                        else -> ""
                    }
                    PredictionRectangularNumberButton(
                        number = number,
                        label = label,
                        onClick = { touchX, touchY, touchPressure, touchSize ->
                            onNumberClick(number, touchX, touchY, touchPressure, touchSize)
                        }
                    )
                }
            }
        }

        // Bottom row (delete, 0, enter)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Delete button
            Button(
                onClick = onDeleteClick,
                modifier = Modifier
                    .width(95.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "⌫",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 0 button
            PredictionRectangularNumberButton(
                number = "0",
                label = "",
                onClick = { touchX, touchY, touchPressure, touchSize ->
                    onNumberClick("0", touchX, touchY, touchPressure, touchSize)
                }
            )

            // Spacer button (invisible, maintains layout symmetry)
            Box(
                modifier = Modifier
                    .width(95.dp)
                    .height(72.dp)
            )
        }
    }
}

@Composable
fun PredictionRectangularNumberButton(
    number: String,
    label: String,
    onClick: (Float?, Float?, Float?, Float?) -> Unit
) {
    // Track pressed state for visual feedback
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(95.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPressed)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .pointerInput(number) {
                awaitEachGesture {
                    // Wait for the first touch down
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Show pressed state
                    isPressed = true

                    // Capture initial touch data
                    val touchX = down.position.x
                    val touchY = down.position.y
                    val touchPressure = down.pressure
                    val touchSize = down.pressure // Using pressure as proxy for size

                    android.util.Log.d(
                        "NumberButton",
                        "Touch on $number: x=$touchX, y=$touchY, pressure=$touchPressure"
                    )

                    // Wait for all pointers to be released
                    var released = false
                    while (!released) {
                        val event = awaitPointerEvent()
                        // Check if all pointers are up (released)
                        if (event.changes.all { !it.pressed }) {
                            released = true
                            // Remove pressed state
                            isPressed = false
                            // Consume the event
                            event.changes.forEach { it.consume() }
                            // Trigger the click callback with touch data
                            onClick(touchX, touchY, touchPressure, touchSize)
                            android.util.Log.d(
                                "NumberButton",
                                "Released $number - calling onClick with data"
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}