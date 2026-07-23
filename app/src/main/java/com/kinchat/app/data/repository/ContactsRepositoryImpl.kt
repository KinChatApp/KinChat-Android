package com.kinchat.app.data.repository

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.kinchat.app.domain.model.ContactSyncResult
import com.kinchat.app.domain.model.RegisteredUserDto
import com.kinchat.app.domain.model.UserContact
import com.kinchat.app.domain.repository.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ContactsRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context
) : ContactsRepository {

    private val _contacts = MutableStateFlow<List<UserContact>>(emptyList())

    companion object {
        private const val TAG = "ContactsRepository"
        private const val LOOKUP_CHUNK_SIZE = 200
        private const val RPC_CHUNK_SIZE = 500
    }

    override fun getContacts(): Flow<List<UserContact>> = _contacts.asStateFlow()

    override suspend fun loadContactsFromRemote() {
        withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext
                val data = supabase.postgrest["user_contacts"]
                    .select {
                        filter { eq("user_id", userId) }
                    }.decodeList<UserContact>()

                _contacts.value = data.sortedBy { it.contactName }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load contacts from remote", e)
            }
        }
    }

    override suspend fun syncDeviceContacts(): ContactSyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return@withContext ContactSyncResult(false, "User not authenticated")

            val deviceContacts = getLocalContacts()
            if (deviceContacts.isEmpty()) {
                return@withContext ContactSyncResult(true)
            }

            val uniqueContactsMap = linkedMapOf<String, LocalContact>()
            for (contact in deviceContacts) {
                val normalized = sanitizePhoneNumber(contact.phone)
                if (normalized.length >= 11 && !uniqueContactsMap.containsKey(normalized)) {
                    uniqueContactsMap[normalized] = LocalContact(
                        name = contact.name,
                        original = contact.phone,
                        normalized = normalized
                    )
                }
            }

            if (uniqueContactsMap.isEmpty()) return@withContext ContactSyncResult(true)

            val normalizedPhonesList = uniqueContactsMap.keys.toList()

            val matchedUsers = mutableListOf<RegisteredUserDto>()
            normalizedPhonesList.chunked(LOOKUP_CHUNK_SIZE).forEach { chunk ->
                try {
                    val result = supabase.postgrest["users"].select {
                        filter { isIn("phone", chunk) }
                    }.decodeList<RegisteredUserDto>()
                    matchedUsers.addAll(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to look up registered users for a chunk", e)
                }
            }
            val matchedByPhone = matchedUsers.associateBy { it.phone }

            val contactsPayload: List<JsonObject> = uniqueContactsMap.values.map { local ->
                val registeredUser = matchedByPhone[local.normalized]
                buildJsonObject {
                    put("contact_name", local.name)
                    put("contact_phone", local.original)
                    put("contact_phone_normalized", local.normalized)
                    put("registered_user_id", registeredUser?.id)
                }
            }

            var anyFailure = false
            var lastError: String? = null
            contactsPayload.chunked(RPC_CHUNK_SIZE).forEach { chunk ->
                try {
                    supabase.postgrest.rpc(
                        function = "sync_user_contacts",
                        parameters = buildJsonObject {
                            put("contacts", JsonArray(chunk))
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "sync_user_contacts RPC failed for a chunk", e)
                    anyFailure = true
                    lastError = e.message
                }
            }

            loadContactsFromRemote()

            if (anyFailure) {
                ContactSyncResult(false, lastError ?: "Some contacts failed to sync")
            } else {
                ContactSyncResult(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync device contacts", e)
            ContactSyncResult(false, e.message ?: "Failed to sync device contacts")
        }
    }

    private fun getLocalContacts(): List<LocalContact> {
        val contacts = mutableListOf<LocalContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex) ?: "Unknown"
                val number = cursor.getString(numberIndex) ?: continue
                contacts.add(LocalContact(name = name, phone = number))
            }
        }
        return contacts
    }

    private fun sanitizePhoneNumber(phone: String): String {
        var cleaned = phone.replace(Regex("[^\\d+]"), "")
        if (cleaned.startsWith("01") && cleaned.length == 11) {
            cleaned = "+88$cleaned"
        } else if (cleaned.startsWith("8801") && cleaned.length == 13) {
            cleaned = "+$cleaned"
        }
        return cleaned
    }

    private data class LocalContact(
        val name: String,
        val phone: String = "",
        val original: String = "",
        val normalized: String = ""
    )
}
