package com.kinchat.app.data.repository

import com.kinchat.app.core.utils.PhoneNumberSanitizer
import com.kinchat.app.data.local.db.ContactDao
import com.kinchat.app.data.local.db.ContactEntity
import com.kinchat.app.data.source.contacts.LocalContactsDataSource
import com.kinchat.app.data.source.contacts.RemoteContactsDataSource
import com.kinchat.app.domain.model.ContactSyncResult
import com.kinchat.app.domain.model.UserContact
import com.kinchat.app.domain.model.RegisteredUserDto
import com.kinchat.app.domain.repository.ContactsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val localDataSource: LocalContactsDataSource,
    private val remoteDataSource: RemoteContactsDataSource,
    private val contactDao: ContactDao,
    private val phoneSanitizer: PhoneNumberSanitizer,
    private val supabaseClient: SupabaseClient
) : ContactsRepository {

    override fun getContacts(): Flow<List<UserContact>> {
        return contactDao.observeAllContacts().map { entities ->
            entities.map {
                UserContact(
                    id = it.id,
                    userId = "",
                    contactName = it.contactName,
                    contactPhone = it.contactPhone,
                    contactPhoneNormalized = it.contactPhoneNormalized,
                    registeredUserId = it.registeredUserId
                )
            }
        }
    }

    override suspend fun syncDeviceContacts(): ContactSyncResult {
        return try {
            val deviceContacts = localDataSource.getDeviceContacts()

            val contactEntities = deviceContacts.map { deviceContact ->
                val normalizedPhone = phoneSanitizer.sanitize(deviceContact.phone)
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    contactName = deviceContact.name,
                    contactPhone = deviceContact.phone,
                    contactPhoneNormalized = normalizedPhone,
                    registeredUserId = null
                )
            }

            if (contactEntities.isNotEmpty()) {
                contactDao.insertContacts(contactEntities)
            }

            ContactSyncResult(isSuccess = true, errorMessage = null)
        } catch (e: Exception) {
            ContactSyncResult(isSuccess = false, errorMessage = "Failed to sync local contacts: ${e.message}")
        }
    }

    override suspend fun loadContactsFromRemote() {
        try {
            // ১. Room ডেটাবেস থেকে সব সেভ করা কন্টাক্ট নিন
            val localContacts = contactDao.observeAllContacts().first()
            if (localContacts.isEmpty()) return

            val normalizedPhones = localContacts.map { it.contactPhoneNormalized }.filter { it.isNotEmpty() }
            if (normalizedPhones.isEmpty()) return

            // ২. Supabase থেকে এই নম্বরগুলোর সাথে ম্যাচ করা ইউজারদের আনুন
            // ⚠️ আপনার Supabase টেবিলের নাম যদি 'profiles' বা অন্য কিছু হয়, তবে 'users' পরিবর্তন করে সেই নাম দিন
            val registeredUsers = supabaseClient.postgrest["users"] 
                .select {
                    filter {
                        isIn("phone", normalizedPhones)
                    }
                }.decodeList<RegisteredUserDto>()

            if (registeredUsers.isEmpty()) return

            // ৩. লোকাল কন্টাক্টগুলোর registeredUserId আপডেট করুন
            val updatedContacts = localContacts.map { localContact ->
                val matchedUser = registeredUsers.find { it.phone == localContact.contactPhoneNormalized }
                if (matchedUser != null) {
                    localContact.copy(registeredUserId = matchedUser.id)
                } else {
                    localContact
                }
            }

            // ৪. আপডেট করা কন্টাক্টগুলো আবার Room-এ সেভ করুন
            contactDao.insertContacts(updatedContacts)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
