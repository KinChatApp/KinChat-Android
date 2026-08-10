package com.kinchat.app.data.repository

import android.util.Log
import com.kinchat.app.core.utils.PhoneNumberSanitizer
import com.kinchat.app.data.source.contacts.LocalContactsDataSource
import com.kinchat.app.data.source.contacts.RemoteContactsDataSource
import com.kinchat.app.data.source.contacts.model.DeviceContact
import com.kinchat.app.data.source.contacts.model.NormalizedContact
import com.kinchat.app.domain.model.ContactSyncResult
import com.kinchat.app.domain.model.UserContact
import com.kinchat.app.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ContactsRepositoryImpl @Inject constructor(
    private val localDataSource: LocalContactsDataSource,
    private val remoteDataSource: RemoteContactsDataSource,
    private val phoneSanitizer: PhoneNumberSanitizer
) : ContactsRepository {

    private val _contacts = MutableStateFlow<List<UserContact>>(emptyList())

    companion object {
        private const val TAG = "ContactsRepository"
        private const val MIN_PHONE_LENGTH = 11
    }

    override fun getContacts(): Flow<List<UserContact>> = _contacts.asStateFlow()

    override suspend fun loadContactsFromRemote() {
        withContext(Dispatchers.IO) {
            try {
                val userId = remoteDataSource.getCurrentUserId() ?: return@withContext
                val data = remoteDataSource.getUserContacts(userId)
                _contacts.value = data.sortedBy { it.contactName }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load contacts from remote", e)
            }
        }
    }

    override suspend fun syncDeviceContacts(): ContactSyncResult = withContext(Dispatchers.IO) {
        try {
            if (remoteDataSource.getCurrentUserId() == null) {
                return@withContext ContactSyncResult(false, "User not authenticated")
            }

            val deviceContacts = localDataSource.getDeviceContacts()
            if (deviceContacts.isEmpty()) {
                return@withContext ContactSyncResult(true)
            }

            val uniqueContacts = processAndFilterContacts(deviceContacts)
            if (uniqueContacts.isEmpty()) {
                return@withContext ContactSyncResult(true)
            }

            val contactsPayload = buildContactsPayload(uniqueContacts)
            val syncResult = remoteDataSource.syncUserContacts(contactsPayload)
            
            loadContactsFromRemote()

            if (!syncResult.success) {
                ContactSyncResult(false, syncResult.error ?: "Some contacts failed to sync")
            } else {
                ContactSyncResult(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync device contacts", e)
            ContactSyncResult(false, e.message ?: "Failed to sync device contacts")
        }
    }

    private fun processAndFilterContacts(
        deviceContacts: List<DeviceContact>
    ): List<NormalizedContact> {
        val uniqueContactsMap = linkedMapOf<String, NormalizedContact>()
        
        for (contact in deviceContacts) {
            val normalized = phoneSanitizer.sanitize(contact.phone)
            if (normalized.length >= MIN_PHONE_LENGTH && !uniqueContactsMap.containsKey(normalized)) {
                uniqueContactsMap[normalized] = NormalizedContact(
                    name = contact.name,
                    originalPhone = contact.phone,
                    normalizedPhone = normalized
                )
            }
        }
        return uniqueContactsMap.values.toList()
    }

    private suspend fun buildContactsPayload(
        uniqueContacts: List<NormalizedContact>
    ): List<JsonObject> {
        val normalizedPhonesList = uniqueContacts.map { it.normalizedPhone }
        val matchedUsers = remoteDataSource.lookupRegisteredUsers(normalizedPhonesList)
        val matchedByPhone = matchedUsers.associateBy { it.phone }

        return uniqueContacts.map { local ->
            val registeredUser = matchedByPhone[local.normalizedPhone]
            buildJsonObject {
                put("contact_name", local.name)
                put("contact_phone", local.originalPhone)
                put("contact_phone_normalized", local.normalizedPhone)
                put("registered_user_id", registeredUser?.id)
            }
        }
    }
}
