package com.kinchat.app.domain.model

data class Chat(
    val id: String,
    val name: String,
    val lastMessage: String?,
    val timestamp: Long,
    val unreadCount: Int,
    val avatarUrl: String?
)

data class UserProfile(
    val id: String,
    val avatarUrl: String?
)
