package com.kinchat.app.features.search.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchRpcParams(
    val p_user_id: String,
    val p_query: String
)

@Serializable
data class RpcContactSearchResultDto(
    val id: String,
    @SerialName("contact_name") val contactName: String,
    @SerialName("contact_phone") val contactPhone: String,
    @SerialName("registered_user_id") val registeredUserId: String? = null
)

@Serializable
data class RpcMessageSearchResultDto(
    val id: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("other_user_id") val otherUserId: String? = null
)

@Serializable
data class SearchRpcResponseDto(
    val contacts: List<RpcContactSearchResultDto> = emptyList(),
    val messages: List<RpcMessageSearchResultDto> = emptyList()
)
