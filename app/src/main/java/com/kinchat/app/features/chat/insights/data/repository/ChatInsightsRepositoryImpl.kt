package com.kinchat.app.features.chat.insights.data.repository

import com.kinchat.app.data.local.db.ChatInsightsDao
import com.kinchat.app.features.chat.insights.data.factory.ChatInsightsEntityFactory
import com.kinchat.app.features.chat.insights.data.mapper.toDomain
import com.kinchat.app.features.chat.insights.data.source.ChatInsightsRemoteDataSource
import com.kinchat.app.features.chat.insights.domain.model.ChatInsights
import com.kinchat.app.features.chat.insights.domain.repository.ChatInsightsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatInsightsRepositoryImpl @Inject constructor(
    private val remoteDataSource: ChatInsightsRemoteDataSource,
    private val chatInsightsDao: ChatInsightsDao,
    private val entityFactory: ChatInsightsEntityFactory
) : ChatInsightsRepository {

    override fun getChatInsightsFlow(friendId: String): Flow<ChatInsights?> {
        return chatInsightsDao.getInsightsFlow(friendId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun refreshChatInsights(friendId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val meId = remoteDataSource.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val rawData = remoteDataSource.fetchChatInsightsData(meId, friendId)
                ?: return@withContext Result.failure(Exception("Chat not found"))

            val entity = entityFactory.createEntity(
                meId = meId,
                friendId = friendId,
                rawData = rawData
            )
            
            chatInsightsDao.insertOrUpdate(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
