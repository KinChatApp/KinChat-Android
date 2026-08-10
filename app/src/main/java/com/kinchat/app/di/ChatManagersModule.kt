package com.kinchat.app.di

import com.kinchat.app.data.local.db.*
import com.kinchat.app.data.repository.chat.*
import com.kinchat.app.data.repository.chat.settings.*
import com.kinchat.app.data.repository.chat.sync.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatManagersModule {

    @Provides
    @Singleton
    fun provideChatMessageManager(
        supabaseClient: SupabaseClient,
        chatDao: ChatDao,
        chatMessageDao: ChatMessageDao,
        attachmentDao: AttachmentDao, // 🚀 Added AttachmentDao
        pendingOperationDao: PendingOperationDao,
        messageReactionDao: MessageReactionDao,
        chatInsightsDao: ChatInsightsDao
    ): ChatMessageManager {
        return ChatMessageManager(
            supabaseClient,
            chatDao,
            chatMessageDao,
            attachmentDao, // 🚀 Passed to Manager
            pendingOperationDao,
            messageReactionDao,
            chatInsightsDao
        )
    }

    @Provides
    @Singleton
    fun provideChatSettingsManager(
        supabaseClient: SupabaseClient,
        chatParticipantDao: ChatParticipantDao,
        pendingOperationDao: PendingOperationDao
    ): ChatSettingsManager {
        return ChatSettingsManager(
            supabaseClient,
            chatParticipantDao,
            pendingOperationDao
        )
    }

    @Provides
    @Singleton
    fun providePendingSyncCoordinator(
        impl: PendingSyncCoordinatorImpl
    ): PendingSyncCoordinator = impl

    @Provides
    @Singleton
    fun provideChatSessionProvider(
        impl: ChatSessionProviderImpl
    ): ChatSessionProvider = impl
}
