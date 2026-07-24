package com.kinchat.app.data.local.db

import androidx.room.Embedded
import androidx.room.Relation

// ড্যাশবোর্ডের জন্য কম্বাইন্ড ডেটা মডেল
data class ChatPreview(
    @Embedded val chat: ChatEntity,
    
    // লাস্ট মেসেজ ফেস করার জন্য Relation
    @Relation(
        parentColumn = "lastMessageId",
        entityColumn = "id"
    )
    val lastMessage: ChatMessageEntity?,
    
    // চ্যাটের মেম্বারদের ফেস করার জন্য Relation (১-১ চ্যাটে অপরজনের নাম/ছবি পেতে)
    @Relation(
        parentColumn = "id",
        entityColumn = "chatId"
    )
    val participants: List<ChatParticipantEntity>
)
