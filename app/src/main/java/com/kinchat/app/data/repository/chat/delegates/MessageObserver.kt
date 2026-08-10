package com.kinchat.app.data.repository.chat.delegates

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.toDomainModel
import com.kinchat.app.data.repository.chat.sync.ChatSyncManager
import com.kinchat.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Collections
import javax.inject.Inject

class MessageObserver @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val syncManager: ChatSyncManager
) {
    private val startedChats = Collections.synchronizedSet(mutableSetOf<String>())

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        AppLogger.d("MessageObserver", "Observing messages for chatId: $chatId")

        if (startedChats.add(chatId)) {
            syncManager.fetchMissedMessages(chatId)
            syncManager.startRealtimeListener(chatId)
        }

        return chatMessageDao.observeMessagesWithDetails(chatId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}
