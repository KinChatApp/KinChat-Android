package com.tuktak.app.core.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object ChatFormatters {
    fun formatChatTime(timeInMillis: Long): String {
        if (timeInMillis == 0L) return ""
        
        val messageTime = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
        
        return when {
            // আজকের মেসেজ
            DateUtils.isToday(timeInMillis) -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timeInMillis))
            }
            // গতকালের মেসেজ
            isYesterday(messageTime) -> {
                "Yesterday"
            }
            // তার আগের মেসেজ
            else -> {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timeInMillis))
            }
        }
    }

    private fun isYesterday(messageTime: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return yesterday.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
               yesterday.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR)
    }
}

object DurationFormatter {
    fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}

object FileFormatter {
    fun formatSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", sizeInBytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
}
