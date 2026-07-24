package com.kinchat.app.features.chat.ui

import com.kinchat.app.features.chat.ui.models.MessageUiModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class ChatListItem {
    data class Msg(val uiModel: MessageUiModel) : ChatListItem()
    data class Header(val date: LocalDate, val label: String) : ChatListItem()
}

fun localDateOf(instantStr: String?): LocalDate = try {
    instantStr?.let { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.now()
} catch (e: Exception) {
    LocalDate.now()
}

fun dateLabelFor(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMMM d"))
        else -> date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    }
}
