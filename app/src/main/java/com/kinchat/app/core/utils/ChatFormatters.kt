package com.kinchat.app.core.utils

import androidx.compose.runtime.Stable
import com.kinchat.app.domain.model.Chat
import com.kinchat.app.domain.model.UserContact
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Stable
object ContactResolver {
    fun resolveDisplayName(
        contactName: String?,
        profileName: String?,
        username: String?,
        phoneNumber: String?
    ): String {
        // ৭. Contact name আসলেই কোনো ফোন নম্বর কিনা তা যাচাই করা
        val numericContactName = contactName?.replace(Regex("\\D"), "") ?: ""
        val isContactNameActuallyPhone = numericContactName.length > 7 && phoneNumber?.contains(numericContactName) == true

        return when {
            !contactName.isNullOrBlank() && !isContactNameActuallyPhone -> contactName
            !profileName.isNullOrBlank() -> profileName
            !username.isNullOrBlank() -> "@$username"
            !phoneNumber.isNullOrBlank() -> phoneNumber
            else -> "Unknown User"
        }
    }

    fun resolveChatName(chat: Chat, contacts: List<UserContact>): String {
        val cleanPartnerId = chat.partnerId?.replace("\"", "")?.trim()
        var matchedContact = contacts.find { it.registeredUserId == cleanPartnerId }

        if (matchedContact == null) {
            matchedContact = contacts.find { it.contactName.equals(chat.name, ignoreCase = true) }
        }

        val resolvedName = matchedContact?.let {
            resolveDisplayName(
                contactName = it.contactName,
                profileName = it.profileName,
                username = it.username,
                phoneNumber = it.contactPhoneNormalized
            )
        }

        return if (resolvedName != null && resolvedName != "Unknown User") resolvedName else chat.name
    }
}

@Stable
object ChatFormatters {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val dayOfWeekFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

    fun formatChatTime(timeInMillis: Long): String {
        if (timeInMillis <= 0L) return ""

        val zoneId = ZoneId.systemDefault()
        val instant = Instant.ofEpochMilli(timeInMillis)
        val messageDate = instant.atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)

        val daysBetween = ChronoUnit.DAYS.between(messageDate, today)

        return when {
            daysBetween == 0L -> timeFormatter.format(instant.atZone(zoneId))
            daysBetween == 1L -> "Yesterday"
            daysBetween in 2L..6L -> dayOfWeekFormatter.format(instant.atZone(zoneId))
            else -> dateFormatter.format(instant.atZone(zoneId))
        }
    }
}

@Stable
object DurationFormatter {
    fun formatDuration(durationMillis: Long): String {
        if (durationMillis <= 0L) return "00:00"

        val totalSeconds = durationMillis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}

@Stable
object FileFormatter {
    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

    fun formatSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0L) return "0 B"

        var size = sizeInBytes.toDouble()
        var unitIndex = 0

        while (size >= 1024.0 && unitIndex < UNITS.size - 1) {
            size /= 1024.0
            unitIndex++
        }

        return if (unitIndex == 0) {
            String.format(Locale.getDefault(), "%.0f %s", size, UNITS[unitIndex])
        } else {
            String.format(Locale.getDefault(), "%.1f %s", size, UNITS[unitIndex])
        }
    }
}
