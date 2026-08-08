package com.kinchat.app.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Responsible strictly for managing Android Notification Channels.
 */
class NotificationChannelManager(
    private val notificationManager: NotificationManager
) {
    fun createMessageChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConstants.CHANNEL_MESSAGES,
                NotificationConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = NotificationConstants.CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
