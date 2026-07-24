package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chat_participants",
    primaryKeys = ["chatId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
        // 🚀 FIXED: UserEntity-এর ForeignKey রিমুভ করা হয়েছে যাতে Sync এর সময় সাইলেন্ট ক্র্যাশ না হয়
    ],
    indices = [Index("userId"), Index("chatId")]
)
data class ChatParticipantEntity(
    val chatId: String,
    val userId: String,
    val role: String?, // admin, member
    val joinedAt: Long?,
    val lastReadAt: Long?,
    val clearedAt: Long?,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val isLocked: Boolean = false
)
