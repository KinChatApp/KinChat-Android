package com.kinchat.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // messages
            database.execSQL("ALTER TABLE messages ADD COLUMN client_msg_id TEXT")
            database.execSQL("ALTER TABLE messages ADD COLUMN localEditSeq INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_updatedAt ON messages(updatedAt)")

            // pending_operations
            database.execSQL("ALTER TABLE pending_operations ADD COLUMN attempt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE pending_operations ADD COLUMN maxAttempts INTEGER NOT NULL DEFAULT 5")
            database.execSQL("ALTER TABLE pending_operations ADD COLUMN lastError TEXT")
            database.execSQL("ALTER TABLE pending_operations ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
            database.execSQL("ALTER TABLE pending_operations ADD COLUMN sequence INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_pending_operations_status_sequence ON pending_operations(status, sequence)")

            // chat_participants
            database.execSQL("ALTER TABLE chat_participants ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")

            // chats
            database.execSQL("ALTER TABLE chats ADD COLUMN lastMessagePreview TEXT")

            // 🚀 FIX: Create tables for offline read paths
            database.execSQL("CREATE TABLE IF NOT EXISTS `user_settings` (`userId` TEXT NOT NULL, `notificationsEnabled` INTEGER NOT NULL, `readReceiptsEnabled` INTEGER NOT NULL, `theme` TEXT NOT NULL, PRIMARY KEY(`userId`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`id` TEXT NOT NULL, `contactName` TEXT NOT NULL, `contactPhone` TEXT NOT NULL, `contactPhoneNormalized` TEXT NOT NULL, `registeredUserId` TEXT, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `user_blocks` (`blockerId` TEXT NOT NULL, `blockedId` TEXT NOT NULL, PRIMARY KEY(`blockerId`, `blockedId`))")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kinchat_database"
        )
        .addMigrations(MIGRATION_4_5)
        .build()
    }

    @Provides @Singleton fun provideUserDao(appDatabase: AppDatabase): UserDao = appDatabase.userDao()
    @Provides @Singleton fun provideChatDao(appDatabase: AppDatabase): ChatDao = appDatabase.chatDao()
    @Provides @Singleton fun provideChatParticipantDao(appDatabase: AppDatabase): ChatParticipantDao = appDatabase.chatParticipantDao()
    @Provides @Singleton fun provideChatMessageDao(appDatabase: AppDatabase): ChatMessageDao = appDatabase.chatMessageDao()
    @Provides @Singleton fun provideAttachmentDao(appDatabase: AppDatabase): AttachmentDao = appDatabase.attachmentDao()
    @Provides @Singleton fun provideMessageReactionDao(appDatabase: AppDatabase): MessageReactionDao = appDatabase.messageReactionDao()
    @Provides @Singleton fun provideDraftDao(appDatabase: AppDatabase): DraftDao = appDatabase.draftDao()
    @Provides @Singleton fun providePendingOperationDao(appDatabase: AppDatabase): PendingOperationDao = appDatabase.pendingOperationDao()
    @Provides @Singleton fun provideChatInsightsDao(appDatabase: AppDatabase): ChatInsightsDao = appDatabase.chatInsightsDao()
    // 🚀 FIX: Provide DAOs for offline features
    @Provides @Singleton fun provideUserSettingsDao(appDatabase: AppDatabase): UserSettingsDao = appDatabase.userSettingsDao()
    @Provides @Singleton fun provideContactDao(appDatabase: AppDatabase): ContactDao = appDatabase.contactDao()
    @Provides @Singleton fun provideUserBlockDao(appDatabase: AppDatabase): UserBlockDao = appDatabase.userBlockDao()
}
