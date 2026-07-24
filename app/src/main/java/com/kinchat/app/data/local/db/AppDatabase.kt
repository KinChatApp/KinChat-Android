package com.kinchat.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        ChatParticipantEntity::class,
        ChatMessageEntity::class,
        AttachmentEntity::class,
        MessageReactionEntity::class,
        DraftEntity::class,
        PendingOperationEntity::class
    ],
    version = 1,
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
}
