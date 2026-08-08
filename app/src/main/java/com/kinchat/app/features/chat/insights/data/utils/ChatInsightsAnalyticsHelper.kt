package com.kinchat.app.features.chat.insights.data.utils

import com.kinchat.app.features.chat.insights.data.model.MessageDto
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object ChatInsightsAnalyticsHelper {

    fun calculateDaysConnected(firstMessageAt: String?): Int {
        if (firstMessageAt == null) return 1
        return try {
            val days = ChronoUnit.DAYS.between(
                ZonedDateTime.parse(firstMessageAt),
                ZonedDateTime.now()
            ).toInt()
            maxOf(days, 1)
        } catch (e: Exception) {
            1
        }
    }

    fun calculateMostActiveDay(recentMessages: List<MessageDto>): String {
        if (recentMessages.isEmpty()) return "N/A"
        
        val daysCount = mutableMapOf<String, Int>()
        recentMessages.forEach { msg ->
            msg.createdAt?.let { dateStr ->
                try {
                    val date = ZonedDateTime.parse(dateStr)
                    val day = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    daysCount[day] = daysCount.getOrDefault(day, 0) + 1
                } catch (e: Exception) {
                    // Ignore parsing errors for individual messages
                }
            }
        }
        return daysCount.maxByOrNull { it.value }?.key ?: "N/A"
    }

    fun calculateMostActiveHour(recentMessages: List<MessageDto>): String {
        if (recentMessages.isEmpty()) return "N/A"
        
        val hoursCount = mutableMapOf<String, Int>()
        recentMessages.forEach { msg ->
            msg.createdAt?.let { dateStr ->
                try {
                    val date = ZonedDateTime.parse(dateStr)
                    val hour = date.hour
                    val hourStr = formatHour(hour)
                    hoursCount[hourStr] = hoursCount.getOrDefault(hourStr, 0) + 1
                } catch (e: Exception) {
                    // Ignore parsing errors for individual messages
                }
            }
        }
        return hoursCount.maxByOrNull { it.value }?.key ?: "N/A"
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }
}
