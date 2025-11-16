# PIN Screen Design Documentation

## Overview
This document explains the design decisions behind the SenseKey PIN entry screen, specifically optimized for sensor-based PIN detection research on Samsung Galaxy S24.

## Design Objectives

The primary goal is to maximize the **differentiation in sensor data** (accelerometer, gyroscope, rotation vector) between consecutive button presses during PIN entry.

## Key Design Decisions

### 1. Button Dimensions: 105dp × 64dp

**Location:** `PinEntryActivity.kt:230-231`

**Rationale:**
- **Larger target area** encourages users to tap at the center of each button
- Reduces variance in tap location, creating more **consistent and predictable motion patterns**
- On Samsung S24 (~420 DPI), this translates to approximately 31mm × 19mm physical size
- Large enough for deliberate taps without accidental presses

**Sensor Impact:**
- Consistent tap locations produce **repeatable acceleration signatures**
- Larger buttons reduce edge-tapping which creates noisy sensor data

### 2. Button Spacing: 16dp (horizontal and vertical)

**Location:** `PinEntryActivity.kt:151, 157, 183`

**Rationale:**
- **16dp ≈ 48-64 pixels** on S24 display
- Creates sufficient spatial separation between buttons without wasting screen space
- Total movement distances:
  - Horizontal (e.g., 1→2): ~121dp (105 + 16)
  - Vertical (e.g., 1→4): ~80dp (64 + 16)
  - Diagonal (e.g., 1→5): ~145dp

**Sensor Impact:**
- **Larger movement distances** = more distinct accelerometer and gyroscope patterns
- Each button-to-button movement creates a unique motion signature
- 16dp spacing provides optimal balance between:
  - Too close: overlapping sensor signals
  - Too far: uncomfortable reach, inconsistent grip

### 3. Bottom-Aligned Keypad

**Location:** `PinEntryActivity.kt:54` (`Arrangement.SpaceBetween`)

**Rationale:**
- Forces keypad to bottom of screen, title/dots to top
- Users naturally hold phone in **lower half** for bottom-positioned controls
- Encourages **consistent two-handed grip** across all participants

**Sensor Impact:**
- **Two-handed grip constraint:**
  - One hand stabilizes (holding hand) → baseline sensor readings
  - One hand/thumb taps (active hand) → creates distinct motion patterns
- Bottom positioning creates **consistent device orientation** across users
- Rotation vector sensors capture characteristic tilts when tapping different positions
- Holding hand provides **stable reference frame** for differential analysis

### 4. Removed Enter/Submit Button

**Location:** `PinEntryActivity.kt:212-216` (replaced with invisible spacer)

**Rationale:**
- Only number buttons (0-9) and delete button remain functional
- Eliminates unnecessary interaction that doesn't contribute to PIN data
- Maintains visual symmetry with invisible spacer

**Sensor Impact:**
- Reduces accidental taps that would **pollute sensor dataset**
- Auto-validation at 4 digits (line 109) removes need for explicit submit
- Cleaner data: only PIN-relevant button presses recorded

### 5. Rectangular Shape with 12dp Corner Radius

**Location:** `PinEntryActivity.kt:232`

**Rationale:**
- Rectangular (not square or circular) provides visual distinction
- 12dp corner radius softens appearance without compromising tap area
- Matches modern Android design patterns

**Sensor Impact:**
- Rectangular shape provides **directional cues** (horizontal vs vertical separation)
- Users can visually distinguish button positions more easily
- Better position awareness = more consistent tapping patterns

### 6. PIN Dots Spacing: 20dp

**Location:** `PinEntryActivity.kt:82`

**Rationale:**
- Clear visual feedback for entered digits
- 24dp dot size with 20dp spacing provides good visibility
- Positioned in upper section, away from keypad

**Sensor Impact:**
- Clear feedback reduces user hesitation between taps
- Reduces need for users to look down/adjust grip
- Maintains consistent grip throughout PIN entry sequence

## Spatial Layout Analysis

### Button Grid Coordinates (Relative to Screen)

Assuming standard portrait mode on S24 (1080×2340px):

