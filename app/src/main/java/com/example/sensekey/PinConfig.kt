package com.example.sensekey

/**
 * Configuration object for PIN authentication and research mode
 *
 * NORMAL MODE:
 * - Set CORRECT_PIN to empty string ("") to allow any 4-digit PIN
 * - Set CORRECT_PIN to a specific value (e.g., "1234") to enforce that PIN
 *
 * RESEARCH MODE:
 * - Enable RESEARCH_MODE to use predefined PINs for data collection
 * - Predefined PINs are designed to test different motion patterns
 */
object PinConfig {
    // Normal mode PIN
    const val CORRECT_PIN = ""  // Empty string = accept any 4-digit PIN

    const val PIN_LENGTH = 4

    // Research mode settings
    var RESEARCH_MODE = true  // Set to true to enable research data collection

    // Predefined PINs for research (different spatial patterns on keyboard)
    // Designed to cover all angles, distances, and hand movement patterns
    val RESEARCH_PINS = listOf(
        // Horizontal patterns
        "1478",  // Left column: top to bottom
        "2580",  // Middle column: top to bottom
        "3690",  // Right column: top to bottom

        // Vertical patterns
        "1230",  // Top row: left to right
        "4560",  // Middle row: left to right
        "7890",  // Bottom row: left to right

        // Diagonal patterns
        "1590",  // Top-left to bottom-right diagonal
        "3570",  // Top-right to bottom-left diagonal
        "7531",  // Bottom-left to top-right (reverse diagonal)
        "9531",  // Bottom-right to top-left (reverse diagonal)

        // Sequential patterns
        "1234",  // Sequential: commonly used pattern
        "4321",  // Reverse sequential

        // Cross patterns
        "2846",  // Cross from top center
        "5123",  // Center outward

        // Corner to corner
        "1397",  // Top-left to top-right to bottom-right
        "7913",  // Bottom-left to bottom-right to top-right

        // Random distributed (covers all zones)
        "1593",  // All corners
        "2684",  // Sides only
        "5927",  // Random across keyboard
        "3816",  // Wide distribution

        // Same-hand patterns (for two-hand detection)
        "1245",  // Left side cluster
        "6987"   // Right side cluster
    )

    // Current PIN for research mode (index into RESEARCH_PINS)
    var currentResearchPinIndex = 0

    /**
     * Get the current research PIN
     * If CORRECT_PIN is set, use that; otherwise use predefined research PINs
     */
    fun getCurrentResearchPin(): String {
        // If CORRECT_PIN is explicitly set, use it
        if (CORRECT_PIN.isNotEmpty()) {
            return CORRECT_PIN
        }

        // Otherwise use predefined research PINs
        return if (currentResearchPinIndex < RESEARCH_PINS.size) {
            RESEARCH_PINS[currentResearchPinIndex]
        } else {
            RESEARCH_PINS[0]
        }
    }

    /**
     * Move to next research PIN
     */
    fun nextResearchPin() {
        currentResearchPinIndex = (currentResearchPinIndex + 1) % RESEARCH_PINS.size
    }

    /**
     * Reset to first research PIN
     */
    fun resetResearchPin() {
        currentResearchPinIndex = 0
    }

    /**
     * Validates the entered PIN against the configured PIN
     * @param enteredPin The PIN entered by the user
     * @return true if PIN is valid, false otherwise
     */
    fun validatePin(enteredPin: String): Boolean {
        // Check if PIN has correct length
        if (enteredPin.length != PIN_LENGTH) {
            return false
        }

        // Check if PIN contains only digits
        if (!enteredPin.all { it.isDigit() }) {
            return false
        }

        // Research mode: validate against current research PIN
        if (RESEARCH_MODE) {
            return enteredPin == getCurrentResearchPin()
        }

        // Normal mode: validate against CORRECT_PIN
        return if (CORRECT_PIN.isEmpty()) {
            true
        } else {
            enteredPin == CORRECT_PIN
        }
    }
}