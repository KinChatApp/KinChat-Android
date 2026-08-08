package com.kinchat.app.navigation

/**
 * Single source of truth for every navigation route, argument key, and
 * route-builder used across the app. Centralizing these avoids typo-driven
 * navigation crashes and duplicated string literals across feature modules.
 */
object NavRoutes {

    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val DEVELOPER_LOGS = "developer_logs"

    const val CHAT_BASE_ROUTE = "chat"
    const val CHAT_ID_ARG = "chatId"
    const val MESSAGE_ID_ARG = "messageId"
    const val CHAT_ROUTE = "$CHAT_BASE_ROUTE/{$CHAT_ID_ARG}?$MESSAGE_ID_ARG={$MESSAGE_ID_ARG}"

    const val CHAT_INFO_BASE_ROUTE = "chatInfo"
    const val CHAT_INFO_USER_ID_ARG = "userId"
    const val CHAT_INFO_ROUTE = "$CHAT_INFO_BASE_ROUTE/{$CHAT_INFO_USER_ID_ARG}"

    const val CHAT_INSIGHTS_BASE_ROUTE = "chatInsights"
    const val CHAT_INSIGHTS_USER_ID_ARG = "userId"
    const val CHAT_INSIGHTS_ROUTE = "$CHAT_INSIGHTS_BASE_ROUTE/{$CHAT_INSIGHTS_USER_ID_ARG}"

    /** Well-known conversation id reserved for the built-in AI assistant chat. */
    const val AI_ASSISTANT_CHAT_ID = "de438bb4-d954-4c31-9ad1-9dd34b85d981"

    /** Builds a route to open a chat, optionally deep-linking to a specific message. */
    fun chatRoute(chatId: String, messageId: String? = null): String =
        if (messageId.isNullOrBlank()) {
            "$CHAT_BASE_ROUTE/$chatId"
        } else {
            "$CHAT_BASE_ROUTE/$chatId?$MESSAGE_ID_ARG=$messageId"
        }

    fun chatInfoRoute(userId: String): String = "$CHAT_INFO_BASE_ROUTE/$userId"

    fun chatInsightsRoute(userId: String): String = "$CHAT_INSIGHTS_BASE_ROUTE/$userId"
}
