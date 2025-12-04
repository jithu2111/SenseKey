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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch

class PredictionActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var rotationVector: Sensor? = null

    private lateinit var pinPredictor: PinPredictor
    private val sensorBuffer = PinPredictor.SensorBuffer()
    
    // NEW: Backend prediction service
    private lateinit var predictionService: PinPredictionService

    // Current sensor values
    private var accelValues = FloatArray(3) { 0f }
    private var gyroValues = FloatArray(3) { 0f }
    private var rotVectorValues = FloatArray(4) { 0f }

    // Prediction state
    private var showKeypad = mutableStateOf(false)     // Show keypad screen
    private var isRecording = mutableStateOf(false)   // Recording sensor data
    private var typedPin = mutableStateOf("")         // Actually typed PIN
    private var resultMessage = mutableStateOf("")
    private var apiPredictedPin = mutableStateOf("") // API prediction result
    private var pinLength = mutableStateOf(4)         // PIN length (3 or 4)
    private var showSendingModal = mutableStateOf(false) // Show "sending data" modal
    private var showResponseModal = mutableStateOf(false) // Show API response modal
    private var apiResponseMessage = mutableStateOf("") // API response message

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Initialize predictors
        pinPredictor = PinPredictor(this)
        predictionService = PinPredictionService(this)

        // Start sensors
        startSensors()

        setContent {
            SenseKeyTheme {
                PredictionScreen(
                    showKeypad = showKeypad.value,
                    isRecording = isRecording.value,
                    typedPin = typedPin.value,
                    pinLength = pinLength.value,
                    apiPredictedPin = apiPredictedPin.value,
                    showSendingModal = showSendingModal.value,
                    showResponseModal = showResponseModal.value,
                    apiResponseMessage = apiResponseMessage.value,
                    onSelectMode = { mode ->
                        pinLength.value = mode
                        showKeypad.value = true
                        isRecording.value = true
                        predictionService.clearData()
                        typedPin.value = ""
                    },
                    onButtonClick = { number, touchX, touchY, pressTime, releaseTime ->
                        handleButtonClick(number, touchX, touchY, pressTime, releaseTime)
                    },
                    onDeleteClick = {
                        if (typedPin.value.isNotEmpty()) {
                            typedPin.value = typedPin.value.dropLast(1)
                        }
                    },
                    onOkClick = {
                        // Call API with typed PIN and collected data
                        lifecycleScope.launch {
                            showSendingModal.value = true
                            try {
                                val apiResult = predictionService.predictPin(typedPin.value)
                                showSendingModal.value = false
                                apiResult.onSuccess { apiPin ->
                                    apiPredictedPin.value = apiPin
                                    apiResponseMessage.value = "Predicted PIN: $apiPin"
                                    showResponseModal.value = true
                                    android.util.Log.d("PredictionActivity", "API predicted PIN: $apiPin")
                                }.onFailure { error ->
                                    apiResponseMessage.value = "Error: ${error.message}"
                                    showResponseModal.value = true
                                    android.util.Log.e("PredictionActivity", "API prediction failed: ${error.message}")
                                }
                            } catch (e: Exception) {
                                showSendingModal.value = false
                                apiResponseMessage.value = "Error: ${e.message}"
                                showResponseModal.value = true
                                android.util.Log.e("PredictionActivity", "API call exception", e)
                            }
                        }
                    },
                    onDismissResponseModal = {
                        showResponseModal.value = false
                    },
                    onReset = {
                        showKeypad.value = false
                        isRecording.value = false
                        typedPin.value = ""
                        apiPredictedPin.value = ""
                        showSendingModal.value = false
                        showResponseModal.value = false
                        apiResponseMessage.value = ""
                        predictionService.clearData()
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

    private fun handleButtonClick(number: String, touchX: Float?, touchY: Float?, pressTime: Long?, releaseTime: Long?) {
        // Only accept input when recording and PIN not complete
        if (!isRecording.value || typedPin.value.length >= pinLength.value) return

        android.util.Log.d("PredictionActivity", "Recording digit: $number")

        // Add touch data to API service (for backward compatibility)
        predictionService.addTouchData(touchX, touchY, 0f, 0f)

        // Add keystroke event with press/release timestamps (CRITICAL for temporal features)
        if (pressTime != null && releaseTime != null) {
            predictionService.addKeystrokeEvent(number, pressTime, releaseTime)
            android.util.Log.d("PredictionActivity", "Keystroke: digit=$number, press=$pressTime, release=$releaseTime, duration=${releaseTime - pressTime}ms")
        } else {
            // Fallback: use current time if timestamps not provided
            val now = System.currentTimeMillis()
            predictionService.addKeystrokeEvent(number, now - 100, now)  // Assume 100ms duration
        }

        // Add the typed digit to our PIN
        typedPin.value += number

        android.util.Log.d("PredictionActivity", "Typed PIN so far: ${typedPin.value}")
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
                
                // Add sensor data to API service when recording
                if (isRecording.value) {
                    predictionService.addSensorData(
                        accelX = accelValues[0],
                        accelY = accelValues[1], 
                        accelZ = accelValues[2],
                        gyroX = gyroValues[0],
                        gyroY = gyroValues[1],
                        gyroZ = gyroValues[2],
                        rotX = rotVectorValues[0],
                        rotY = rotVectorValues[1],
                        rotZ = rotVectorValues[2],
                        rotScalar = rotVectorValues.getOrElse(3) { 0f }
                    )
                }
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
    showKeypad: Boolean,
    isRecording: Boolean,
    typedPin: String,
    pinLength: Int,
    apiPredictedPin: String,
    showSendingModal: Boolean,
    showResponseModal: Boolean,
    apiResponseMessage: String,
    onSelectMode: (Int) -> Unit,
    onButtonClick: (String, Float?, Float?, Long?, Long?) -> Unit,
    onDeleteClick: () -> Unit,
    onOkClick: () -> Unit,
    onDismissResponseModal: () -> Unit,
    onReset: () -> Unit
) {
    // Sending data modal
    if (showSendingModal) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismiss while sending */ },
            title = {
                Text("Sending Data", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Please wait...", fontSize = 16.sp)
                }
            },
            confirmButton = {}
        )
    }

    // API response modal
    if (showResponseModal) {
        AlertDialog(
            onDismissRequest = onDismissResponseModal,
            title = {
                Text("API Response", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(apiResponseMessage, fontSize = 16.sp)
            },
            confirmButton = {
                Button(onClick = onDismissResponseModal) {
                    Text("OK")
                }
            }
        )
    }

    if (!showKeypad) {
        // Mode selection screen
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "SenseKey",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Select PIN Mode",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onSelectMode(3) },
                    modifier = Modifier.width(200.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("3 Digit Mode", fontSize = 18.sp)
                }
                Button(
                    onClick = { onSelectMode(4) },
                    modifier = Modifier.width(200.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("4 Digit Mode", fontSize = 18.sp)
                }
            }
        }
    } else {
        // Keypad screen
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

                // PIN Dots Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    repeat(pinLength) { index ->
                        PredictionPinDot(filled = index < typedPin.length)
                    }
                }

                // Show API predicted PIN if available
                if (apiPredictedPin.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Predicted: $apiPredictedPin",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom Section: Number Pad
            PredictionNumberPad(
                onNumberClick = { number, touchX, touchY, pressTime, releaseTime ->
                    onButtonClick(number, touchX, touchY, pressTime, releaseTime)
                },
                onDeleteClick = onDeleteClick,
                modifier = Modifier.padding(bottom = 24.dp),
                onOkClick = if (typedPin.length == pinLength) onOkClick else null
            )
        }
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
    onNumberClick: (String, Float?, Float?, Long?, Long?) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOkClick: (() -> Unit)? = null
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
                        onClick = { touchX, touchY, pressTime, releaseTime ->
                            onNumberClick(number, touchX, touchY, pressTime, releaseTime)
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
                onClick = { touchX, touchY, pressTime, releaseTime ->
                    onNumberClick("0", touchX, touchY, pressTime, releaseTime)
                }
            )

            // OK/Enter button (if onOkClick is provided)
            if (onOkClick != null) {
                Button(
                    onClick = onOkClick,
                    modifier = Modifier
                        .width(95.dp)
                        .height(72.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "OK",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Spacer button (invisible, maintains layout symmetry)
                Box(
                    modifier = Modifier
                        .width(95.dp)
                        .height(72.dp)
                )
            }
        }
    }
}

@Composable
fun PredictionRectangularNumberButton(
    number: String,
    label: String,
    onClick: (Float?, Float?, Long?, Long?) -> Unit
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

                    // Capture initial touch data and PRESS timestamp
                    val touchX = down.position.x
                    val touchY = down.position.y
                    val pressTimestamp = System.currentTimeMillis()

                    android.util.Log.d(
                        "NumberButton",
                        "Touch DOWN on $number: x=$touchX, y=$touchY, pressTime=$pressTimestamp"
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
                            // Capture RELEASE timestamp
                            val releaseTimestamp = System.currentTimeMillis()
                            val duration = releaseTimestamp - pressTimestamp
                            
                            // Consume the event
                            event.changes.forEach { it.consume() }
                            // Trigger the click callback with touch data AND timestamps
                            onClick(touchX, touchY, pressTimestamp, releaseTimestamp)
                            android.util.Log.d(
                                "NumberButton",
                                "Released $number - press=$pressTimestamp, release=$releaseTimestamp, duration=${duration}ms"
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