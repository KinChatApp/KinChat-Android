package com.kinchat.app.core.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.kinchat.app.MainActivity
import com.kinchat.app.R
import com.kinchat.app.core.receivers.NotificationActionReceiver

class NotificationActionHelper(context: Context) {
    private val appContext = context.applicationContext

    fun getRawContentIntent(chatId: String): Intent {
        return Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
        }
    }

    fun getContentIntent(chatId: String, notificationId: Int): PendingIntent {
        val intent = getRawContentIntent(chatId)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(appContext, notificationId, intent, flag)
    }

    fun buildReplyAction(chatId: String, notificationId: Int): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(REPLY_KEY).setLabel(LABEL_REPLY).build()
        val replyIntent = Intent(appContext, NotificationActionReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPendingIntent = PendingIntent.getBroadcast(appContext, notificationId, replyIntent, flag)
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_action_reply, 
            LABEL_REPLY, 
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()
    }

    fun buildMarkReadAction(chatId: String, notificationId: Int): NotificationCompat.Action {
        val readIntent = Intent(appContext, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val readPendingIntent = PendingIntent.getBroadcast(appContext, notificationId + 1, readIntent, flag)
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_action_mark_read, 
            LABEL_MARK_READ, 
            readPendingIntent
        ).build()
    }

    companion object {
        const val REPLY_KEY = "key_text_reply"
        private const val ACTION_REPLY = "com.kinchat.app.ACTION_REPLY"
        private const val ACTION_MARK_READ = "com.kinchat.app.ACTION_MARK_READ"
        private const val EXTRA_CHAT_ID = "chat_id"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val LABEL_REPLY = "Reply..."
        private const val LABEL_MARK_READ = "Mark Read"
    }
}
