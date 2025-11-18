package com.example.sensekey

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.sensekey.ui.theme.SenseKeyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinEntryActivity : ComponentActivity() {

    private lateinit var sensorCollector: SensorDataCollector
    private lateinit var csvExporter: CSVExporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
    onPinCorrect: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var trialNumber by remember { mutableStateOf(1) }
    var sampleCount by remember { mutableStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Update sample count periodically if recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                sampleCount = sensorCollector.getBufferSize()
                delay(100) // Update every 100ms
            }
        }
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

            // Research Mode UI
            if (PinConfig.RESEARCH_MODE) {
                // Target PIN Display
                Text(
                    text = "Target PIN: ${PinConfig.getCurrentResearchPin()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Trial Counter
                Text(
                    text = "Trial #$trialNumber",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
            onNumberClick = { number ->
                if (pin.length < PinConfig.PIN_LENGTH && !successMessage.isNotEmpty()) {
                    pin += number
                    errorMessage = ""

                    // Log button press if recording
                    if (isRecording) {
                        sensorCollector.logButtonPress(number, pin.length - 1)
                        sensorCollector.updateCurrentPin(pin)
                    }

                    // Auto-validate when 4 digits are entered
                    if (pin.length == PinConfig.PIN_LENGTH) {
                        // Research mode: Handle data collection
                        if (PinConfig.RESEARCH_MODE && isRecording) {
                            // Check if PIN matches target (for feedback only, still save data)
                            val isCorrect = PinConfig.validatePin(pin)

                            // Wait 800ms after 4th digit for post-interaction data
                            coroutineScope.launch {
                                delay(800) // Post-4th-digit delay

                                // Stop recording and save
                                val collectedData = sensorCollector.stopRecording()
                                isRecording = false

                                // Export to CSV (save regardless of correctness)
                                val file = csvExporter.exportToCSV(collectedData)

                                if (file != null) {
                                    val correctness = if (isCorrect) "✓" else "✗"
                                    successMessage = "Trial #$trialNumber saved! $correctness (${collectedData.size} samples)"
                                    trialNumber++

                                    // Show success message for 2 seconds, then reset
                                    delay(2000)
                                    successMessage = ""
                                    pin = ""
                                } else {
                                    errorMessage = "Failed to save data"
                                    delay(2000)
                                    errorMessage = ""
                                    pin = ""
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
    onNumberClick: (String) -> Unit,
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
                        onClick = { onNumberClick(number) }
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
                    .width(105.dp)
                    .height(64.dp),
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
                onClick = { onNumberClick("0") }
            )

            // Spacer button (invisible, maintains layout symmetry)
            Box(
                modifier = Modifier
                    .width(105.dp)
                    .height(64.dp)
            )
        }
    }
}

@Composable
fun RectangularNumberButton(
    number: String,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(105.dp)
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}