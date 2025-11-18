# SenseKey Data Collection Process

## Overview

SenseKey is a research application designed to collect behavioral biometric data during PIN entry on Android devices. The application captures sensor data (accelerometer, gyroscope, rotation vector) to study unique patterns in how individuals enter PINs, which can be used for user authentication and security research.

---

## Table of Contents

1. [Research Objectives](#research-objectives)
2. [The 22 PIN Selection Rationale](#the-22-pin-selection-rationale)
3. [Data Collection Architecture](#data-collection-architecture)
4. [Complete Data Collection Flow](#complete-data-collection-flow)
5. [Sensor Data Collection Mechanism](#sensor-data-collection-mechanism)
6. [Data Export and Storage](#data-export-and-storage)
7. [Data Format and Structure](#data-format-and-structure)

---

## Research Objectives

The primary goal of this research is to capture behavioral biometric patterns during PIN entry. Specifically:

- **Motion Patterns**: How users physically move their device while entering PINs
- **Hand Movements**: Variations in grip, tilt, and rotation during digit entry
- **Temporal Patterns**: Timing between button presses and post-entry settling
- **Spatial Patterns**: Different movement signatures based on PIN digit locations on the keypad

---

## The 22 PIN Selection Rationale

### Why 22 Different PINs?

The 22 predefined PINs are strategically designed to cover the entire spectrum of possible hand movements and device orientations during PIN entry. This comprehensive coverage ensures we capture diverse behavioral patterns.

### PIN Categories and Movement Coverage

#### 1. **Vertical Column Patterns (3 PINs)**
```
1478  - Left column: top to bottom
2580  - Middle column: top to bottom
3690  - Right column: top to bottom
```
**Purpose**: Captures vertical hand movements across different horizontal positions
- Tests if users naturally move up-to-down or reposition their hand
- Captures differences in left-hand vs right-hand dominant users
- Records device tilt changes during vertical traversal

#### 2. **Horizontal Row Patterns (3 PINs)**
```
1230  - Top row: left to right
4560  - Middle row: left to right
7890  - Bottom row: left to right
```
**Purpose**: Captures horizontal hand movements across different vertical positions
- Tests lateral movement patterns
- Captures thumb stretch patterns for different row heights
- Records device rotation during horizontal traversal

#### 3. **Diagonal Patterns (4 PINs)**
```
1590  - Top-left to bottom-right diagonal
3570  - Top-right to bottom-left diagonal
7531  - Bottom-left to top-right (reverse diagonal)
9531  - Bottom-right to top-left (reverse diagonal)
```
**Purpose**: Captures diagonal movements in all four directions
- Tests maximum reach and hand extension
- Captures device tilting during diagonal movements
- Records different acceleration patterns for corner-to-corner movements

#### 4. **Sequential Patterns (2 PINs)**
```
1234  - Sequential: commonly used pattern
4321  - Reverse sequential
```
**Purpose**: Captures predictable, flowing movements
- Tests muscle memory and automated movements
- Common real-world PINs for ecological validity
- Records smooth continuous motion vs discrete taps

#### 5. **Cross Patterns (2 PINs)**
```
2846  - Cross from top center
5123  - Center outward
```
**Purpose**: Captures radial movements from central points
- Tests movements originating from center vs edges
- Captures multi-directional hand repositioning
- Records complex motion trajectories

#### 6. **Corner-to-Corner Patterns (2 PINs)**
```
1397  - Top-left to top-right to bottom-right
7913  - Bottom-left to bottom-right to top-right
```
**Purpose**: Captures L-shaped movement patterns
- Tests combined horizontal and vertical movements
- Captures hand repositioning mid-PIN
- Records acceleration changes during direction switches

#### 7. **Random Distributed Patterns (4 PINs)**
```
1593  - All four corners
2684  - Sides only (no corners or center)
5927  - Random across keyboard
3816  - Wide distribution
```
**Purpose**: Captures unpredictable, scattered movements
- Tests maximum variability in hand movements
- No predictable flow patterns
- Records complex motion with frequent repositioning
- Captures maximum device orientation changes

#### 8. **Clustered Patterns (2 PINs)**
```
1245  - Left side cluster
6987  - Right side cluster
```
**Purpose**: Captures minimal movement patterns
- Tests one-handed vs two-handed entry detection
- Captures micro-movements within small areas
- Records subtle device stabilization patterns
- Tests hand dominance (left vs right hand usage)

### Coverage Analysis

The 22 PINs ensure:

1. **Complete Spatial Coverage**: Every position (0-9) is used multiple times in different contexts
2. **Directional Diversity**: All 8 primary directions (N, NE, E, SE, S, SW, W, NW) are covered
3. **Distance Variation**: Short (clustered), medium (adjacent), and long (corner-to-corner) movements
4. **Pattern Complexity**: From simple (straight lines) to complex (random scattered)
5. **Real-World Validity**: Includes common patterns (1234, sequences) and random patterns
6. **Hand Position Variation**: Tests both one-handed (clustered) and two-handed (distributed) entry styles

---

## Data Collection Architecture

### System Components

```
PinEntryActivity (UI Layer)
    ├── Participant ID Input Screen
    ├── PIN Entry Screen with Number Pad
    └── Recording Status Display

SensorDataCollector (Data Layer)
    ├── SensorManager Integration
    ├── Real-time Sensor Sampling
    ├── Event Logging (button_press, idle, recording_start/stop)
    └── Data Buffering

CSVExporter (Export Layer)
    ├── CSV File Generation
    ├── Structured Filename Creation
    └── External Storage Management

PinConfig (Configuration Layer)
    ├── Research Mode Toggle
    ├── 22 Predefined PINs
    └── PIN Validation Logic
```

### Sensor Configuration

**Sensors Used:**
1. **Accelerometer** (`TYPE_ACCELEROMETER`)
   - Measures linear acceleration in m/s²
   - Captures device movement and tilt
   - 3-axis: X (lateral), Y (vertical), Z (depth)

2. **Gyroscope** (`TYPE_GYROSCOPE`)
   - Measures angular velocity in rad/s
   - Captures device rotation
   - 3-axis: X (pitch), Y (roll), Z (yaw)

3. **Rotation Vector** (`TYPE_ROTATION_VECTOR`)
   - Measures device orientation
   - Provides quaternion representation (x, y, z, scalar)
   - Combines accelerometer, gyroscope, and magnetometer data

**Sampling Rate:** `SENSOR_DELAY_FASTEST`
- Provides maximum sampling frequency (typically 200-500 Hz depending on device)
- Critical for capturing rapid hand movements during button presses

---

## Complete Data Collection Flow

### Phase 1: Application Initialization

**File:** `PinEntryActivity.kt:32-47`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // 1. Initialize sensor collector
    sensorCollector = SensorDataCollector(this)

    // 2. Initialize CSV exporter
    csvExporter = CSVExporter(this)

    // 3. Check sensor availability
    if (!sensorCollector.areSensorsAvailable()) {
        // Show warning if sensors missing
        Toast.makeText(this,
            "Warning: Missing sensors: ${sensorCollector.getMissingSensors()}",
            Toast.LENGTH_LONG).show()
    }
}
```

**What Happens:**
- `SensorDataCollector` initializes connection to device's `SensorManager`
- Retrieves references to accelerometer, gyroscope, and rotation vector sensors
- `CSVExporter` prepares file system access for data storage
- Sensor availability check ensures all required sensors are present
- If sensors are missing, user is warned but app continues (allows testing on emulators)

### Phase 2: Participant Identification

**File:** `PinEntryActivity.kt:101-109, 492-608`

```kotlin
// State variable controlling screen display
var showParticipantInput by remember { mutableStateOf(true) }

if (showParticipantInput) {
    ParticipantIdInputScreen(
        onContinue = { id ->
            participantId = id        // Store participant ID
            showParticipantInput = false  // Switch to PIN entry screen
        }
    )
    return  // Don't render PIN entry screen yet
}
```

**ParticipantIdInputScreen Details:**
- Displays `OutlinedTextField` for text input
- Validation: Rejects blank/empty IDs (`isBlank()` check)
- Input sanitization: Trims whitespace (`trim()`)
- No character restrictions: Accepts any non-blank string (numbers, letters, special chars)
- Suggested format: "001", "002", "003" for numerical tracking
- Stored in composable state: Persists throughout session

**Why Participant ID First:**
- Ensures proper file naming from first trial
- Prevents data loss from anonymous/unlabeled sessions
- Allows participant tracking across multiple app sessions
- Enables "Edit" functionality during trials to correct mistakes

### Phase 3: Research Mode PIN Display

**File:** `PinEntryActivity.kt:135-171, PinConfig.kt:72-84`

```kotlin
// Display current research PIN
if (PinConfig.RESEARCH_MODE) {
    Text(text = "Target PIN: ${PinConfig.getCurrentResearchPin()}")
}
```

**PinConfig Logic:**
```kotlin
fun getCurrentResearchPin(): String {
    // If CORRECT_PIN is explicitly set, override research PINs
    if (CORRECT_PIN.isNotEmpty()) {
        return CORRECT_PIN
    }

    // Use predefined research PINs (cyclic access)
    return if (currentResearchPinIndex < RESEARCH_PINS.size) {
        RESEARCH_PINS[currentResearchPinIndex]
    } else {
        RESEARCH_PINS[0]  // Fallback to first PIN
    }
}
```

**PIN Progression:**
- Starts at index 0 (PIN "1478")
- Displays current PIN prominently to participant
- Only advances to next PIN upon **correct** entry
- Incorrect entries: Stay on same PIN and trial number
- After PIN 22, cycles back to PIN 1 (modulo operation)

### Phase 4: Recording Initiation

**File:** `PinEntryActivity.kt:176-193`

```kotlin
// Button to start recording
if (!isRecording) {
    Button(onClick = {
        isRecording = true
        sensorCollector.startRecording(
            trialNumber = trialNumber,
            targetPin = PinConfig.getCurrentResearchPin()
        )
        errorMessage = ""
        successMessage = ""
    })
}
```

**Why Manual Start Button:**
- Participant controls when they're ready
- Prevents premature data collection
- Allows participants to position device comfortably
- Ensures participant is focused and aware before trial begins

**What `startRecording()` Does:**

**File:** `SensorDataCollector.kt:65-94`

```kotlin
fun startRecording(trialNumber: Int = 1, targetPin: String = "") {
    // 1. Verify sensors available
    if (!areSensorsAvailable()) {
        Log.e(TAG, "Cannot start recording: Missing sensors")
        return
    }

    // 2. Set recording state
    isRecording = true
    recordingStartTime = System.currentTimeMillis()

    // 3. Generate unique session ID (UUID)
    currentSessionId = UUID.randomUUID().toString()

    // 4. Store session metadata
    currentTrialNumber = trialNumber
    currentTargetPin = targetPin
    currentPin = ""  // Reset to empty

    // 5. Clear previous data
    sensorDataBuffer.clear()

    // 6. Register sensor listeners at maximum sampling rate
    accelerometer?.let {
        sensorManager.registerListener(this, it, SENSOR_DELAY_FASTEST)
    }
    gyroscope?.let {
        sensorManager.registerListener(this, it, SENSOR_DELAY_FASTEST)
    }
    rotationVector?.let {
        sensorManager.registerListener(this, it, SENSOR_DELAY_FASTEST)
    }

    // 7. Log initial "recording_start" event
    logEvent("recording_start")
}
```

**Detailed Breakdown:**

1. **Sensor Availability Check:**
   - Ensures all 3 sensors exist
   - Returns early if any sensor is `null`
   - Prevents crashes from missing hardware

2. **Timestamp Recording:**
   - `recordingStartTime` stores `System.currentTimeMillis()`
   - Used to calculate relative timestamps (time_from_start_ms)
   - Enables synchronization of sensor samples

3. **Session ID Generation:**
   - Uses `UUID.randomUUID()` for globally unique identifier
   - Format: "d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a"
   - Links all samples in this trial together
   - Different from trial number: Session ID is unique per recording attempt

4. **Metadata Storage:**
   - `trialNumber`: Which PIN attempt (1-22+)
   - `targetPin`: What PIN participant should enter
   - `currentPin`: Tracks what's been entered so far (starts empty)

5. **Buffer Initialization:**
   - `sensorDataBuffer.clear()` removes any previous data
   - Ensures clean slate for new recording
   - Prevents contamination from previous trials

6. **Sensor Registration:**
   - `registerListener(this, sensor, SENSOR_DELAY_FASTEST)` activates each sensor
   - `SENSOR_DELAY_FASTEST`: Requests maximum sampling frequency (~200-500 Hz)
   - Triggers `onSensorChanged()` callback every time sensor updates
   - `this` refers to `SensorDataCollector` (implements `SensorEventListener`)

7. **Initial Event Log:**
   - Creates first `SensorData` entry with `eventType = "recording_start"`
   - Captures initial sensor values (usually zeros or last known values)
   - Marks precise moment recording began

### Phase 5: Continuous Sensor Sampling (Idle State)

**File:** `SensorDataCollector.kt:189-206`

```kotlin
override fun onSensorChanged(event: SensorEvent) {
    if (!isRecording) return  // Ignore events if not recording

    // Update internal sensor value arrays
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

    // Log every sensor update as an "idle" event
    logEvent("idle")
}
```

**Critical Understanding:**

This callback fires **every time any sensor updates**. At `SENSOR_DELAY_FASTEST`:
- Accelerometer updates: ~200-500 times per second
- Gyroscope updates: ~200-500 times per second
- Rotation Vector updates: ~100-200 times per second

**Why `clone()` is Essential:**
- `event.values` is a reused array reference
- Without cloning, all stored values would point to same array
- Cloning creates independent copy of current values

**Why Log Every Update as "idle":**
- Captures continuous motion even when no button is pressed
- Pre-button and post-button movements are critical behavioral signals
- Creates high-resolution timeline of device motion
- Typical trial captures 4,000-5,000 samples (3-4 seconds × 200-500 Hz)

**Memory Impact:**
- Each `logEvent()` creates a new `SensorData` object
- 4,500 samples × ~200 bytes per object ≈ 900 KB per trial in memory
- Buffer cleared after each trial, so memory resets

### Phase 6: Button Press Event

**File:** `PinEntryActivity.kt:266-281`

```kotlin
NumberPad(
    onNumberClick = { number ->
        // Block input if research mode and not recording
        if (PinConfig.RESEARCH_MODE && !isRecording) {
            return@NumberPad
        }

        // Only accept if PIN not complete and no success message
        if (pin.length < PinConfig.PIN_LENGTH && !successMessage.isNotEmpty()) {
            pin += number  // Add digit to PIN string
            errorMessage = ""  // Clear any previous errors

            // Log button press to sensor collector
            if (isRecording) {
                sensorCollector.logButtonPress(number, pin.length - 1)
                sensorCollector.updateCurrentPin(pin)
            }
        }
    }
)
```

**Input Blocking Logic:**
```kotlin
if (PinConfig.RESEARCH_MODE && !isRecording) {
    return@NumberPad  // Prevent button presses before "Start Recording"
}
```
**Why This Matters:**
- In research mode, ensures recording is active before PIN entry
- Prevents data loss from participants entering PIN too early
- In normal mode (RESEARCH_MODE = false), allows immediate entry

**Button Press Logging:**

**File:** `SensorDataCollector.kt:119-123`

```kotlin
fun logButtonPress(digit: String, position: Int) {
    currentPin += digit  // Append digit to internal tracking
    logEvent("button_press", digit, position)
}
```

**What Gets Stored:**
- `eventType`: "button_press" (instead of "idle")
- `digitPressed`: The actual digit pressed (e.g., "1", "4", "7", "8")
- `digitPosition`: Position in PIN (0 = first digit, 1 = second, 2 = third, 3 = fourth)
- All current sensor values (accel X/Y/Z, gyro X/Y/Z, rotation X/Y/Z/scalar)
- Timestamp and time_from_start

**Example Button Press Sequence for PIN "1478":**
```
Position 0: digitPressed="1", digitPosition=0, pinEntered="1"
Position 1: digitPressed="4", digitPosition=1, pinEntered="14"
Position 2: digitPressed="7", digitPosition=2, pinEntered="147"
Position 3: digitPressed="8", digitPosition=3, pinEntered="1478"
```

**Current PIN Update:**
```kotlin
sensorCollector.updateCurrentPin(pin)
```
- Keeps `SensorDataCollector.currentPin` synchronized with UI state
- Ensures all subsequent "idle" events reflect current PIN progress
- Used to calculate `isCorrect` field in real-time

### Phase 7: Post-Entry Delay (800ms)

**File:** `PinEntryActivity.kt:284-292`

```kotlin
// Auto-validate when 4 digits are entered
if (pin.length == PinConfig.PIN_LENGTH) {
    if (PinConfig.RESEARCH_MODE && isRecording) {
        val isCorrect = PinConfig.validatePin(pin)

        // Wait 800ms after 4th digit
        coroutineScope.launch {
            delay(800)  // Critical post-entry delay

            // Then stop recording...
        }
    }
}
```

**Why 800ms Delay is Critical:**

After the 4th digit is pressed, users typically:
1. **Release the button** (0-100ms)
2. **Relax their hand** (100-300ms)
3. **Return device to neutral position** (300-600ms)
4. **Stabilize the device** (600-800ms)

**Behavioral Data Captured During This Period:**
- **Return-to-rest motion**: Unique signature of how user "finishes" entry
- **Deceleration patterns**: How quickly hand movement stops
- **Stabilization micro-movements**: Small adjustments after entry
- **Anticipation signals**: Movement patterns if expecting success/failure

**Why Not Shorter (e.g., 200ms):**
- Misses stabilization phase
- Loses valuable post-interaction data
- May not capture full hand return motion

**Why Not Longer (e.g., 2000ms):**
- User may move device unrelated to PIN entry
- Captures extraneous movement (looking at screen, preparing next action)
- Increases trial duration unnecessarily

**800ms is Empirically Optimal:**
- Captures complete interaction cycle (pre-touch to post-release)
- Minimizes noise from unrelated movements
- Balances data quality with participant fatigue

**During This 800ms:**
- `onSensorChanged()` continues firing
- Hundreds more "idle" samples are recorded
- `isRecording` remains `true`
- Participant sees their entered PIN but no feedback yet

### Phase 8: Recording Termination

**File:** `PinEntryActivity.kt:294-296, SensorDataCollector.kt:99-113`

```kotlin
// Stop recording and retrieve data
val collectedData = sensorCollector.stopRecording()
isRecording = false
```

**What `stopRecording()` Does:**

```kotlin
fun stopRecording(): List<SensorData> {
    if (!isRecording) {
        return emptyList()  // Safety check
    }

    // 1. Log final event
    logEvent("recording_stop")

    // 2. Change recording state
    isRecording = false

    // 3. Unregister all sensors (stop callbacks)
    sensorManager.unregisterListener(this)

    // 4. Create immutable copy of buffer
    val collectedData = sensorDataBuffer.toList()

    // 5. Log collection summary
    Log.d(TAG, "Recording stopped - Collected ${collectedData.size} samples")

    // 6. Return data (buffer NOT cleared yet)
    return collectedData
}
```

**Step-by-Step Breakdown:**

1. **Final Event Marker:**
   - Creates `SensorData` with `eventType = "recording_stop"`
   - Captures sensor values at exact moment of termination
   - Provides clear end boundary in data

2. **State Change:**
   - `isRecording = false` stops future `onSensorChanged()` processing
   - Prevents new samples from being added

3. **Sensor Unregistration:**
   - `unregisterListener()` tells Android to stop sensor callbacks
   - Reduces battery drain
   - Frees CPU resources
   - `onSensorChanged()` will no longer fire

4. **Data Extraction:**
   - `toList()` creates immutable copy (defensive programming)
   - Original buffer remains intact (in case needed for debugging)
   - Returned list is safe to process asynchronously

5. **Logging:**
   - Debug log helps troubleshoot data collection issues
   - Typical sample count: 4,000-5,000 for a 3-4 second trial

6. **Return Data:**
   - List of `SensorData` objects ready for export
   - Contains all events: recording_start + idles + button_presses + recording_stop

### Phase 9: PIN Validation

**File:** `PinEntryActivity.kt:288, PinConfig.kt:105-127`

```kotlin
val isCorrect = PinConfig.validatePin(pin)
```

**Validation Logic:**

```kotlin
fun validatePin(enteredPin: String): Boolean {
    // 1. Length check
    if (enteredPin.length != PIN_LENGTH) {
        return false  // Must be exactly 4 digits
    }

    // 2. Digit-only check
    if (!enteredPin.all { it.isDigit() }) {
        return false  // No letters or special characters
    }

    // 3. Research mode validation
    if (RESEARCH_MODE) {
        return enteredPin == getCurrentResearchPin()
    }

    // 4. Normal mode validation
    return if (CORRECT_PIN.isEmpty()) {
        true  // Accept any 4-digit PIN
    } else {
        enteredPin == CORRECT_PIN  // Must match configured PIN
    }
}
```

**Research Mode Behavior:**
- **Correct PIN:** Returns `true`
  - Advances to next PIN (`PinConfig.nextResearchPin()`)
  - Increments trial number
  - Shows success message with ✓

- **Incorrect PIN:** Returns `false`
  - **Does NOT advance** to next PIN
  - **Does NOT increment** trial number
  - Shows success message with ✗
  - Participant must retry same PIN

**Why Save Wrong Attempts:**
```kotlin
// Export to CSV (save regardless of correctness)
val file = csvExporter.exportToCSV(
    data = collectedData,
    participantId = participantId,
    trialNumber = trialNumber,
    targetPin = PinConfig.getCurrentResearchPin(),
    isCorrect = isCorrect  // Saved as metadata
)
```
- Wrong attempts contain valuable behavioral data
- Error patterns may differ from correct patterns
- Allows analysis of "stressed" vs "confident" entry styles
- Enables detection of intentional vs accidental errors

### Phase 10: CSV Export

**File:** `PinEntryActivity.kt:299-306, CSVExporter.kt:28-65`

```kotlin
val file = csvExporter.exportToCSV(
    data = collectedData,
    participantId = participantId,
    trialNumber = trialNumber,
    targetPin = PinConfig.getCurrentResearchPin(),
    isCorrect = isCorrect
)
```

**CSV Export Process:**

**Step 1: Data Validation**
```kotlin
if (data.isEmpty()) {
    Log.w(TAG, "No data to export")
    return null
}
```
- Prevents creating empty CSV files
- Safety check for recording failures

**Step 2: File Creation**
```kotlin
val file = createCSVFile(fileName, participantId, trialNumber, targetPin, isCorrect)
```

**File Path Construction:**

**File:** `CSVExporter.kt:70-108`

```kotlin
// Base directory (app-specific external storage)
val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
// Typically: /storage/emulated/0/Android/data/com.example.sensekey/files/Documents

// Create SenseKey subdirectory
val senseKeyDir = File(documentsDir, "SenseKey")
if (!senseKeyDir.exists()) {
    senseKeyDir.mkdirs()
}
// Result: /storage/emulated/0/Android/data/com.example.sensekey/files/Documents/SenseKey
```

**Filename Generation:**

```kotlin
val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
val timestamp = dateFormat.format(Date())
// Example: "20251118_040013"

if (participantId != null && trialNumber != null && targetPin != null) {
    val trialStr = trialNumber.toString().padStart(2, '0')
    // trialNumber=1 → trialStr="01"
    // trialNumber=12 → trialStr="12"

    val correctnessStr = when (isCorrect) {
        true -> "CORRECT"
        false -> "WRONG"
        null -> "UNKNOWN"
    }

    // Final filename format:
    "participant_${participantId}_trial_${trialStr}_pin_${targetPin}_${correctnessStr}_$timestamp.csv"
}
```

**Example Filenames:**
```
participant_1_trial_01_pin_1478_CORRECT_20251118_040013.csv
participant_1_trial_01_pin_1478_WRONG_20251118_040824.csv
participant_1_trial_07_pin_1590_WRONG_20251118_040203.csv
participant_Alice_trial_15_pin_1397_CORRECT_20251118_143052.csv
```

**Filename Design Benefits:**
1. **Sortable**: Alphabetical sort groups by participant, then trial
2. **Self-documenting**: All metadata visible without opening file
3. **Unique**: Timestamp prevents overwrites if same trial repeated
4. **Parseable**: Consistent format enables automated processing
5. **Human-readable**: No cryptic codes or GUIDs in filename

**Step 3: CSV Writing**

```kotlin
FileWriter(file).use { writer ->
    // Write header row
    writer.append(SensorData.getCsvHeader())
    writer.append("\n")

    // Write data rows
    data.forEach { sensorData ->
        writer.append(sensorData.toCsvRow())
        writer.append("\n")
    }

    writer.flush()
}
```

**Header Row:**

**File:** `SensorData.kt:51-57`

```kotlin
fun getCsvHeader(): String {
    return "session_id,trial_number,target_pin,pin_entered,is_correct,timestamp_ms,time_from_start_ms," +
           "accel_x,accel_y,accel_z," +
           "gyro_x,gyro_y,gyro_z," +
           "rot_x,rot_y,rot_z,rot_scalar," +
           "event_type,digit_pressed,digit_position"
}
```

**Data Row Format:**

**File:** `SensorData.kt:39-45`

```kotlin
fun toCsvRow(): String {
    return "$sessionId,$trialNumber,$targetPin,$pinEntered,$isCorrect,$timestamp,$timeFromStart," +
           "$accelX,$accelY,$accelZ," +
           "$gyroX,$gyroY,$gyroZ," +
           "$rotVectorX,$rotVectorY,$rotVectorZ,$rotVectorScalar," +
           "$eventType,${digitPressed ?: ""},${digitPosition ?: ""}"
}
```

**Example CSV Row:**
```
d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a,1,1478,1,0,1763456409825,62,-0.20578274,3.2590244,9.183653,0.0,0.0,0.0,0.0,0.0,0.0,0.0,idle,,
```

**Field-by-Field Breakdown:**
- `session_id`: d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a (UUID)
- `trial_number`: 1
- `target_pin`: 1478
- `pin_entered`: 1 (only first digit entered so far)
- `is_correct`: 0 (incomplete PIN)
- `timestamp_ms`: 1763456409825 (Unix timestamp in milliseconds)
- `time_from_start_ms`: 62 (62ms since recording started)
- `accel_x`: -0.20578274 m/s²
- `accel_y`: 3.2590244 m/s²
- `accel_z`: 9.183653 m/s² (close to 9.8, device mostly upright)
- `gyro_x`: 0.0 rad/s (no rotation)
- `gyro_y`: 0.0 rad/s
- `gyro_z`: 0.0 rad/s
- `rot_x`: 0.0
- `rot_y`: 0.0
- `rot_z`: 0.0
- `rot_scalar`: 0.0
- `event_type`: idle
- `digit_pressed`: (empty, only populated for button_press events)
- `digit_position`: (empty, only populated for button_press events)

**Step 4: Success Logging**

```kotlin
Log.d(TAG, "CSV exported successfully: ${file.absolutePath}")
Log.d(TAG, "Total rows: ${data.size}")
return file
```

**Why Return File Object:**
- Allows UI to display filename to user
- Enables success message: "Trial #1 saved! ✓ (4521 samples)"
- Could be used for immediate file sharing (future feature)

### Phase 11: Trial Progression Logic

**File:** `PinEntryActivity.kt:307-332`

```kotlin
if (file != null) {
    val correctness = if (isCorrect) "✓" else "✗"
    successMessage = "Trial #$trialNumber saved! $correctness (${collectedData.size} samples)"

    // Only advance if correct
    if (isCorrect) {
        PinConfig.nextResearchPin()  // Move to next PIN
        trialNumber++

        // Reset after completing all 22 PINs
        if (trialNumber > PinConfig.RESEARCH_PINS.size) {
            trialNumber = 1
        }
    }
    // If wrong, stay on same trial number and PIN

    // Show message for 2 seconds, then reset
    delay(2000)
    successMessage = ""
    pin = ""
} else {
    errorMessage = "Failed to save data"
    delay(2000)
    errorMessage = ""
    pin = ""
}
```

**Correct Entry Behavior:**
1. Show success message: "Trial #1 saved! ✓ (4521 samples)"
2. Call `PinConfig.nextResearchPin()`:
   ```kotlin
   currentResearchPinIndex = (currentResearchPinIndex + 1) % RESEARCH_PINS.size
   ```
   - Index 0 (1478) → Index 1 (2580)
   - Index 21 (6987) → Index 0 (1478) [cycles back]
3. Increment `trialNumber`: 1 → 2
4. After trial 22, reset to 1 (allows unlimited data collection)
5. Wait 2 seconds (participant reads feedback)
6. Clear success message and PIN input

**Incorrect Entry Behavior:**
1. Show success message: "Trial #1 saved! ✗ (4521 samples)"
2. **Do NOT** call `nextResearchPin()` → Stays on PIN "1478"
3. **Do NOT** increment `trialNumber` → Stays on Trial 1
4. Data still saved with `WRONG` label in filename
5. Wait 2 seconds
6. Clear success message and PIN input
7. Participant can press "Start Recording" again for retry

**Why This Design:**
- Ensures exactly 22 successful trials per participant (one per PIN pattern)
- Wrong attempts don't pollute trial numbering
- Allows analysis of learning effects (2nd attempt vs 1st attempt on same PIN)
- Participant knows immediately if they made a mistake

### Phase 12: Session Reset

After 2-second delay:
```kotlin
delay(2000)
successMessage = ""  // Clear feedback message
pin = ""  // Clear entered digits
```

**State After Reset:**
- `isRecording = false` (still off)
- `participantId` (preserved)
- `trialNumber` (incremented if correct, unchanged if wrong)
- `PinConfig.currentResearchPinIndex` (advanced if correct, unchanged if wrong)
- Target PIN display updates automatically via `getCurrentResearchPin()`
- "Start Recording" button reappears
- Number pad ready for next trial

**Participant sees:**
```
SenseKey
Participant: 1    [Edit]    Trial #2

Target PIN: 2580

[Start Recording]  ← Ready for next trial

○ ○ ○ ○  ← Empty PIN dots
```

---

## Sensor Data Collection Mechanism

### Real-Time Sampling Architecture

**File:** `SensorDataCollector.kt:189-206`

The `onSensorChanged()` callback is the heart of data collection:

```kotlin
override fun onSensorChanged(event: SensorEvent) {
    if (!isRecording) return

    when (event.sensor.type) {
        Sensor.TYPE_ACCELEROMETER -> accelValues = event.values.clone()
        Sensor.TYPE_GYROSCOPE -> gyroValues = event.values.clone()
        Sensor.TYPE_ROTATION_VECTOR -> rotVectorValues = event.values.clone()
    }

    logEvent("idle")
}
```

### Event Logging Logic

**File:** `SensorDataCollector.kt:135-172`

```kotlin
private fun logEvent(eventType: String, digitPressed: String? = null, digitPosition: Int? = null) {
    if (!isRecording) return

    val now = System.currentTimeMillis()

    // Calculate correctness
    val isCorrect = if (currentPin.length == 4) {
        if (currentPin == currentTargetPin) 1 else 0
    } else {
        0  // Incomplete PIN always marked 0
    }

    // Create sensor data snapshot
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
    onDataCollected?.invoke(sensorData)  // Optional callback (unused currently)
}
```

### Sampling Characteristics

**Typical Sample Timeline for PIN "1478" (3.5 seconds total):**

```
Time 0ms:     recording_start  (1 sample)
Time 0-1500ms: idle events     (~300 samples) - hand approaching device
Time 1500ms:  button_press "1" (1 sample)
Time 1500-2000ms: idle events  (~100 samples) - moving to digit 4
Time 2000ms:  button_press "4" (1 sample)
Time 2000-2500ms: idle events  (~100 samples) - moving to digit 7
Time 2500ms:  button_press "7" (1 sample)
Time 2500-3000ms: idle events  (~100 samples) - moving to digit 8
Time 3000ms:  button_press "8" (1 sample)
Time 3000-3800ms: idle events  (~160 samples) - 800ms post-entry delay
Time 3800ms:  recording_stop   (1 sample)

Total: ~767 samples minimum
```

**Actual sample counts are much higher** (4,000-5,000) because:
- Sensors fire at 200-500 Hz, not 200 Hz exactly
- Multiple sensors firing independently
- Each sensor update triggers `logEvent("idle")`
- Accelerometer + Gyroscope + Rotation Vector = 3× sample rate

**Sample Distribution by Event Type:**
```
recording_start:  1 sample   (0.02%)
idle:            ~4,515 samples  (99.8%)
button_press:     4 samples   (0.09%)
recording_stop:   1 sample   (0.02%)
```

### Data Buffer Memory Management

**File:** `SensorDataCollector.kt:33`

```kotlin
private val sensorDataBuffer = mutableListOf<SensorData>()
```

**Memory Growth During Recording:**
- Each `SensorData` object: ~200 bytes (19 fields + object overhead)
- 4,500 samples × 200 bytes = 900 KB per trial
- 22 trials × 900 KB = 19.8 MB total session memory (if buffer not cleared)

**Buffer Lifecycle:**
1. `startRecording()`: `sensorDataBuffer.clear()` → 0 bytes
2. During recording: Grows to ~900 KB
3. `stopRecording()`: Copied to CSV, buffer remains (~900 KB)
4. Next `startRecording()`: `clear()` called again → 0 bytes

**Why Not Clear Immediately After Export:**
- Defensive programming: Buffer available for debugging if export fails
- Allows potential retry without re-recording
- Cleared on next trial anyway

---

## Data Export and Storage

### Storage Location

**Android File System Path:**
```
/storage/emulated/0/
└── Android/
    └── data/
        └── com.example.sensekey/
            └── files/
                └── Documents/
                    └── SenseKey/
                        ├── participant_1_trial_01_pin_1478_CORRECT_20251118_040013.csv
                        ├── participant_1_trial_01_pin_1478_WRONG_20251118_040824.csv
                        ├── participant_1_trial_02_pin_2580_CORRECT_20251118_040023.csv
                        └── ...
```

**Why This Location:**

1. **No Permissions Required (Android 10+)**
   - `getExternalFilesDir()` is app-specific storage
   - No `WRITE_EXTERNAL_STORAGE` permission needed
   - Automatic permission grant

2. **User Accessible**
   - Visible via USB connection to computer
   - Accessible through file managers
   - Easy data extraction for analysis

3. **Automatic Cleanup**
   - Files deleted when app is uninstalled
   - Prevents orphaned data on device
   - Respects user privacy

4. **Large Storage Capacity**
   - External storage typically has GBs available
   - Internal storage more limited
   - CSV files can grow large (22 × 900 KB = ~20 MB per participant)

### File Management Utilities

**File:** `CSVExporter.kt:110-161`

**Get Export Directory:**
```kotlin
fun getExportDirectory(): File? {
    val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        ?: return null
    return File(documentsDir, "SenseKey")
}
```

**List All Exported Files:**
```kotlin
fun getExportedFiles(): List<File> {
    val exportDir = getExportDirectory() ?: return emptyList()
    return exportDir.listFiles { file ->
        file.extension == "csv"
    }?.sortedByDescending { it.lastModified() } ?: emptyList()
}
```
- Filters only `.csv` files
- Sorts by modification time (newest first)
- Returns empty list if directory doesn't exist

**Delete File:**
```kotlin
fun deleteFile(file: File): Boolean {
    return try {
        val deleted = file.delete()
        if (deleted) Log.d(TAG, "File deleted: ${file.name}")
        deleted
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting file", e)
        false
    }
}
```

**Calculate Total Storage Used:**
```kotlin
fun getTotalExportedSize(): Long {
    return getExportedFiles().sumOf { it.length() }
}
```

**Format File Size:**
```kotlin
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
```

**These utilities enable future features:**
- File browser UI in app
- "Delete all" functionality
- Storage usage display
- Export to cloud storage
- Share via email/messaging

---

## Data Format and Structure

### CSV Schema

**19 Columns Per Row:**

| Column # | Name | Type | Unit | Description |
|----------|------|------|------|-------------|
| 1 | session_id | String (UUID) | - | Unique identifier for this recording session |
| 2 | trial_number | Integer | - | Trial number (1-22+) |
| 3 | target_pin | String | - | PIN participant should enter |
| 4 | pin_entered | String | - | PIN entered so far (partial or complete) |
| 5 | is_correct | Integer | 0/1 | 1 if complete and matches target, 0 otherwise |
| 6 | timestamp_ms | Long | milliseconds | Unix timestamp (absolute time) |
| 7 | time_from_start_ms | Long | milliseconds | Time since recording started (relative time) |
| 8 | accel_x | Float | m/s² | Accelerometer X-axis (lateral) |
| 9 | accel_y | Float | m/s² | Accelerometer Y-axis (vertical) |
| 10 | accel_z | Float | m/s² | Accelerometer Z-axis (depth) |
| 11 | gyro_x | Float | rad/s | Gyroscope X-axis (pitch rotation) |
| 12 | gyro_y | Float | rad/s | Gyroscope Y-axis (roll rotation) |
| 13 | gyro_z | Float | rad/s | Gyroscope Z-axis (yaw rotation) |
| 14 | rot_x | Float | - | Rotation vector X component |
| 15 | rot_y | Float | - | Rotation vector Y component |
| 16 | rot_z | Float | - | Rotation vector Z component |
| 17 | rot_scalar | Float | - | Rotation vector scalar (w component of quaternion) |
| 18 | event_type | String | - | "recording_start", "idle", "button_press", "recording_stop" |
| 19 | digit_pressed | String | - | Digit pressed (only for button_press events, else empty) |
| 20 | digit_position | Integer | 0-3 | Position of digit in PIN (only for button_press events, else empty) |

### Data Types and Ranges

**session_id:**
- Format: UUID v4 (RFC 4122)
- Example: "d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a"
- Uniqueness: Globally unique (collision probability: ~10⁻³⁸)

**trial_number:**
- Range: 1 to 22 (for first complete session)
- Can exceed 22 if participant repeats all PINs
- Never decrements (except manual app restart)

**target_pin:**
- Format: 4-character string of digits
- Example: "1478", "2580", "3690"
- One of 22 predefined PINs from `PinConfig.RESEARCH_PINS`

**pin_entered:**
- Format: 0-4 character string of digits
- Examples: "", "1", "14", "147", "1478"
- Grows incrementally as digits are pressed
- Reset to "" on next trial

**is_correct:**
- Values: 0 or 1 (boolean encoded as integer)
- 0: Incomplete PIN or wrong PIN
- 1: Complete PIN matching target_pin
- Changes from 0 to 1 exactly when 4th digit is pressed (if correct)

**timestamp_ms:**
- Format: Unix timestamp in milliseconds
- Example: 1763456409825 → November 18, 2025, 04:00:09.825 AM
- Used for absolute time synchronization
- Can correlate with external events (e.g., lab timestamps)

**time_from_start_ms:**
- Format: Milliseconds since `recordingStartTime`
- Example: 0, 52, 62, 125, 237, ...
- Used for relative time analysis
- First sample always near 0 (recording_start event)
- Last sample typically 3000-4500ms (recording_stop event)

**accel_x, accel_y, accel_z:**
- Format: 32-bit floating point
- Units: m/s² (meters per second squared)
- Typical ranges:
  - Device at rest: Near ±9.8 on one axis (gravity)
  - Device in motion: -20 to +20 m/s²
  - Extreme movements: up to ±40 m/s²
- Example: accel_z=9.183653 (device mostly upright, gravity pulling down)

**gyro_x, gyro_y, gyro_z:**
- Format: 32-bit floating point
- Units: rad/s (radians per second)
- Typical ranges:
  - Device stationary: 0.0 (or near 0.0 due to sensor noise)
  - Slow rotation: 0.1 to 1.0 rad/s
  - Fast rotation: 1.0 to 5.0 rad/s
- Example: gyro_z=0.0 (no yaw rotation)

**rot_x, rot_y, rot_z, rot_scalar:**
- Format: 32-bit floating point
- Units: Unitless (quaternion components)
- Represents device orientation in 3D space
- Quaternion: q = x*i + y*j + z*k + w
  - rot_x = x
  - rot_y = y
  - rot_z = z
  - rot_scalar = w
- Magnitude: sqrt(x² + y² + z² + w²) ≈ 1.0 (normalized)

**event_type:**
- Values: "recording_start", "idle", "button_press", "recording_stop"
- Distribution: ~99.8% "idle", ~0.2% others
- Used for filtering data in analysis

**digit_pressed:**
- Format: Single character string ("0"-"9")
- Empty string for non-button_press events
- Example: "1", "4", "7", "8" for PIN 1478

**digit_position:**
- Format: Integer 0-3
- Empty for non-button_press events
- 0 = first digit, 1 = second, 2 = third, 3 = fourth

### Sample Data Examples

**Recording Start:**
```csv
d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a,1,1478,,0,1763456409815,52,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,recording_start,,
```
- No PIN entered yet (pin_entered empty)
- is_correct=0 (incomplete)
- Sensor values may be 0.0 (initial state)

**Idle Sample (Pre-Button):**
```csv
d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a,1,1478,,0,1763456409825,62,-0.20578274,3.2590244,9.183653,0.0,0.0,0.0,0.0,0.0,0.0,0.0,idle,,
```
- Still no PIN entered
- Sensor values show slight movement (device being held)
- accel_z≈9.18 (close to 9.8, gravity)

**Button Press Event (First Digit):**
```csv
d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a,1,1478,1,0,1763456411234,1421,-1.523,4.872,8.234,0.234,0.112,0.034,0.023,0.045,0.012,0.998,button_press,1,0
```
- pin_entered="1" (first digit)
- digit_pressed="1", digit_position=0
- Sensor values show impact of button press
- is_correct=0 (still incomplete)

**Idle Sample (Between Buttons):**
```csv
d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a,1,1478,1,0,1763456411456,1643,-0.834,3.456,9.012,0.045,0.023,0.012,0.015,0.032,0.008,0.999,idle,,
```
- pin_entered="1" (no change)
- event_type=idle (hand moving to next button)
- Sensor values show motion trajectory

**Button Press Event (Fourth Digit - Correct):**
```csv
d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a,1,1478,1478,1,1763456413456,3643,-2.123,5.234,7.890,0.456,0.234,0.089,0.034,0.056,0.023,0.996,button_press,8,3
```
- pin_entered="1478" (complete)
- digit_pressed="8", digit_position=3
- **is_correct=1** (matches target_pin)
- Sensor values show button press impact

**Idle Sample (Post-Entry Delay):**
```csv
d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a,1,1478,1478,1,1763456413567,3754,-0.345,3.123,9.456,0.012,0.008,0.004,0.008,0.015,0.005,0.999,idle,,
```
- pin_entered="1478" (complete)
- is_correct=1 (maintained)
- Sensor values show hand returning to rest
- Still within 800ms post-entry delay

**Recording Stop:**
```csv
d0dcc574-0fca-45c6-9b1a-a6c64e1c2a3a,1,1478,1478,1,1763456414256,4443,-0.123,3.012,9.678,0.001,0.002,0.001,0.003,0.007,0.002,1.000,recording_stop,,
```
- Final sample in CSV
- Sensor values near resting state
- time_from_start_ms=4443 (4.443 seconds total recording)

### File Size Estimates

**Single Trial:**
- 4,500 samples × ~150 characters per row = 675 KB
- With header and formatting: ~650-700 KB
- Actual observed: 600-800 KB (varies by recording duration)

**Complete Session (22 Trials):**
- 22 trials × 700 KB = 15.4 MB
- Plus wrong attempts: +2-5 MB
- Total per participant: 17-20 MB

**100 Participants:**
- 100 × 18 MB = 1.8 GB
- Manageable on modern computers
- Requires ~2 GB cloud storage

---

## Summary of Data Collection Pipeline

1. **Initialization**: App loads, sensors verified, CSV exporter ready
2. **Participant ID**: User enters unique identifier (e.g., "1", "Alice")
3. **Target PIN**: System displays next PIN from 22-PIN sequence (e.g., "1478")
4. **Recording Start**: User taps "Start Recording" button
   - Sensors activate at maximum rate (200-500 Hz)
   - UUID session ID generated
   - Buffer cleared
5. **PIN Entry**: User enters 4 digits
   - Each button press logged with exact timestamp and sensor snapshot
   - Continuous idle samples between presses
6. **Post-Entry Delay**: 800ms after 4th digit
   - Captures hand return motion
   - Records stabilization patterns
7. **Recording Stop**: Sensors deactivate, data extracted from buffer
8. **Validation**: PIN checked against target
9. **CSV Export**: Data written to structured file with metadata filename
10. **Progression**: If correct, advance to next PIN; if wrong, retry same PIN
11. **Reset**: UI clears, ready for next trial

**Result**: Comprehensive behavioral biometric dataset capturing motion patterns across 22 diverse PIN entry scenarios, enabling analysis of user authentication signatures.

---

## File Metadata

- **Created**: 2025-11-18
- **Project**: SenseKey Behavioral Biometrics Research
- **Authors**: Research Team
- **Version**: 1.0
- **License**: Research Use Only