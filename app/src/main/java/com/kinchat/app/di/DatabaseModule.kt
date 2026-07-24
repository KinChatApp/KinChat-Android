package com.kinchat.app.di

import android.content.Context
import androidx.room.Room
import com.kinchat.app.data.local.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kinchat_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao = appDatabase.userDao()

    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase): ChatDao = appDatabase.chatDao()

    @Provides
    @Singleton
    fun provideChatParticipantDao(appDatabase: AppDatabase): ChatParticipantDao = appDatabase.chatParticipantDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(appDatabase: AppDatabase): ChatMessageDao = appDatabase.chatMessageDao()

    @Provides
    @Singleton
    fun provideAttachmentDao(appDatabase: AppDatabase): AttachmentDao = appDatabase.attachmentDao()

    @Provides
    @Singleton
    fun provideMessageReactionDao(appDatabase: AppDatabase): MessageReactionDao = appDatabase.messageReactionDao()

    @Provides
    @Singleton
    fun provideDraftDao(appDatabase: AppDatabase): DraftDao = appDatabase.draftDao()

    @Provides
    @Singleton
    fun providePendingOperationDao(appDatabase: AppDatabase): PendingOperationDao = appDatabase.pendingOperationDao()
}
