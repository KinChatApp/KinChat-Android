package com.tuktak.app.domain.model

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
    @SerialName("registered_user_id") val registeredUserId: String? = null
)

@Serializable
data class RegisteredUserDto(
    @SerialName("id") val id: String,
    @SerialName("phone") val phone: String
)

data class ContactSyncResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

// বর্তমানে ContactsRepositoryImpl-এ upsert manually buildJsonObject দিয়ে হচ্ছে,
// এই DTO সরাসরি ব্যবহার হচ্ছে না। তবু ভবিষ্যতে কোথাও data-class সিরিয়ালাইজেশন
// দিয়ে insert করলে যেন null key মিসিং না হয়, তাই @EncodeDefault(ALWAYS) রাখা হলো।
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
