package com.kinchat.app.domain.usecase

import com.kinchat.app.domain.repository.ContactsRepository
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    operator fun invoke() = repository.getContacts()
}

class SyncDeviceContactsUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke() = repository.syncDeviceContacts()
}

class LoadRemoteContactsUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke() = repository.loadContactsFromRemote()
}

// 🚀 FIX: 'data class' থেকে সাধারণ 'class' করা হয়েছে এবং '@Inject constructor' বসানো হয়েছে।
class ContactsUseCases @Inject constructor(
    val getContacts: GetContactsUseCase,
    val syncDeviceContacts: SyncDeviceContactsUseCase,
    val loadRemoteContacts: LoadRemoteContactsUseCase
)
