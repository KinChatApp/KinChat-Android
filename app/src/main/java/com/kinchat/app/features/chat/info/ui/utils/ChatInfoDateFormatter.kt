package com.kinchat.app.features.chat.info.ui.utils

import java.text.SimpleDateFormat
import java.util.Locale

fun formatLastSeen(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return "Offline"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val formatter = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
        val date = parser.parse(dateString)
        if (date != null) formatter.format(date) else "Offline"
    } catch (e: Exception) {
        "Offline"
    }
}
