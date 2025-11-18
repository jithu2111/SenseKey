# SenseKey Research Guide: PIN Detection Using Sensor Data

## Project Overview

**Goal:** Detect 4-digit PINs entered by users using two hands on Samsung S24 device through sensor data analysis.

**Sensors Used:**
- Accelerometer (m/s²)
- Gyroscope (rad/s)
- Rotation Vector (unitless)

**Output:** CSV files containing timestamped sensor data for machine learning model training.

---

## 1. Data Collection Strategy

### Why Use Predefined PINs?

**Recommended Approach: YES, use predefined PINs!**

**Benefits:**
- ✅ **Controlled variable** - Eliminates PIN complexity as a confounding factor
- ✅ **Repeatable** - Same PIN = comparable motion patterns across trials
- ✅ **Balanced dataset** - Equal samples per digit position
- ✅ **Ground truth** - Known expected input for validation
- ✅ **Easier ML training** - Consistent labels and patterns

### Recommended PIN Set

Use 3-5 predefined PINs with different spatial patterns:

| PIN  | Pattern Type | Description |
|------|--------------|-------------|
| 1234 | Sequential | Left to right, top to bottom |
| 5927 | Random | Distributed across keyboard |
| 2580 | Vertical | Middle column, top to bottom |
| 1590 | Diagonal | Top-left to bottom-right |
| 7531 | Reverse Diagonal | Bottom-left to top-right |

**Why multiple PINs?**
- Tests different hand movement patterns
- Prevents overfitting to single keyboard layout
- Creates more generalizable ML model
- Captures variety in motion dynamics

---

## 2. Data Collection Timeline

### Complete Collection Flow

```
User clicks "Start Recording"
    ↓
Sensors activate (Accelerometer, Gyroscope, Rotation Vector @ ~200Hz)
    ↓
[PRE-INTERACTION PERIOD]
User prepares hands, approaches phone
Duration: Variable (500-2000ms typical)
Purpose: Baseline sensor readings
    ↓
User presses 1st digit (e.g., "5")
    → Event logged: button_press, digit=5, position=0
    → Timestamp recorded
    ↓
[INTER-DIGIT TRANSITION]
User moves hand to next digit
Duration: 300-800ms typical
Captures: Hand movement patterns
    ↓
User presses 2nd digit (e.g., "9")
    → Event logged: button_press, digit=9, position=1
    ↓
User presses 3rd digit (e.g., "2")
    → Event logged: button_press, digit=2, position=2
    ↓
User presses 4th digit (e.g., "7")
    → Event logged: button_press, digit=7, position=3
    ↓
[POST-INTERACTION DELAY] ⭐ CRITICAL!
Automatic delay: 800ms
Purpose: Capture hand release and return motion
    ↓
Auto-stop recording
    ↓
Export to CSV file
    ↓
Display: "Trial #X saved! (XXX samples)"
    ↓
Reset PIN field
Increment trial counter
Ready for next trial
```

### Timing Parameters

| Phase | Duration | Purpose |
|-------|----------|---------|
| Pre-interaction | Variable (user controlled) | Baseline, hand approach |
| Inter-digit transitions | 300-800ms typical | Hand movement patterns |
| **Post-4th-digit delay** | **800ms (automatic)** | Hand release, return motion |

**Why 800ms post-delay?**
- 0-200ms: Finger release from screen
- 200-500ms: Hand return to rest position
- 500-800ms: Buffer for slower movements
- Research standard for touch interaction studies

---

## 3. How Much Data Do You Need?

### Quick Answer
**Minimum: 500-2000 total PIN entries**

### Detailed Scenarios

#### Scenario A: Single-User Authentication
**Goal:** Verify YOU are YOU (biometric verification)

```
Participants: 1 user (yourself)
PINs: 1 predefined PIN (e.g., 1234)
Trials per PIN: 100-200 trials
Total samples: 100-200 PIN entries

ML Data Split:
├── Training: 70% (70-140 samples)
├── Validation: 15% (15-30 samples)
└── Testing: 15% (15-30 samples)

Expected Accuracy: 85-95%
Model Type: SVM, Random Forest, Gradient Boosting
```

#### Scenario B: Multi-User Identification
**Goal:** Identify WHO entered the PIN (user classification)

```
Participants: 10-30 users
PINs: 1 predefined PIN per user
Trials per user: 50-100 trials
Total samples: 500-3000 PIN entries

ML Data Split (per user):
├── Training: 70%
├── Validation: 15%
└── Testing: 15%

Expected Accuracy: 70-90%
Model Type: CNN, LSTM, Deep Learning
```

#### Scenario C: PIN Prediction
**Goal:** Predict WHICH PIN was entered (PIN classification)

