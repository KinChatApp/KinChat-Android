package com.kinchat.app.data.repository.chat.delegates

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.toDomainModel
import com.kinchat.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class MessageObserver @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {
    fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        AppLogger.d("MessageObserver", "Observing local messages for chatId: $chatId")

        return chatMessageDao.observeMessagesWithDetails(chatId).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    entity.toDomainModel()
                } catch (e: Exception) {
                    AppLogger.e("MessageObserver", "Error mapping message ${entity.message.id}", e)
                    null
                }
            }
        }.onEach { messages ->
            // Logging limited to not clog console on large lists
            AppLogger.d("MessageObserver", "🔄 ROOM FLOW EMISSION | chatId=$chatId | totalMessages=${messages.size}")
        }
    }
}
