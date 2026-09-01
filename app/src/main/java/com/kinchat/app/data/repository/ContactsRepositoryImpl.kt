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
                    registeredUserId = it.registeredUserId,
                    profileName = it.profileName,
                    username = it.username,
                    avatarUrl = it.avatarUrl
                )
            }
        }
    }

    override suspend fun syncDeviceContacts(): ContactSyncResult {
        return try {
            val deviceContacts = localDataSource.getDeviceContacts()
            val existingContacts = contactDao.observeAllContacts().first().associateBy { it.contactPhoneNormalized }

            val contactEntities = deviceContacts
                .map { it.copy(phone = phoneSanitizer.sanitize(it.phone)) }
                .filter { it.phone.isNotBlank() }
                .associateBy { it.phone }
                .values
                .map { deviceContact ->
                    val normalizedPhone = deviceContact.phone
                    val existing = existingContacts[normalizedPhone]

                    ContactEntity(
                        id = normalizedPhone,
                        contactPhoneNormalized = normalizedPhone,
                        contactName = deviceContact.name, 
                        contactPhone = deviceContact.phone,
                        registeredUserId = existing?.registeredUserId,
                        profileName = existing?.profileName,
                        username = existing?.username,
                        avatarUrl = existing?.avatarUrl
                    )
                }

            val currentNormalizedPhones = contactEntities.map { it.contactPhoneNormalized }.toSet()
            val staleContacts = existingContacts.values.filter { it.contactPhoneNormalized !in currentNormalizedPhones }

            if (staleContacts.isNotEmpty()) {
                contactDao.deleteContacts(staleContacts)
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
            val localContacts = contactDao.observeAllContacts().first()
            if (localContacts.isEmpty()) return

            val normalizedPhones = localContacts.map { it.contactPhoneNormalized }.filter { it.isNotEmpty() }
            if (normalizedPhones.isEmpty()) return

            val registeredUsersMap = try {
                supabaseClient.postgrest["users"]
                    .select {
                        filter {
                            isIn("phone", normalizedPhones)
                        }
                    }.decodeList<RegisteredUserDto>().associateBy { it.phone }
            } catch (e: Exception) {
                e.printStackTrace()
                return 
            }

            val updatedContacts = localContacts.map { localContact ->
                val matchedUser = registeredUsersMap[localContact.contactPhoneNormalized]
                localContact.copy(
                    registeredUserId = matchedUser?.id,
                    profileName = matchedUser?.displayName,
                    username = matchedUser?.username,
                    avatarUrl = matchedUser?.avatarUrl
                )
            }

            contactDao.insertContacts(updatedContacts)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