```
Row 1:  [1]    [2]    [3]     Y: ~1560px
Row 2:  [4]    [5]    [6]     Y: ~1644px
Row 3:  [7]    [8]    [9]     Y: ~1728px
Row 4:  [⌫]    [0]    [ ]     Y: ~1812px
```

### Movement Vector Examples

Each button-to-button transition creates a unique motion signature:

- **1→2**: Pure horizontal movement (~121dp right)
- **1→4**: Pure vertical movement (~80dp down)
- **1→5**: Diagonal movement (~145dp, 33° angle)
- **2→8**: Vertical movement (~168dp down)
- **7→3**: Diagonal movement (~241dp, complex path)

### Expected Sensor Signatures

**Accelerometer (m/s²):**
- Horizontal taps (e.g., 1→2): Strong X-axis acceleration spike
- Vertical taps (e.g., 1→4): Strong Y-axis acceleration spike
- Diagonal taps: Combined X+Y acceleration pattern
- Holding hand: Subtle counter-movements to stabilize

**Gyroscope (rad/s):**
- Left-to-right taps: Positive Z-axis rotation (clockwise tilt)
- Right-to-left taps: Negative Z-axis rotation (counter-clockwise tilt)
- Top-to-bottom: Forward pitch (X-axis rotation)
- Bottom-to-top: Backward pitch

**Rotation Vector (orientation):**
- Each tap location creates characteristic device tilt
- Two-handed grip amplifies rotational differences
- Button position correlates with quaternion changes

## Two-Handed Grip Hypothesis

### Expected User Behavior

**Scenario 1: Right-handed user**
- Left hand: Grips lower-left edge (stabilizing)
- Right thumb: Taps buttons
- Expected sensor pattern: Right-ward tilts when tapping, left-ward counter-tilt from stabilizing hand

**Scenario 2: Left-handed user**
- Right hand: Grips lower-right edge (stabilizing)
- Left thumb: Taps buttons
- Expected sensor pattern: Left-ward tilts when tapping, right-ward counter-tilt

**Scenario 3: Both thumbs**
- Both hands: Grip sides symmetrically
- Both thumbs: Alternate tapping
- Expected sensor pattern: Smaller rotations, more vertical motion

### Data Collection Recommendations

To maximize sensor differentiation:

1. **Instruct participants** to hold phone with both hands at bottom
2. **Record baseline sensors** for 1-2 seconds before first tap
3. **Capture sensor data** at highest possible frequency (≥100Hz)
4. **Log tap timestamps** precisely to align with sensor data
5. **Record screen coordinates** of each tap for position normalization
6. **Include haptic feedback** to mark exact tap moment in sensor timeline

## Design Validation Metrics

To verify this design achieves sensor differentiation:

1. **Inter-button distance variation**: Measure accelerometer magnitude for each button pair
2. **Rotation signature uniqueness**: Compute gyroscope pattern correlation between different taps
3. **Temporal separation**: Ensure tap events are separated by >200ms minimum
4. **Position clustering**: Verify taps cluster at button centers, not edges

## Implementation Files

- **Main layout**: `PinEntryActivity.kt:44-128` (`PinEntryScreen` composable)
- **Button component**: `PinEntryActivity.kt:222-258` (`RectangularNumberButton`)
- **Keypad grid**: `PinEntryActivity.kt:144-219` (`NumberPad` composable)
- **Config**: `PinConfig.kt` (PIN validation logic)

## Future Optimization Considerations

1. **Haptic feedback**: Add vibration on tap to mark exact sensor event timing
2. **Button press animation**: Visual feedback to encourage consistent tap duration
3. **Adaptive spacing**: A/B test with 14dp, 16dp, 18dp spacing to find optimal separation
4. **Pressure sensitivity**: If device supports, log tap pressure for additional feature
5. **Audio feedback**: Optional beep to mark tap events in sensor timeline

## References

- Samsung Galaxy S24 specifications: 6.2" display, 1080×2340px, ~420 DPI
- Android sensor documentation: [Sensor Types](https://developer.android.com/reference/android/hardware/Sensor)
- Material Design touch targets: Minimum 48dp recommended