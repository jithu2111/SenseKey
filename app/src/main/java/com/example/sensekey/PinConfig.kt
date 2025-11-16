package com.example.sensekey

/**
 * Configuration object for PIN authentication
 *
 * CHANGE THE PIN HERE:
 * - Set CORRECT_PIN to empty string ("") to allow any 4-digit PIN (current behavior)
 * - Set CORRECT_PIN to a specific value (e.g., "1234") to enforce that PIN
 */
object PinConfig {
    // TODO: Change this value to set a specific PIN
    // Example: const val CORRECT_PIN = "1234"
    const val CORRECT_PIN = ""  // Empty string = accept any 4-digit PIN

    const val PIN_LENGTH = 4

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

        // If CORRECT_PIN is empty, accept any valid 4-digit PIN
        // Otherwise, check if entered PIN matches the configured PIN
        return if (CORRECT_PIN.isEmpty()) {
            true
        } else {
            enteredPin == CORRECT_PIN
        }
    }
}