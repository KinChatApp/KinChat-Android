package com.kinchat.app.core.notifications.actions

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import com.kinchat.app.core.notifications.actions.NotificationActionHelper
import com.kinchat.app.data.local.datastore.AuthPreferencesManager
import com.kinchat.app.domain.repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var authPreferencesManager: AuthPreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra("chat_id") ?: return
        val notificationId = intent.getIntExtra("notification_id", -1)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🚀 ডাটা-স্টোর থেকে বর্তমান ইউজারের আইডি আনা হলো
                val currentUserId = authPreferencesManager.meId.firstOrNull() ?: return@launch

                when (intent.action) {
                    "com.kinchat.app.ACTION_REPLY" -> {
                        val replyText = getMessageText(intent)?.toString()
                        if (!replyText.isNullOrEmpty()) {

                            chatRepository.sendMessage(
                                chatId = chatId,
                                senderId = currentUserId,
                                content = replyText
                            )

                            chatRepository.updateLastRead(chatId, currentUserId)

                            notificationManager.cancel(notificationId)
                        }
                    }
                    "com.kinchat.app.ACTION_MARK_READ" -> {

                        chatRepository.updateLastRead(chatId, currentUserId)

                        notificationManager.cancel(notificationId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun getMessageText(intent: Intent): CharSequence? {
        val remoteInput: Bundle? = RemoteInput.getResultsFromIntent(intent)
        // 🚀 NotificationHelper এর পরিবর্তে NotificationActionHelper ব্যবহার করা হয়েছে
        return remoteInput?.getCharSequence(NotificationActionHelper.REPLY_KEY)
    }
}
