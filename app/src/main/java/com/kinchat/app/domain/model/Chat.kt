package com.kinchat.app.domain.model

data class Chat(
    val id: String,
    val name: String,
    val partnerId: String? = null, // 🚀 FIX: অন্য ইউজারের আইডি চেনার জন্য যোগ করা হলো
    val lastMessage: String?,
    val timestamp: Long,
    val unreadCount: Int,
    val avatarUrl: String?,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false
)

data class UserProfile(
    val id: String,
    val avatarUrl: String?
)