```
Participants: 5-10 users
PINs: Multiple (e.g., 1234, 5927, 2580, 1590)
Trials per PIN per user: 30-50
Total samples: 600-2000 PIN entries

Example: 10 users × 4 PINs × 40 trials = 1,600 samples

Expected Accuracy:
├── Easy case (very different PINs): 70-80%
└── Hard case (similar motion patterns): 40-60%

Model Type: BiLSTM, CNN-LSTM, Transformer
```

---

## 4. Research Protocol

### Session Structure

```
PARTICIPANT #1:
│
├── PIN: 1234
│   ├── Trial 1  (session_id: abc-123, trial: 1)
│   ├── Trial 2  (session_id: abc-124, trial: 2)
│   ├── Trial 3  (session_id: abc-125, trial: 3)
│   ├── ...
│   └── Trial 30 (session_id: abc-152, trial: 30)
│
├── PIN: 5927
│   ├── Trial 1  (session_id: def-456, trial: 1)
│   ├── ...
│   └── Trial 30 (session_id: def-485, trial: 30)
│
└── PIN: 2580
    ├── Trial 1
    ├── ...
    └── Trial 30
```

### Recommended Trials Per Participant

| Goal | Trials per PIN | Total Data |
|------|----------------|------------|
| Minimum (proof of concept) | 20-30 | Small dataset |
| Optimal (good ML model) | 50-100 | Medium dataset |
| Research gold standard | 100+ | Large dataset |

**Why so many trials?**
- Captures natural variation in hand movements
- Accounts for user fatigue effects
- Captures learning effects (user gets faster/more accurate)
- Provides sufficient data for train/validation/test splits
- Statistical significance

### Data Collection Best Practices

#### Phase 1: Training (Don't Save)
```
Duration: 5-10 practice trials
Purpose: User familiarization
Action: DO NOT save this data
Goal: Ensure natural, comfortable behavior
```

#### Phase 2: Active Collection (Save Everything)
```
Duration: 50-100 trials
Breaks: Every 20 trials (5 minutes rest)
Action: Save ALL attempts (including errors)
Time of day: Randomize (morning, afternoon, evening)
```

#### Phase 3: Data Diversity
Collect across different conditions:
- Hand position (tight grip vs. loose grip)
- Posture (sitting, standing, walking)
- Different days (capture day-to-day variation)
- Different times (morning fatigue vs. evening)

---

## 5. ML Model Expectations

### Accuracy by Dataset Size

| Total Samples | Expected Accuracy | Recommended Model | Training Time |
|---------------|-------------------|-------------------|---------------|
| 100-300 | 60-75% | SVM, Random Forest | Minutes |
| 300-1000 | 70-85% | Gradient Boosting, Basic CNN | 10-30 min |
| 1000-5000 | 80-95% | LSTM, CNN-LSTM | 1-3 hours |
| 5000+ | 85-98% | BiLSTM, Transformers | 3-12 hours |

### Data Volume per Trial

**Typical sampling:**
- Sensor sampling rate: ~200Hz (200 readings/second)
- Average PIN entry time: 3-5 seconds
- Post-delay: 0.8 seconds
- **Total duration per trial: ~4-6 seconds**

**Data points per trial:**
- 200 Hz × 5 seconds = 1,000 sensor readings
- 3 sensors × (3 accel + 3 gyro + 4 rot) = 10 values per reading
- **Total: ~10,000 data points per PIN entry**

**Storage:**
- CSV row size: ~150 bytes
- 1,000 rows per trial = ~150 KB per trial
- 100 trials = ~15 MB
- 1,000 trials = ~150 MB

---

## 6. Recommended Research Timeline

### Phase 1: Proof of Concept (Week 1)

```
PIN: 1234
Participants: Just you
Trials: 50
Duration: 2-3 days

Goals:
✓ Test if sensors work correctly
✓ Verify data quality (no missing values)
✓ Check CSV export functionality
✓ Inspect sensor data patterns visually
✓ Confirm 800ms delay captures full motion

Success criteria:
- All 50 trials saved successfully
- CSV files readable and complete
- Sensor data shows variation on button presses
```

### Phase 2: Real Collection (Week 2-3)

```
PINs: 1234, 5927, 2580
Participants: Yourself + 4 friends = 5 people
Trials: 30 per PIN per person
Total: 5 × 3 × 30 = 450 samples
Duration: 1-2 weeks

Goals:
✓ Collect research-grade dataset
✓ Ensure diversity across users
✓ Maintain consistent protocol
✓ Quality control (check data after each session)

Success criteria:
- 450+ total trials
- <5% data loss/corruption
- Balanced distribution across users and PINs
```

### Phase 3: Expansion (Optional, Week 4+)

