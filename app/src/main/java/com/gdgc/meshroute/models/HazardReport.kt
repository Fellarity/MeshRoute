package com.gdgc.meshroute.models

import java.util.UUID

/**
 * Data model for an emergency hazard report.
 * @property id Unique identifier for the report to prevent duplicate processing.
 * @property nodeName The specific location or node name where the hazard is (e.g., "Stairwell B").
 * @property description Human-readable description of the hazard.
 * @property timestamp When the hazard was reported.
 * @property severity 1 (low) to 5 (critical).
 */
data class HazardReport(
    val id: String = UUID.randomUUID().toString(),
    val nodeName: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: Int = 3
) {
    // Helper to convert to byte array for Nearby Connections
    fun toPayload(): String = "$id|$nodeName|$description|$timestamp|$severity"

    companion object {
        // Helper to reconstruct from Nearby Connections payload
        fun fromPayload(payload: String): HazardReport? {
            val parts = payload.split("|")
            return if (parts.size >= 5) {
                HazardReport(
                    id = parts[0],
                    nodeName = parts[1],
                    description = parts[2],
                    timestamp = parts[3].toLong(),
                    severity = parts[4].toInt()
                )
            } else null
        }
    }
}
