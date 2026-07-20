package com.tuktak.app.features.chat.info.di

import com.tuktak.app.features.chat.info.data.repository.ChatInfoRepositoryImpl
import com.tuktak.app.features.chat.info.domain.repository.ChatInfoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatInfoModule {

    @Binds
    @Singleton
    abstract fun bindChatInfoRepository(
        chatInfoRepositoryImpl: ChatInfoRepositoryImpl
    ): ChatInfoRepository
}