```
Action: Add more participants or trials if accuracy is low
Target: 1,000+ samples
Additional PINs: 1590, 7531

Goals:
✓ Improve ML model accuracy
✓ Test generalization
✓ Publish research results

Success criteria:
- ML accuracy >80%
- Model generalizes to new users
```

---

## 7. CSV Data Format

### Output File Structure

**Filename format:** `sensekey_data_YYYYMMDD_HHMMSS.csv`

**Location:** `/storage/emulated/0/Android/data/com.example.sensekey/files/Documents/SenseKey/`

### CSV Columns

```csv
session_id,trial_number,pin_entered,timestamp_ms,time_from_start_ms,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,rot_x,rot_y,rot_z,rot_scalar,event_type,digit_pressed,digit_position
```

### Column Descriptions

| Column | Type | Description | Example |
|--------|------|-------------|---------|
| `session_id` | UUID | Unique ID for this PIN entry | `abc-123-def-456` |
| `trial_number` | Integer | Trial count in session | `1`, `2`, `3`... |
| `pin_entered` | String | The PIN being entered | `1234`, `5927` |
| `timestamp_ms` | Long | Absolute system time (ms) | `1701234567890` |
| `time_from_start_ms` | Long | Time since recording started | `0`, `10`, `20`... |
| `accel_x` | Float | Accelerometer X-axis (m/s²) | `0.15` |
| `accel_y` | Float | Accelerometer Y-axis (m/s²) | `9.81` |
| `accel_z` | Float | Accelerometer Z-axis (m/s²) | `0.08` |
| `gyro_x` | Float | Gyroscope X-axis (rad/s) | `0.02` |
| `gyro_y` | Float | Gyroscope Y-axis (rad/s) | `0.03` |
| `gyro_z` | Float | Gyroscope Z-axis (rad/s) | `0.00` |
| `rot_x` | Float | Rotation vector X | `0.11` |
| `rot_y` | Float | Rotation vector Y | `0.21` |
| `rot_z` | Float | Rotation vector Z | `0.31` |
| `rot_scalar` | Float | Rotation vector scalar | `0.94` |
| `event_type` | String | Event marker | `recording_start`, `idle`, `button_press`, `recording_stop` |
| `digit_pressed` | String | Digit pressed (if applicable) | `5`, `9`, `2`, `7` |
| `digit_position` | Integer | Position in PIN (0-3) | `0`, `1`, `2`, `3` |

### Sample CSV Rows

```csv
session_id,trial_number,pin_entered,timestamp_ms,time_from_start_ms,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,rot_x,rot_y,rot_z,rot_scalar,event_type,digit_pressed,digit_position
abc123,1,5927,1701234567890,0,0.12,9.81,0.05,0.01,0.02,-0.01,0.1,0.2,0.3,0.95,recording_start,,
abc123,1,5927,1701234567895,5,0.15,9.79,0.08,0.02,0.03,0.00,0.11,0.21,0.31,0.94,idle,,
abc123,1,5927,1701234568100,210,0.20,9.75,0.10,0.05,0.01,0.02,0.15,0.25,0.35,0.93,button_press,5,0
abc123,1,5927,1701234568105,215,0.25,9.70,0.15,0.08,0.02,0.03,0.16,0.26,0.36,0.92,idle,,
abc123,1,5927,1701234568800,910,0.18,9.77,0.12,0.03,0.01,0.01,0.14,0.24,0.34,0.93,button_press,9,1
abc123,1,5927,1701234569500,1610,0.16,9.80,0.09,0.02,0.02,0.00,0.13,0.23,0.33,0.94,button_press,2,2
abc123,1,5927,1701234570200,2310,0.14,9.82,0.06,0.01,0.01,-0.01,0.12,0.22,0.32,0.95,button_press,7,3
abc123,1,5927,1701234571000,3110,0.13,9.81,0.05,0.01,0.02,-0.01,0.11,0.21,0.31,0.95,recording_stop,,
```

---

## 8. Implementation Details

### Research Mode Features

**UI Components:**
1. **"Start Recording" Button** - Large, green, prominent
2. **Recording Indicator** - "🔴 Recording... Trial #X"
3. **Target PIN Display** - "Enter PIN: 1234"
4. **Sample Counter** - "(245 samples collected)"
5. **Cancel Button** - Abort current trial
6. **Trial Counter** - "Trial 5/50"

### Auto-Stop Logic

```kotlin
When 4th digit is entered:
    1. Log final button_press event
    2. Wait exactly 800ms
    3. Log recording_stop event
    4. Stop sensor listeners
    5. Export data to CSV
    6. Show success message
    7. Reset PIN field
    8. Increment trial counter
    9. Stay on PIN screen (ready for next trial)
```

### Error Handling

**What if user makes a mistake?**
- **Action:** Still save the data (error patterns are valuable!)
- **CSV marking:** PIN entry marked as entered, ML can learn errors

