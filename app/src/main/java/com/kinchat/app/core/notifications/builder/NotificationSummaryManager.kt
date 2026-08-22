package com.kinchat.app.core.notifications.builder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kinchat.app.MainActivity
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
            
            // 🚀 FIX (P8): If no child notifications left, cancel the stale summary notification
            if (activeChatCount == 0) {
                notificationManager.cancel(NotificationConstants.SUMMARY_ID)
                return
            }

            val summaryText = if (activeChatCount > 1) {
                "$activeChatCount ${NotificationConstants.SUMMARY_MULTIPLE_MSG}"
            } else {
                NotificationConstants.SUMMARY_SINGLE_MSG
            }

            // 🚀 FIX (P8): Add content intent to summary to open app when tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val summaryBuilder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setStyle(NotificationCompat.InboxStyle().setSummaryText(summaryText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(NotificationConstants.GROUP_KEY_MESSAGES)
                .setGroupSummary(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

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
