package com.kinchat.app.navigation

import android.net.Uri

object NavRoutes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val DEVELOPER_LOGS = "developer_logs"
    const val CHAT_BASE_ROUTE = "chat"
    const val CHAT_ID_ARG = "chatId"
    const val MESSAGE_ID_ARG = "messageId"
    const val REPLY_ID_ARG = "replyId"
    const val CHAT_NAME_ARG = "chatName" // 🚀 নতুন যোগ করা হয়েছে

    // 🚀 রাউটে chatName আর্গুমেন্ট রিসিভ করার ব্যবস্থা করা হলো
    const val CHAT_ROUTE = "$CHAT_BASE_ROUTE/{$CHAT_ID_ARG}?$MESSAGE_ID_ARG={$MESSAGE_ID_ARG}&$CHAT_NAME_ARG={$CHAT_NAME_ARG}"       
    const val CHAT_MEDIA_PICKER_BASE_ROUTE = "chatMediaPicker"
    const val CHAT_MEDIA_PICKER_ROUTE = "$CHAT_MEDIA_PICKER_BASE_ROUTE/{$CHAT_ID_ARG}?$REPLY_ID_ARG={$REPLY_ID_ARG}"

    const val CHAT_INFO_BASE_ROUTE = "chatInfo"
    const val CHAT_INFO_USER_ID_ARG = "userId"
    const val CHAT_INFO_ROUTE = "$CHAT_INFO_BASE_ROUTE/{$CHAT_INFO_USER_ID_ARG}"

    const val CHAT_INSIGHTS_BASE_ROUTE = "chatInsights"
    const val CHAT_INSIGHTS_USER_ID_ARG = "userId"
    const val CHAT_INSIGHTS_ROUTE = "$CHAT_INSIGHTS_BASE_ROUTE/{$CHAT_INSIGHTS_USER_ID_ARG}"

    const val AI_ASSISTANT_CHAT_ID = "de438bb4-d954-4c31-9ad1-9dd34b85d981"

    // 🚀 রাউট বিল্ড করার সময় নাম পাঠানোর অপশন
    fun chatRoute(chatId: String, messageId: String? = null, chatName: String? = null): String {
        val queryParams = mutableListOf<String>()
        if (!messageId.isNullOrBlank()) queryParams.add("$MESSAGE_ID_ARG=$messageId")
        if (!chatName.isNullOrBlank()) {
            val encodedName = Uri.encode(chatName) // নামের স্পেস ঠিক রাখার জন্য
            queryParams.add("$CHAT_NAME_ARG=$encodedName")
        }
        return if (queryParams.isEmpty()) {
            "$CHAT_BASE_ROUTE/$chatId"
        } else {
            "$CHAT_BASE_ROUTE/$chatId?${queryParams.joinToString("&")}"
        }
    }

    fun chatMediaPickerRoute(chatId: String, replyId: String? = null): String =
        if (replyId.isNullOrBlank()) "$CHAT_MEDIA_PICKER_BASE_ROUTE/$chatId" else "$CHAT_MEDIA_PICKER_BASE_ROUTE/$chatId?$REPLY_ID_ARG=$replyId"

    fun chatInfoRoute(userId: String): String = "$CHAT_INFO_BASE_ROUTE/$userId"
    fun chatInsightsRoute(userId: String): String = "$CHAT_INSIGHTS_BASE_ROUTE/$userId"
}
