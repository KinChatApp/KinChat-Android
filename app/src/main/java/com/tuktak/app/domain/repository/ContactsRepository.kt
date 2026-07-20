package com.tuktak.app.domain.repository

import com.tuktak.app.domain.model.ContactSyncResult
import com.tuktak.app.domain.model.UserContact
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    fun getContacts(): Flow<List<UserContact>>
    suspend fun syncDeviceContacts(): ContactSyncResult
    suspend fun loadContactsFromRemote()
}
