package com.tuktak.app.features.search.domain.model

data class SearchResult(
    val contacts: List<ContactSearchResult> = emptyList(),
    val messages: List<MessageSearchResult> = emptyList()
)

data class ContactSearchResult(
    val id: String,
    val name: String,
    val phone: String,
    val registeredUserId: String?
)

data class MessageSearchResult(
    val id: String,
    val content: String,
    val createdAt: String,
    val displayName: String,
    val otherUserId: String?
)
