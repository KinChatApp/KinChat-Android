package com.kinchat.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class, ChatEntity::class, ChatParticipantEntity::class,
        ChatMessageEntity::class, AttachmentEntity::class, MessageReactionEntity::class,
        DraftEntity::class, PendingOperationEntity::class, ChatInsightsEntity::class,
        UserSettingsEntity::class, ContactEntity::class, UserBlockEntity::class // 🚀 FIX: Added new entities for offline read paths
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun chatParticipantDao(): ChatParticipantDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun messageReactionDao(): MessageReactionDao
    abstract fun draftDao(): DraftDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun chatInsightsDao(): ChatInsightsDao
    // 🚀 FIX: Added DAOs for offline features
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun contactDao(): ContactDao
    abstract fun userBlockDao(): UserBlockDao
}
