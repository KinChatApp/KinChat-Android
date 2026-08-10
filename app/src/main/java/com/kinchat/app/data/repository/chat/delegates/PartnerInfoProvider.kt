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
            if (!localTitle.isNullOrBlank()) {
                AppLogger.d("PartnerInfoProvider", "Fetched partner name from Local DB")
                return localTitle
            }
        } catch (e: Exception) {
            AppLogger.e("PartnerInfoProvider", "Local DB Error getting partner name", e)
        }

        return rpcService.getPartnerName(chatId, currentUserId)
    }
}
