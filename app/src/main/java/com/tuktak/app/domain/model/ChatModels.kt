package com.tuktak.app.domain.model

data class ChatThread(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val lastMessage: String?,
    val timestamp: String?,
    val unreadCount: Int,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false
)