**What if user deletes a digit?**
- **Action:** Continue recording, log the deletion
- **CSV marking:** Shows button_press → delete → button_press sequence

**What if user takes too long (30+ seconds)?**
- **Action:** Auto-stop and discard data
- **UI:** Show "Timeout - Please restart"

---

## 9. Understanding CSV Output Structure

### What ONE CSV File Contains

**IMPORTANT:** One CSV file ≠ One row. One CSV file = ONE complete trial with 800-1200 rows!

**One Trial Breakdown:**
```
ONE CSV FILE (e.g., sensekey_data_20250117_143022.csv)
│
├─ Row 1:   [recording_start] event marker
├─ Row 2-240:   [idle] continuous sensor readings (pre-interaction ~1.2s @ 200Hz)
├─ Row 241: [button_press, digit=1, pos=0] ⭐ EVENT
├─ Row 242-360: [idle] sensor readings (inter-digit transition)
├─ Row 361: [button_press, digit=2, pos=1] ⭐ EVENT
├─ Row 362-500: [idle] sensor readings (inter-digit transition)
├─ Row 501: [button_press, digit=3, pos=2] ⭐ EVENT
├─ Row 502-680: [idle] sensor readings (inter-digit transition)
├─ Row 681: [button_press, digit=4, pos=3] ⭐ EVENT
├─ Row 682-841: [idle] sensor readings (post-delay 800ms @ 200Hz)
└─ Row 842: [recording_stop] event marker

TOTAL: ~842 rows for ONE trial
```

**Why so many rows?**
- Sensors sample at ~200 Hz = 200 readings per second
- Total recording time: ~4 seconds
- 4 seconds × 200 Hz = 800 rows of continuous sensor data
- Plus event markers = ~842 total rows per trial

**File Structure:**
```
Total duration: ~4 seconds per trial
├─ Pre-interaction: 1.2s → ~240 rows
├─ Digit transitions: 2.0s → ~400 rows
├─ Post-delay: 0.8s → ~160 rows
└─ Event markers: 6 rows (start + 4 presses + stop)

Size per file: ~126 KB
```

### Multiple Trials = Multiple Files

**Recommended: One CSV file per trial**

```
/SenseKey/
├── sensekey_data_20250117_143022.csv  (Trial 1: 842 rows - PIN 1234)
├── sensekey_data_20250117_143155.csv  (Trial 2: 820 rows - PIN 1234)
├── sensekey_data_20250117_143310.csv  (Trial 3: 850 rows - PIN 1234)
├── sensekey_data_20250117_143445.csv  (Trial 4: 815 rows - PIN 5927)
├── sensekey_data_20250117_143620.csv  (Trial 5: 835 rows - PIN 5927)
│   ...
├── sensekey_data_20250117_150255.csv  (Trial 98: 825 rows)
├── sensekey_data_20250117_150430.csv  (Trial 99: 840 rows)
└── sensekey_data_20250117_150605.csv  (Trial 100: 830 rows)
```

**Total for 100 trials:**
- 100 CSV files
- ~84,000 total rows
- ~12.6 MB total size

---

## 10. ML Training Workflow: From CSV to Model

### Complete Pipeline Overview

```
PHASE 1: Data Collection (Android App)
    ↓
100 CSV files collected
Each file = 1 trial = ~840 rows
    ↓
PHASE 2: Transfer to Computer
    ↓
Copy all CSV files to ML environment
    ↓
PHASE 3: Load & Combine (Python)
    ↓
Load all 100 files → Combine into one DataFrame
    ↓
PHASE 4: Preprocess & Extract Features
    ↓
Extract windows around button presses
OR use full sequences
    ↓
PHASE 5: Train ML Model
    ↓
Split train/test → Train model → Evaluate
```

---

### Step 1: Load Multiple CSV Files (Python)

```python
import pandas as pd
import glob
import os

# Path to folder containing all CSV files
data_folder = "/path/to/SenseKey/"

# Get all CSV files
csv_files = glob.glob(data_folder + "sensekey_data_*.csv")

print(f"Found {len(csv_files)} CSV files")
# Output: Found 100 CSV files

# Load all CSV files into a list
all_dataframes = []

for file in csv_files:
    df = pd.read_csv(file)
    all_dataframes.append(df)
    print(f"Loaded {file}: {len(df)} rows")

# Combine all dataframes into one large DataFrame
combined_df = pd.concat(all_dataframes, ignore_index=True)

print(f"\nTotal combined rows: {len(combined_df)}")
# Output: Total combined rows: 84,000

print(f"Unique sessions: {combined_df['session_id'].nunique()}")
# Output: Unique sessions: 100

print(f"Unique PINs: {combined_df['pin_entered'].unique()}")
# Output: Unique PINs: ['1234' '5927' '2580']
```

