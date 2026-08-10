package com.kinchat.app.data.repository.dashboard.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal object DashboardDateUtils {
    fun parseTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()).apply { 
                timeZone = TimeZone.getTimeZone("UTC") 
            }
            format.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) { 
            System.currentTimeMillis() 
        }
    }
}
