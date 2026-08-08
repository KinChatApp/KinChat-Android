package com.kinchat.app.features.chat.insights.di

import com.kinchat.app.features.chat.insights.data.repository.ChatInsightsRepositoryImpl
import com.kinchat.app.features.chat.insights.domain.repository.ChatInsightsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatInsightsModule {

    @Binds
    @Singleton
    abstract fun bindChatInsightsRepository(
        repositoryImpl: ChatInsightsRepositoryImpl
    ): ChatInsightsRepository
}
