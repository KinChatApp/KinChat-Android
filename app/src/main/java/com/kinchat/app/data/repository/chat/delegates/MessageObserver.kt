package com.kinchat.app.data.repository.chat.delegates

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.data.local.db.toDomainModel
import com.kinchat.app.data.repository.chat.sync.ChatSyncManager
import com.kinchat.app.domain.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class MessageObserver @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val syncManager: ChatSyncManager
) {
    private val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        AppLogger.d("MessageObserver", "Observing local messages for chatId: $chatId")

        observerScope.launch {
            try {
                syncManager.fetchMissedMessages(chatId)
            } catch (e: Exception) {
                AppLogger.e("MessageObserver", "Failed to fetch missed messages for $chatId", e)
            }
        }

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
            AppLogger.d("MessageObserver", "🔄 ROOM FLOW EMISSION | chatId=$chatId | totalMessages=${messages.size}")
            
            // 🚀 FIX: changed `status` to `localStatus`
            messages.takeLast(5).forEach { message ->
                AppLogger.d("MessageObserver", "💬 MESSAGE STATUS | id=${message.id} | localStatus=${message.localStatus}")
            }
        }
    }
}
