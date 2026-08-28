package com.kinchat.app.domain.model

data class Chat(
    val id: String,
    val name: String,
    val partnerId: String? = null,
    val lastMessage: String?,
    val timestamp: Long,
    val unreadCount: Int,
    val avatarUrl: String?,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false,
    // 🚀 FIX: মেসেজ স্ট্যাটাস এবং সেভড মেসেজ ফিল্টারের জন্য নতুন প্রোপার্টি
    val isLastMessageFromMe: Boolean = false,
    val tickState: TickState? = null,
    val isSaved: Boolean = false
)

data class UserProfile(
    val id: String,
    val avatarUrl: String?
)