**Result:** All 100 trials combined into one DataFrame with 84,000 rows.

---

### Step 2: Exploratory Data Analysis

```python
# Check data quality
print("Event type distribution:")
print(combined_df['event_type'].value_counts())
# Output:
# idle              83,594
# button_press         400  (100 trials × 4 digits)
# recording_start      100
# recording_stop       100

# Check for missing values
print("\nMissing values:")
print(combined_df.isnull().sum())

# Verify sampling rate
sample_session = combined_df[combined_df['session_id'] == combined_df['session_id'].iloc[0]]
time_diffs = sample_session['time_from_start_ms'].diff().dropna()
avg_sampling_interval = time_diffs.mean()
sampling_rate = 1000 / avg_sampling_interval  # Convert to Hz

print(f"\nAverage sampling rate: {sampling_rate:.1f} Hz")
# Output: Average sampling rate: 200.0 Hz
```

---

### Step 3: Preprocessing - Option A (Event-Based Windows)

**Extract fixed-size windows around each button press**

```python
import numpy as np

# Configuration
window_size_ms = 300  # ±300ms around button press

# Extract all button press events
button_events = combined_df[combined_df['event_type'] == 'button_press'].copy()

print(f"Total button presses: {len(button_events)}")
# Output: Total button presses: 400

# Storage for extracted samples
samples = []
labels = []

for idx, event in button_events.iterrows():
    session_id = event['session_id']
    event_time = event['time_from_start_ms']
    digit_pressed = event['digit_pressed']

    # Get all data for this specific session
    session_data = combined_df[combined_df['session_id'] == session_id].copy()

    # Define time window
    window_start = event_time - window_size_ms
    window_end = event_time + window_size_ms

    # Extract window data
    window_data = session_data[
        (session_data['time_from_start_ms'] >= window_start) &
        (session_data['time_from_start_ms'] <= window_end)
    ].copy()

    # Extract sensor features (10 columns)
    features = window_data[[
        'accel_x', 'accel_y', 'accel_z',
        'gyro_x', 'gyro_y', 'gyro_z',
        'rot_x', 'rot_y', 'rot_z', 'rot_scalar'
    ]].values

    samples.append(features)
    labels.append(digit_pressed)

print(f"\nExtracted {len(samples)} training samples")
# Output: Extracted 400 training samples

# Convert to numpy arrays
X = np.array(samples, dtype=object)  # Shape: (400,) - each element is (time_steps, 10)
y = np.array(labels)                  # Shape: (400,)

print(f"Sample shape example: {samples[0].shape}")
# Output: Sample shape example: (120, 10)  # ~120 time steps, 10 features
```

**Result:** 400 training samples (100 trials × 4 digits), each with ~120 time steps × 10 features.

---

### Step 4: Preprocessing - Option B (Full Sequences)

**Use entire trial sequence for LSTM/RNN models**

```python
from tensorflow.keras.preprocessing.sequence import pad_sequences

# Group data by session (each session = one trial)
sessions = combined_df.groupby('session_id')

sequences = []
sequence_labels = []

for session_id, session_data in sessions:
    # Remove start/stop events, keep sensor data
    sensor_data = session_data[
        session_data['event_type'].isin(['idle', 'button_press'])
    ].copy()

    # Extract features
    features = sensor_data[[
        'accel_x', 'accel_y', 'accel_z',
        'gyro_x', 'gyro_y', 'gyro_z',
        'rot_x', 'rot_y', 'rot_z', 'rot_scalar'
    ]].values

    # Extract label (PIN entered)
    pin = session_data['pin_entered'].iloc[0]

    sequences.append(features)
    sequence_labels.append(pin)

print(f"Created {len(sequences)} sequences")
# Output: Created 100 sequences

# Pad sequences to same length (required for batch training)
X_seq = pad_sequences(sequences, padding='post', dtype='float32', value=0.0)
y_seq = np.array(sequence_labels)

print(f"X_seq shape: {X_seq.shape}")  # (100, max_length, 10)
print(f"y_seq shape: {y_seq.shape}")  # (100,)
```

**Result:** 100 sequences (one per trial), each padded to same length with 10 features.

---

### Step 5: Train ML Model - Example 1 (Random Forest)

**Using windowed data (Option A)**

