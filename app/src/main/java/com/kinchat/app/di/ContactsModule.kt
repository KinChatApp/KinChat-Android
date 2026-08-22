package com.kinchat.app.di

import com.kinchat.app.core.utils.PhoneNumberSanitizer
import com.kinchat.app.data.local.db.ContactDao
import com.kinchat.app.data.repository.ContactsRepositoryImpl
import com.kinchat.app.data.source.contacts.LocalContactsDataSource
import com.kinchat.app.data.source.contacts.RemoteContactsDataSource
import com.kinchat.app.domain.repository.ContactsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContactsModule {

    @Provides
    @Singleton
    fun provideContactsRepository(
        localDataSource: LocalContactsDataSource,
        remoteDataSource: RemoteContactsDataSource,
        contactDao: ContactDao,
        phoneSanitizer: PhoneNumberSanitizer,
        supabaseClient: SupabaseClient
    ): ContactsRepository {
        return ContactsRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            contactDao = contactDao,
            phoneSanitizer = phoneSanitizer,
            supabaseClient = supabaseClient
        )
    }
}
