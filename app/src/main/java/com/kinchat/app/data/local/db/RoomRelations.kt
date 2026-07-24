package com.kinchat.app.data.local.db

import androidx.room.Embedded
import androidx.room.Relation

// Chat Screen-এর জন্য: একটি মেসেজের সাথে তার অ্যাটাচমেন্ট এবং রিঅ্যাকশন
data class MessageWithDetails(
    @Embedded val message: ChatMessageEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val attachments: List<AttachmentEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val reactions: List<MessageReactionEntity>
)

// Dashboard-এর জন্য: চ্যাটের সাথে লাস্ট মেসেজ এবং পার্টিসিপেন্টদের লিস্ট
data class ChatWithDetails(
    @Embedded val chat: ChatEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "chatId"
    )
    val participants: List<ChatParticipantEntity>,
    
    @Relation(
        parentColumn = "lastMessageId",
        entityColumn = "id"
    )
    val lastMessage: ChatMessageEntity?
)