```python
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix

# Flatten time series for Random Forest (doesn't handle sequences)
X_flat = np.array([sample.flatten() for sample in samples])

print(f"Flattened X shape: {X_flat.shape}")
# Output: (400, 1200)  # 120 timesteps × 10 features = 1200

# Split into train/test (80/20)
X_train, X_test, y_train, y_test = train_test_split(
    X_flat, y, test_size=0.2, random_state=42, stratify=y
)

print(f"Training samples: {len(X_train)}")
print(f"Testing samples: {len(X_test)}")
# Output: Training: 320, Testing: 80

# Train Random Forest
rf = RandomForestClassifier(
    n_estimators=100,
    max_depth=20,
    random_state=42,
    n_jobs=-1
)

rf.fit(X_train, y_train)

# Predictions
y_pred = rf.predict(X_test)

# Evaluate
accuracy = accuracy_score(y_test, y_pred)
print(f"\nTest Accuracy: {accuracy:.2%}")

print("\nClassification Report:")
print(classification_report(y_test, y_pred))

print("\nConfusion Matrix:")
print(confusion_matrix(y_test, y_pred))
```

**Expected Output:**
```
Test Accuracy: 72.5%

Classification Report:
              precision    recall  f1-score   support

           1       0.75      0.70      0.72        20
           2       0.68      0.75      0.71        20
           3       0.75      0.70      0.72        20
           4       0.72      0.75      0.73        20
```

---

### Step 6: Train ML Model - Example 2 (LSTM)

**Using full sequences (Option B)**

```python
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, BatchNormalization
from sklearn.preprocessing import LabelEncoder

# Encode PIN labels to integers
le = LabelEncoder()
y_encoded = le.fit_transform(y_seq)

print(f"Encoded labels: {le.classes_}")
# Output: ['1234' '2580' '5927']

# Split data
X_train, X_test, y_train, y_test = train_test_split(
    X_seq, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
)

print(f"X_train shape: {X_train.shape}")  # (80, max_length, 10)
print(f"X_test shape: {X_test.shape}")    # (20, max_length, 10)

# Build LSTM model
model = Sequential([
    LSTM(128, input_shape=(X_train.shape[1], X_train.shape[2]),
         return_sequences=True),
    Dropout(0.3),
    BatchNormalization(),

    LSTM(64, return_sequences=False),
    Dropout(0.3),
    BatchNormalization(),

    Dense(32, activation='relu'),
    Dropout(0.2),

    Dense(len(le.classes_), activation='softmax')
])

# Compile model
model.compile(
    optimizer='adam',
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

model.summary()

# Train model
history = model.fit(
    X_train, y_train,
    epochs=50,
    batch_size=16,
    validation_split=0.2,
    verbose=1
)

# Evaluate on test set
loss, accuracy = model.evaluate(X_test, y_test)
print(f"\nTest Accuracy: {accuracy:.2%}")

# Predictions
y_pred_proba = model.predict(X_test)
y_pred = np.argmax(y_pred_proba, axis=1)

# Confusion matrix
from sklearn.metrics import confusion_matrix, classification_report

print("\nConfusion Matrix:")
print(confusion_matrix(y_test, y_pred))

print("\nClassification Report:")
print(classification_report(y_test, y_pred, target_names=le.classes_))
```

**Expected Output:**
```
Epoch 50/50
5/5 [==============================] - 0s 45ms/step - loss: 0.2134 - accuracy: 0.9125 - val_loss: 0.3456 - val_accuracy: 0.8500

Test Accuracy: 85.00%

Confusion Matrix:
[[7 0 0]
 [1 6 0]
 [0 1 5]]

Classification Report:
              precision    recall  f1-score   support

        1234       0.88      1.00      0.93         7
        2580       0.86      0.86      0.86         7
        5927       1.00      0.71      0.83         6
```

---

### Step 7: Feature Engineering (Advanced)

**Calculate derived features for better accuracy**

```python
def extract_statistical_features(window_data):
    """Extract statistical features from sensor window"""

    features = {}

    for col in ['accel_x', 'accel_y', 'accel_z', 'gyro_x', 'gyro_y', 'gyro_z']:
        values = window_data[col].values

        features[f'{col}_mean'] = np.mean(values)
        features[f'{col}_std'] = np.std(values)
        features[f'{col}_min'] = np.min(values)
        features[f'{col}_max'] = np.max(values)
        features[f'{col}_range'] = np.max(values) - np.min(values)

    # Calculate magnitude
    accel_mag = np.sqrt(
        window_data['accel_x']**2 +
        window_data['accel_y']**2 +
        window_data['accel_z']**2
    )
    gyro_mag = np.sqrt(
        window_data['gyro_x']**2 +
        window_data['gyro_y']**2 +
        window_data['gyro_z']**2
    )

    features['accel_mag_mean'] = np.mean(accel_mag)
    features['gyro_mag_mean'] = np.mean(gyro_mag)

    return features

# Apply to all windows
feature_vectors = []

for sample in samples:
    window_df = pd.DataFrame(sample, columns=[
        'accel_x', 'accel_y', 'accel_z',
        'gyro_x', 'gyro_y', 'gyro_z',
        'rot_x', 'rot_y', 'rot_z', 'rot_scalar'
    ])

    features = extract_statistical_features(window_df)
    feature_vectors.append(list(features.values()))

X_features = np.array(feature_vectors)
print(f"Feature matrix shape: {X_features.shape}")
# Output: (400, 32)  # 400 samples, 32 engineered features
```

