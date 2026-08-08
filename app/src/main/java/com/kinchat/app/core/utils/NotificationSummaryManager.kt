package com.kinchat.app.core.utils

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kinchat.app.R

/**
 * Handles the grouping and summary updates for stacked notifications.
 */
class NotificationSummaryManager(
    private val context: Context,
    private val notificationManager: NotificationManager
) {
    fun updateSummaryNotification() {
        try {
            val activeChatCount = getActiveMessageNotificationCount()
            val summaryText = if (activeChatCount > 1) {
                "$activeChatCount ${NotificationConstants.SUMMARY_MULTIPLE_MSG}"
            } else {
                NotificationConstants.SUMMARY_SINGLE_MSG
            }

            val summaryBuilder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setStyle(NotificationCompat.InboxStyle().setSummaryText(summaryText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(NotificationConstants.GROUP_KEY_MESSAGES)
                .setGroupSummary(true)

            notificationManager.notify(NotificationConstants.SUMMARY_ID, summaryBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "updateSummaryNotification failed", e)
        }
    }

    private fun getActiveMessageNotificationCount(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return 0
        return try {
            notificationManager.activeNotifications.count {
                it.notification.group == NotificationConstants.GROUP_KEY_MESSAGES && it.id != NotificationConstants.SUMMARY_ID
            }
        } catch (e: Exception) {
            Log.e(TAG, "getActiveMessageNotificationCount failed", e)
            0
        }
    }

    companion object {
        private const val TAG = "NotificationSummaryManager"
    }
}
