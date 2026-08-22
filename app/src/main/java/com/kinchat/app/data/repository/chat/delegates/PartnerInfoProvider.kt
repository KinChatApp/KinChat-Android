package com.kinchat.app.data.repository.chat.delegates

import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.local.db.ChatDao
import com.kinchat.app.data.remote.api.ChatRpcService
import javax.inject.Inject

class PartnerInfoProvider @Inject constructor(
    private val chatDao: ChatDao,
    private val rpcService: ChatRpcService
) {
    suspend fun getPartnerName(chatId: String, currentUserId: String): String? {
        try {
            val localTitle = chatDao.getChatTitle(chatId)
            // 🚀 FIX: Ignore default "New Chat" title so it can fallback to FCM payload
            if (!localTitle.isNullOrBlank() && localTitle.trim().lowercase() != "new chat") {
                AppLogger.d("PartnerInfoProvider", "Fetched partner name from Local DB: $localTitle")
                return localTitle
            }
        } catch (e: Exception) {
            AppLogger.e("PartnerInfoProvider", "Local DB Error getting partner name", e)
        }

        return try {
            val remoteName = rpcService.getPartnerName(chatId, currentUserId)
            if (!remoteName.isNullOrBlank() && remoteName.trim().lowercase() != "new chat") {
                remoteName
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