---

## 11. ML Model Training Tips

### Feature Engineering

**Raw features (from CSV):**
- Accelerometer: accel_x, accel_y, accel_z
- Gyroscope: gyro_x, gyro_y, gyro_z
- Rotation vector: rot_x, rot_y, rot_z, rot_scalar

**Derived features to calculate:**
- Magnitude: √(x² + y² + z²)
- Jerk: Rate of acceleration change
- Statistical: Mean, std dev, min, max per digit window
- Frequency domain: FFT components

### Model Selection Guide

| Goal | Recommended Model | Complexity | Expected Accuracy |
|------|-------------------|------------|-------------------|
| Digit classification | Random Forest | Low | 70-80% |
| PIN prediction (few PINs) | SVM, Gradient Boosting | Medium | 75-85% |
| PIN prediction (many PINs) | LSTM, GRU | High | 80-90% |
| User identification | CNN-LSTM | Very High | 85-95% |

### Hyperparameter Tuning

```python
from sklearn.model_selection import GridSearchCV

# Example: Tune Random Forest
param_grid = {
    'n_estimators': [50, 100, 200],
    'max_depth': [10, 20, 30, None],
    'min_samples_split': [2, 5, 10]
}

grid_search = GridSearchCV(
    RandomForestClassifier(random_state=42),
    param_grid,
    cv=5,
    scoring='accuracy',
    n_jobs=-1
)

grid_search.fit(X_train, y_train)

print(f"Best parameters: {grid_search.best_params_}")
print(f"Best cross-validation score: {grid_search.best_score_:.2%}")
```

---

## 12. Complete ML Pipeline Script

**Save this as `train_model.py`:**

```python
#!/usr/bin/env python3
"""
SenseKey ML Training Pipeline
Loads multiple CSV files and trains a PIN detection model
"""

import pandas as pd
import numpy as np
import glob
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report

# Configuration
DATA_FOLDER = "/path/to/SenseKey/"
WINDOW_SIZE_MS = 300

# Step 1: Load all CSV files
print("Loading CSV files...")
csv_files = glob.glob(DATA_FOLDER + "sensekey_data_*.csv")
all_dataframes = [pd.read_csv(f) for f in csv_files]
combined_df = pd.concat(all_dataframes, ignore_index=True)
print(f"Loaded {len(csv_files)} files with {len(combined_df)} total rows")

# Step 2: Extract button press windows
print("Extracting features...")
button_events = combined_df[combined_df['event_type'] == 'button_press']
samples, labels = [], []

for idx, event in button_events.iterrows():
    session_data = combined_df[combined_df['session_id'] == event['session_id']]
    window_data = session_data[
        (session_data['time_from_start_ms'] >= event['time_from_start_ms'] - WINDOW_SIZE_MS) &
        (session_data['time_from_start_ms'] <= event['time_from_start_ms'] + WINDOW_SIZE_MS)
    ]
    features = window_data[['accel_x', 'accel_y', 'accel_z', 'gyro_x', 'gyro_y', 'gyro_z',
                             'rot_x', 'rot_y', 'rot_z', 'rot_scalar']].values
    samples.append(features.flatten())
    labels.append(event['digit_pressed'])

X = np.array(samples)
y = np.array(labels)
print(f"Extracted {len(X)} samples")

# Step 3: Train model
print("Training model...")
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
model = RandomForestClassifier(n_estimators=100, random_state=42, n_jobs=-1)
model.fit(X_train, y_train)

# Step 4: Evaluate
y_pred = model.predict(X_test)
accuracy = accuracy_score(y_test, y_pred)
print(f"\nTest Accuracy: {accuracy:.2%}")
print("\nClassification Report:")
print(classification_report(y_test, y_pred))

print("\nDone!")
```

---

## 13. Quick Start Checklist

### Before Data Collection

- [ ] Samsung S24 device fully charged
- [ ] App installed and tested
- [ ] Storage space available (200+ MB)
- [ ] Predefined PINs configured (1234, 5927, 2580)
- [ ] Participant consent obtained (if applicable)
- [ ] Quiet environment for data collection

### During Collection Session

- [ ] Explain task to participant
- [ ] 5-10 practice trials (not saved)
- [ ] Start recording mode
- [ ] Collect 30-50 trials per PIN
- [ ] Take breaks every 20 trials
- [ ] Verify CSV files after each session

### After Collection

- [ ] Backup CSV files to computer
- [ ] Check data quality (no corruption)
- [ ] Count total trials collected
- [ ] Document any issues/notes
- [ ] Begin ML preprocessing

