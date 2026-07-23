package com.kinchat.app.domain.repository

import com.kinchat.app.domain.model.ContactSyncResult
import com.kinchat.app.domain.model.UserContact
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    fun getContacts(): Flow<List<UserContact>>
    suspend fun syncDeviceContacts(): ContactSyncResult
    suspend fun loadContactsFromRemote()
}
