package com.example.sensekey

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.example.sensekey.ui.theme.SenseKeyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinEntryActivity : ComponentActivity() {

    private lateinit var sensorCollector: SensorDataCollector
    private lateinit var csvExporter: CSVExporter

    // Permission launcher for storage access
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Storage permission required to save CSV files",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request storage permissions if needed
        requestStoragePermissions()

        // Initialize sensor collector and CSV exporter
        sensorCollector = SensorDataCollector(this)
        csvExporter = CSVExporter(this)

        // Check if sensors are available
        if (!sensorCollector.areSensorsAvailable()) {
            Toast.makeText(
                this,
                "Warning: Missing sensors: ${sensorCollector.getMissingSensors()}",
                Toast.LENGTH_LONG
            ).show()
        }

        setContent {
            SenseKeyTheme {
                PinEntryScreen(
                    sensorCollector = sensorCollector,
                    csvExporter = csvExporter,
                    lifecycleScope = lifecycleScope,
                    onPinCorrect = {
                        // Navigate to MainActivity when PIN is correct
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    private fun requestStoragePermissions() {
        // Android 13+ doesn't need storage permissions for app-specific storage
        // Android 10-12 needs WRITE_EXTERNAL_STORAGE
        // Android 9 and below needs both READ and WRITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: No permissions needed for public Documents folder with MediaStore
            // But we're using legacy API, so we still work on Android 13+
            return
        }

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                // Android 9 and below
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        if (permissions.isNotEmpty()) {
            storagePermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up sensor resources
        sensorCollector.cleanup()
    }
}

@Composable
fun PinEntryScreen(
    sensorCollector: SensorDataCollector,
    csvExporter: CSVExporter,
    lifecycleScope: CoroutineScope,
    onPinCorrect: () -> Unit
) {
    var participantId by remember { mutableStateOf("") }
    var showParticipantInput by remember { mutableStateOf(true) }
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    // Prevent double-click: track if currently saving data
    var isSavingData by remember { mutableStateOf(false) }
    var trialNumber by remember { mutableStateOf(1) }
    var sampleCount by remember { mutableStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Update sample count periodically if recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                sampleCount = sensorCollector.getBufferSize()
                delay(100) // Update every 100ms
            }
        }
    }

    // Show Participant ID input screen first
    if (showParticipantInput) {
        ParticipantIdInputScreen(
            onContinue = { id ->
                participantId = id
                showParticipantInput = false
            }
        )
        return
    }

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

            // Participant ID display with edit button and Trial number
            if (PinConfig.RESEARCH_MODE) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Participant: $participantId",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = { showParticipantInput = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            text = "Edit",
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Trial #$trialNumber",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

            // Research Mode UI
                // Target PIN Display
                Text(
                    text = "Target PIN: ${PinConfig.getCurrentResearchPin()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Recording Button or Recording Status
                if (!isRecording) {
                    Button(
                        onClick = {
                            isRecording = true
                            sensorCollector.startRecording(
                                trialNumber = trialNumber,
                                targetPin = PinConfig.getCurrentResearchPin()
                            )
                            errorMessage = ""
                            successMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Start Recording", fontSize = 16.sp)
                    }
                } else {
                    // Recording Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔴",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Recording...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sample Count
                    Text(
                        text = "($sampleCount samples)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            } else {
                // Normal mode subtitle
                Text(
                    text = "Enter PIN to continue",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(60.dp))
            }

            // PIN Dots Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(PinConfig.PIN_LENGTH) { index ->
                    PinDot(filled = index < pin.length)
                }
            }

            // Success Message
            if (successMessage.isNotEmpty()) {
                Text(
                    text = successMessage,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Error Message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Bottom Section: Number Pad
        NumberPad(
            onNumberClick = { number, touchX, touchY, touchPressure, touchSize ->
                android.util.Log.d("PinEntry", "Button clicked: $number, isRecording=$isRecording, pinLength=${pin.length}")

                // Only allow PIN entry if recording and no success message
                if (PinConfig.RESEARCH_MODE && !isRecording) {
                    android.util.Log.d("PinEntry", "Blocked: Not recording")

                    return@NumberPad  // Block input until recording starts
                }

                if (pin.length < PinConfig.PIN_LENGTH && !successMessage.isNotEmpty()) {
                    pin += number
                    errorMessage = ""
                    android.util.Log.d("PinEntry", "PIN updated: $pin")

                    // Log button press with touch data if recording
                    if (isRecording) {
                        sensorCollector.logButtonPress(
                            digit = number,
                            position = pin.length - 1,
                            touchX = touchX,
                            touchY = touchY,
                            touchPressure = touchPressure,
                            touchSize = touchSize
                        )
                        sensorCollector.updateCurrentPin(pin)
                    }

                    // Auto-validate when 4 digits are entered
                    if (pin.length == PinConfig.PIN_LENGTH) {
                        // Research mode: Handle data collection
                        if (PinConfig.RESEARCH_MODE && isRecording) {
                            // Check if PIN matches target (for feedback only, still save data)
                            val isCorrect = PinConfig.validatePin(pin)

                            // Prevent duplicate saves during delay
                            if (isSavingData) return@NumberPad
                            isSavingData = true

                            // Wait 800ms after 4th digit for post-interaction data
                            // Use lifecycleScope to ensure coroutine is cancelled if activity is destroyed
                            lifecycleScope.launch {
                                delay(800) // Post-4th-digit delay

                                // Stop recording and save
                                val collectedData = sensorCollector.stopRecording()
                                isRecording = false

                                // Export to CSV (save regardless of correctness)
                                val file = csvExporter.exportToCSV(
                                    data = collectedData,
                                    participantId = participantId,
                                    trialNumber = trialNumber,
                                    targetPin = PinConfig.getCurrentResearchPin(),
                                    isCorrect = isCorrect
                                )

                                if (file != null) {
                                    val correctness = if (isCorrect) "✓" else "✗"
                                    successMessage = "Trial #$trialNumber saved! $correctness (${collectedData.size} samples)"

                                    // Only advance to next PIN and increment trial if correct
                                    if (isCorrect) {
                                        PinConfig.nextResearchPin()
                                        trialNumber++

                                        // Reset trial number after completing all 22 PINs
                                        if (trialNumber > PinConfig.RESEARCH_PINS.size) {
                                            trialNumber = 1
                                        }
                                    }
                                    // If wrong, stay on same trial number and same PIN

                                    // Show success message for 2 seconds, then reset
                                    delay(2000)
                                    successMessage = ""
                                    pin = ""
                                    isSavingData = false
                                } else {
                                    errorMessage = "Failed to save data"
                                    delay(2000)
                                    errorMessage = ""
                                    pin = ""
                                    isSavingData = false
                                }
                            }
                        } else {
                            // Normal mode: Validate PIN
                            if (PinConfig.validatePin(pin)) {
                                onPinCorrect()
                            } else {
                                errorMessage = "Incorrect PIN"
                                pin = ""
                            }
                        }
                    }
                }
            },
            onDeleteClick = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                    errorMessage = ""

                    // Update current PIN if recording
                    if (isRecording) {
                        sensorCollector.updateCurrentPin(pin)
                    }
                }
            },
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
fun PinDot(filled: Boolean) {
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
fun NumberPad(
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
                    RectangularNumberButton(
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
            RectangularNumberButton(
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
fun RectangularNumberButton(
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

@Composable
fun ParticipantIdInputScreen(
    onContinue: (String) -> Unit
) {
    var participantId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "SenseKey",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Research Data Collection",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Input Field Label
        Text(
            text = "Enter Participant ID",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Text Input
        OutlinedTextField(
            value = participantId,
            onValueChange = {
                participantId = it
                errorMessage = ""
            },
            label = { Text("Participant ID") },
            placeholder = { Text("e.g., 001") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Helper Text
        Text(
            text = "Use format: 001, 002, 003... or names",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue Button
        Button(
            onClick = {
                if (participantId.isBlank()) {
                    errorMessage = "Please enter a participant ID"
                } else {
                    onContinue(participantId.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Start Data Collection",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start Predicting Button
        Button(
            onClick = {
                val intent = Intent(context, PredictionActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "Start Predicting",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info text
        Text(
            text = "You'll collect data for 22 different PIN patterns",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}