package com.kinchat.app.domain.model

data class Chat(
    val id: String,
    val name: String,
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
