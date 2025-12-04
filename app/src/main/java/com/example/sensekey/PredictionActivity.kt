package com.example.sensekey

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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset

class PredictionActivity : ComponentActivity() {

    private lateinit var predictionService: PinPredictionService

    // State
    private var showKeypad = mutableStateOf(false)
    private var isRecording = mutableStateOf(false)
    private var hasStartedRecording = mutableStateOf(false)
    private var typedPin = mutableStateOf("")
    private var apiPredictedPin = mutableStateOf("")
    private var pinLength = mutableStateOf(4)
    private var showSendingModal = mutableStateOf(false)
    private var showResponseModal = mutableStateOf(false)
    private var apiResponseMessage = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize simplified service
        predictionService = PinPredictionService(this)

        setContent {
            SenseKeyTheme {
                PredictionScreen(
                    showKeypad = showKeypad.value,
                    isRecording = isRecording.value,
                    hasStartedRecording = hasStartedRecording.value,
                    typedPin = typedPin.value,
                    pinLength = pinLength.value,
                    apiPredictedPin = apiPredictedPin.value,
                    showSendingModal = showSendingModal.value,
                    showResponseModal = showResponseModal.value,
                    apiResponseMessage = apiResponseMessage.value,
                    onStartRecording = {
                        hasStartedRecording.value = true
                        isRecording.value = true
                        predictionService.clearData() // Start fresh
                    },
                    onSelectMode = { mode ->
                        pinLength.value = mode
                        showKeypad.value = true
                        typedPin.value = ""
                    },
                    onButtonClick = { number, touchX, touchY ->
                        handleButtonClick(number, touchX, touchY)
                    },
                    onDeleteClick = {
                        if (typedPin.value.isNotEmpty()) typedPin.value = typedPin.value.dropLast(1)
                    },
                    onOkClick = { }, // Not used in auto-submit flow
                    onDismissResponseModal = {
                        showResponseModal.value = false
                        // Clean up for next attempt
                        predictionService.clearData()
                        typedPin.value = ""
                        apiPredictedPin.value = ""
                    },
                    onReset = {
                        showKeypad.value = false
                        isRecording.value = false
                        hasStartedRecording.value = false
                        typedPin.value = ""
                        apiPredictedPin.value = ""
                        showSendingModal.value = false
                        showResponseModal.value = false
                        predictionService.clearData()
                    }
                )
            }
        }
    }

    private fun handleButtonClick(number: String, touchX: Float?, touchY: Float?) {
        // Only record if we are in recording mode and PIN isn't full
        if (!hasStartedRecording.value || !isRecording.value || typedPin.value.length >= pinLength.value) return

        // UPDATED: Capture touch coordinate directly (ignore timestamps)
        if (touchX != null && touchY != null) {
            val normalizedX = touchX - 80f
            val normalizedY = touchY - 1200f
            predictionService.addTouchData(normalizedX, normalizedY)
        }

        typedPin.value += number

        // When PIN is complete, send request immediately
        if (typedPin.value.length == pinLength.value) {
            showSendingModal.value = true

            lifecycleScope.launch {
                val result = predictionService.predictPin(typedPin.value)

                showSendingModal.value = false
                result.onSuccess { pin ->
                    apiPredictedPin.value = pin
                    apiResponseMessage.value = "Predicted PIN: $pin"
                    showResponseModal.value = true
                }.onFailure { e ->
                    apiResponseMessage.value = "Error: ${e.message}"
                    showResponseModal.value = true
                }
            }
        }
    }
}

// --- UI Components ---

@Composable
fun PredictionScreen(
    showKeypad: Boolean,
    isRecording: Boolean,
    hasStartedRecording: Boolean,
    typedPin: String,
    pinLength: Int,
    apiPredictedPin: String,
    showSendingModal: Boolean,
    showResponseModal: Boolean,
    apiResponseMessage: String,
    onStartRecording: () -> Unit,
    onSelectMode: (Int) -> Unit,
    onButtonClick: (String, Float?, Float?) -> Unit,
    onDeleteClick: () -> Unit,
    onOkClick: () -> Unit,
    onDismissResponseModal: () -> Unit,
    onReset: () -> Unit
) {
    if (showSendingModal) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Sending Data") },
            text = { Text("Please wait...") },
            confirmButton = {}
        )
    }

    if (showResponseModal) {
        AlertDialog(
            onDismissRequest = onDismissResponseModal,
            title = { Text("API Response") },
            text = { Text(apiResponseMessage) },
            confirmButton = {
                Button(onClick = onDismissResponseModal) { Text("OK") }
            }
        )
    }

    if (!showKeypad) {
        // Mode Selection
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("SenseKey", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Select PIN Mode")
                Button(onClick = { onSelectMode(3) }) { Text("3 Digit Mode") }
                Button(onClick = { onSelectMode(4) }) { Text("4 Digit Mode") }
            }
        }
    } else {
        // Keypad
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
                Text("SenseKey", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(20.dp))

                if (!hasStartedRecording) {
                    Button(onClick = onStartRecording) { Text("Start Recording") }
                } else {
                    Text("Recording...", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pin dots
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(pinLength) { index ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (index < typedPin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        )
                    }
                }
            }

            PredictionNumberPad(
                onNumberClick = onButtonClick,
                onDeleteClick = onDeleteClick,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun PredictionNumberPad(
    onNumberClick: (String, Float?, Float?) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (col in 1..3) {
                    val number = (row * 3 + col).toString()
                    PredictionRectangularNumberButton(number = number) { x, y ->
                        onNumberClick(number, x, y)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Delete button placeholder/functioning
            Button(
                onClick = onDeleteClick,
                modifier = Modifier.width(95.dp).height(72.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) { Text("⌫", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            PredictionRectangularNumberButton(number = "0") { x, y ->
                onNumberClick("0", x, y)
            }

            // Empty placeholder for layout balance
            Box(modifier = Modifier.width(95.dp).height(72.dp))
        }
    }
}

@Composable
fun PredictionRectangularNumberButton(
    number: String,
    label: String = "", // Added default empty string to match usage
    onClick: (Float?, Float?) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Store the absolute position of this button on the screen
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .width(95.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            // 1. Get the button's absolute X/Y on the screen
            .onGloballyPositioned { coordinates ->
                buttonPosition = coordinates.positionInRoot()
            }
            .pointerInput(number) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true

                    // 2. Add the local touch offset to the button's absolute position
                    val absTouchX = buttonPosition.x + down.position.x
                    val absTouchY = buttonPosition.y + down.position.y

                    // Wait for release
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })

                    isPressed = false

                    // 3. Send the CALCULATED absolute coordinates
                    onClick(absTouchX, absTouchY)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Handle optional label if your UI needs it
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}