---

## 14. Troubleshooting

### Common Issues

**Problem:** Low accuracy (<60%)
**Solutions:**
- Collect more data (target 1000+ samples)
- Check sensor calibration
- Try different ML models
- Improve feature engineering

**Problem:** Missing sensor data
**Solutions:**
- Verify HIGH_SAMPLING_RATE_SENSORS permission
- Check sensor availability on device
- Restart app and device

**Problem:** CSV files too large
**Solutions:**
- Split into multiple sessions
- Compress files (.zip)
- Reduce sampling rate (not recommended)

---

## 15. Research Ethics & Privacy

### Important Considerations

- **Informed Consent:** Participants should understand data collection purpose
- **Data Security:** CSV files contain motion data, not personally identifiable
- **Anonymization:** Use participant IDs (P001, P002) not names
- **Data Retention:** Plan for secure storage and eventual deletion
- **Publication:** Follow research ethics guidelines for your institution

---

## 16. Expected Results

### What You Should See in Data

**Good quality data indicators:**
- Clear spikes in accelerometer when button pressed
- Gyroscope shows rotation changes
- Rotation vector stable during idle, changes during motion
- ~200 samples per second (check `time_from_start_ms` differences)

**Red flags:**
- All sensor values are zero
- Sampling rate <50 Hz
- Large gaps in timestamps
- Identical consecutive values (sensor stuck)

---

## Summary

**This research project will:**
1. Collect sensor data during PIN entry on Samsung S24
2. Use predefined PINs (1234, 5927, 2580) for controlled experiments
3. Record 30-100 trials per PIN per participant
4. Apply 800ms post-delay to capture complete motion patterns
5. Export data to CSV for ML model training
6. Load multiple CSV files in Python and train ML models
7. Achieve 70-95% accuracy depending on dataset size and model

**Key success factors:**
- Collect enough data (500-2000 samples minimum)
- Maintain consistent protocol
- Ensure data quality
- Use appropriate ML models
- Follow the complete pipeline from data collection → CSV export → Python loading → Model training

Good luck with your research! 🎓

### Before Data Collection

- [ ] Samsung S24 device fully charged
- [ ] App installed and tested
- [ ] Storage space available (200+ MB)
- [ ] Predefined PINs configured (1234, 5927, 2580)
- [ ] Participant consent obtained (if applicable)
- [ ] Quiet environment for data collection

### During Collection Session

- [ ] Explain task to participant
- [ ] 5-10 practice trials (not saved)
- [ ] Start recording mode
- [ ] Collect 30-50 trials per PIN
- [ ] Take breaks every 20 trials
- [ ] Verify CSV files after each session

### After Collection

- [ ] Backup CSV files to computer
- [ ] Check data quality (no corruption)
- [ ] Count total trials collected
- [ ] Document any issues/notes
- [ ] Begin ML preprocessing

---

## 11. Troubleshooting

### Common Issues

**Problem:** Low accuracy (<60%)
**Solutions:**
- Collect more data (target 1000+ samples)
- Check sensor calibration
- Try different ML models
- Improve feature engineering

**Problem:** Missing sensor data
**Solutions:**
- Verify HIGH_SAMPLING_RATE_SENSORS permission
- Check sensor availability on device
- Restart app and device

**Problem:** CSV files too large
**Solutions:**
- Split into multiple sessions
- Compress files (.zip)
- Reduce sampling rate (not recommended)

---

## 12. Research Ethics & Privacy

### Important Considerations

- **Informed Consent:** Participants should understand data collection purpose
- **Data Security:** CSV files contain motion data, not personally identifiable
- **Anonymization:** Use participant IDs (P001, P002) not names
- **Data Retention:** Plan for secure storage and eventual deletion
- **Publication:** Follow research ethics guidelines for your institution

---

## 13. Expected Results

### What You Should See in Data

**Good quality data indicators:**
- Clear spikes in accelerometer when button pressed
- Gyroscope shows rotation changes
- Rotation vector stable during idle, changes during motion
- ~200 samples per second (check `time_from_start_ms` differences)

**Red flags:**
- All sensor values are zero
- Sampling rate <50 Hz
- Large gaps in timestamps
- Identical consecutive values (sensor stuck)

---

## Summary

**This research project will:**
1. Collect sensor data during PIN entry on Samsung S24
2. Use predefined PINs (1234, 5927, 2580) for controlled experiments
3. Record 30-100 trials per PIN per participant
4. Apply 800ms post-delay to capture complete motion patterns
5. Export data to CSV for ML model training
6. Achieve 70-95% accuracy depending on dataset size

**Key success factors:**
- Collect enough data (500-2000 samples minimum)
- Maintain consistent protocol
- Ensure data quality
- Use appropriate ML models

Good luck with your research! 🎓
