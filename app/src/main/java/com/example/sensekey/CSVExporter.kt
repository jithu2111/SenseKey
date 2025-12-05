package com.example.sensekey

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports sensor data to CSV files
 */
class CSVExporter(private val context: Context) {

    /**
     * Export sensor data to CSV file
     * @param data List of sensor data to export
     * @param fileName Optional custom filename (default: auto-generated with timestamp)
     * @param participantId Optional participant ID for filename
     * @param handMode Optional hand mode for filename
     * @param trialNumber Optional trial number for filename
     * @param targetPin Optional target PIN for filename
     * @param isCorrect Optional flag to indicate if PIN was correct
     * @return File object if successful, null otherwise
     */
    fun exportToCSV(
        data: List<SensorData>,
        fileName: String? = null,
        participantId: String? = null,
        handMode: String? = null,
        trialNumber: Int? = null,
        targetPin: String? = null,
        isCorrect: Boolean? = null
    ): File? {
        if (data.isEmpty()) {
            Log.w(TAG, "No data to export")
            return null
        }

        try {
            val file = createCSVFile(fileName, participantId, handMode, trialNumber, targetPin, isCorrect)
            FileWriter(file).use { writer ->
                // Write header
                writer.append(SensorData.getCsvHeader())
                writer.append("\n")

                // Write data rows
                data.forEach { sensorData ->
                    writer.append(sensorData.toCsvRow())
                    writer.append("\n")
                }

                writer.flush()
            }

            Log.d(TAG, "CSV exported successfully: ${file.absolutePath}")
            Log.d(TAG, "Total rows: ${data.size}")
            return file

        } catch (e: IOException) {
            Log.e(TAG, "Error exporting CSV", e)
            return null
        }
    }

    /**
     * Create CSV file in public Documents folder
     */
    private fun createCSVFile(
        customFileName: String?,
        participantId: String?,
        handMode: String?,
        trialNumber: Int?,
        targetPin: String?,
        isCorrect: Boolean?
    ): File {
        // Use public Documents directory (persists after app uninstall)
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            ?: throw IOException("External storage not available")

        // Create SenseKey directory if it doesn't exist
        val senseKeyDir = File(documentsDir, "SenseKey")
        if (!senseKeyDir.exists()) {
            senseKeyDir.mkdirs()
        }

        // Generate filename
        val fileName = customFileName ?: run {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timestamp = dateFormat.format(Date())

            // If research mode parameters provided, use structured naming
            if (participantId != null && trialNumber != null && targetPin != null) {
                val trialStr = trialNumber.toString().padStart(2, '0')
                val correctnessStr = when (isCorrect) {
                    true -> "CORRECT"
                    false -> "WRONG"
                    null -> "UNKNOWN"
                }

                // Sanitize hand mode string (remove spaces)
                val handModeStr = handMode?.replace(" ", "") ?: "UnknownHand"

                "participant_${participantId}_${handModeStr}_trial_${trialStr}_pin_${targetPin}_${correctnessStr}_$timestamp.csv"
            } else {
                // Fallback to timestamp-only naming
                "sensekey_data_$timestamp.csv"
            }
        }

        return File(senseKeyDir, fileName)
    }

    /**
     * Get the directory where CSV files are stored
     */
    fun getExportDirectory(): File? {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            ?: return null
        return File(documentsDir, "SenseKey")
    }

    /**
     * Get list of all exported CSV files
     */
    fun getExportedFiles(): List<File> {
        val exportDir = getExportDirectory() ?: return emptyList()
        return exportDir.listFiles { file ->
            file.extension == "csv"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Delete a CSV file
     */
    fun deleteFile(file: File): Boolean {
        return try {
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "File deleted: ${file.name}")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }

    /**
     * Get total size of all exported files in bytes
     */
    fun getTotalExportedSize(): Long {
        return getExportedFiles().sumOf { it.length() }
    }

    /**
     * Format file size to human-readable string
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    companion object {
        private const val TAG = "CSVExporter"
    }
}