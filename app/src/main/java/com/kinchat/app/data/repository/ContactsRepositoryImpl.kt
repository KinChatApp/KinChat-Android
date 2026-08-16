package com.kinchat.app.data.repository

import com.kinchat.app.core.utils.PhoneNumberSanitizer
import com.kinchat.app.data.local.db.ContactDao
import com.kinchat.app.data.source.contacts.LocalContactsDataSource
import com.kinchat.app.data.source.contacts.RemoteContactsDataSource
import com.kinchat.app.domain.model.ContactSyncResult
import com.kinchat.app.domain.model.UserContact
import com.kinchat.app.domain.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val localDataSource: LocalContactsDataSource,
    private val remoteDataSource: RemoteContactsDataSource,
    private val contactDao: ContactDao,
    private val phoneSanitizer: PhoneNumberSanitizer
) : ContactsRepository {

    override fun getContacts(): Flow<List<UserContact>> {
        return contactDao.observeAllContacts().map { entities ->
            entities.map {
                UserContact(
                    id = it.id,
                    userId = "", // Offline-first মডেলে userId আপাতত ব্ল্যাঙ্ক
                    contactName = it.contactName,
                    contactPhone = it.contactPhone,
                    contactPhoneNormalized = it.contactPhoneNormalized,
                    registeredUserId = it.registeredUserId
                )
            }
        }
    }

    override suspend fun syncDeviceContacts(): ContactSyncResult {
        // TODO: Offline pending operation যুক্ত করতে হবে
        return ContactSyncResult(isSuccess = true, errorMessage = null)
    }

    override suspend fun loadContactsFromRemote() {
        // TODO: Room sync logic
    }
}
