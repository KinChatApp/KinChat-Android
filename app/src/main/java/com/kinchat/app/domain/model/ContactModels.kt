package com.kinchat.app.domain.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserContact(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("contact_name") val contactName: String,
    @SerialName("contact_phone") val contactPhone: String,
    @SerialName("contact_phone_normalized") val contactPhoneNormalized: String,
    @SerialName("registered_user_id") val registeredUserId: String? = null,
    val profileName: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class RegisteredUserDto(
    @SerialName("id") val id: String,
    @SerialName("phone") val phone: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

data class ContactSyncResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UserContactInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("contact_name") val contactName: String,
    @SerialName("contact_phone") val contactPhone: String,
    @SerialName("contact_phone_normalized") val contactPhoneNormalized: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("registered_user_id") val registeredUserId: String? = null
)
