package com.kinchat.app.core.notifications.builder

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object NotificationTimeFormatter {
    private val isoFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun parseTimestampSafe(createdAt: String?): Long {
        if (createdAt.isNullOrEmpty()) return System.currentTimeMillis()
        return try {
            isoFormatter.get()?.parse(createdAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
