package com.kinchat.app.core.notifications.builder

/**
 * Centralized constants for all Notification related configurations.
 */
object NotificationConstants {
    // Channel & Group Configurations
    const val CHANNEL_MESSAGES = "kinchat_messages_channel"
    const val CHANNEL_NAME = "Chat Messages"
    const val CHANNEL_DESC = "Notifications for incoming messages"
    const val GROUP_KEY_MESSAGES = "com.kinchat.app.MESSAGES_GROUP"
    const val SUMMARY_ID = 9999

    // UI Elements
    const val BRAND_COLOR = "#4CAF50"
    const val LABEL_ME = "Me"
    const val FALLBACK_MEDIA_CONTENT = "📎 সংযুক্তি"
    const val SUMMARY_SINGLE_MSG = "নতুন মেসেজ"
    const val SUMMARY_MULTIPLE_MSG = "টি চ্যাটে নতুন মেসেজ"
}
