package com.kinchat.app.di

import com.kinchat.app.core.utils.PhoneNumberSanitizer
import com.kinchat.app.data.repository.ContactsRepositoryImpl
import com.kinchat.app.data.source.contacts.LocalContactsDataSource
import com.kinchat.app.data.source.contacts.RemoteContactsDataSource
import com.kinchat.app.domain.repository.ContactsRepository
import com.kinchat.app.domain.usecase.ContactsUseCases
import com.kinchat.app.domain.usecase.GetContactsUseCase
import com.kinchat.app.domain.usecase.LoadRemoteContactsUseCase
import com.kinchat.app.domain.usecase.SyncDeviceContactsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContactsModule {

    @Provides
    @Singleton
    fun provideContactsRepository(
        localDataSource: LocalContactsDataSource,
        remoteDataSource: RemoteContactsDataSource,
        phoneSanitizer: PhoneNumberSanitizer
    ): ContactsRepository {
        return ContactsRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            phoneSanitizer = phoneSanitizer
        )
    }

    @Provides
    @Singleton
    fun provideContactsUseCases(repository: ContactsRepository): ContactsUseCases {
        return ContactsUseCases(
            getContacts = GetContactsUseCase(repository),
            syncDeviceContacts = SyncDeviceContactsUseCase(repository),
            loadRemoteContacts = LoadRemoteContactsUseCase(repository)
        )
    }
}
