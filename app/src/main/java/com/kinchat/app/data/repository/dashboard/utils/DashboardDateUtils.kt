package com.kinchat.app.data.repository.dashboard.utils

internal object DashboardDateUtils {
    fun parseTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()

        // 🚀 ECHO FIX: Instant.parse accepts any ISO-8601 variant (incl. microsecond
        // precision), so the preview row gets the REAL message timestamp instead of
        // falling back to "now", which made the synthetic row float to the bottom of
        // the chat list right next to the user's newest message.
        return try {
            java.time.Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
