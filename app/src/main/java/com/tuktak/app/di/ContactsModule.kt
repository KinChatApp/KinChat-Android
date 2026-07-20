package com.tuktak.app.di

import android.content.Context
import com.tuktak.app.data.repository.ContactsRepositoryImpl
import com.tuktak.app.domain.repository.ContactsRepository
import com.tuktak.app.domain.usecase.ContactsUseCases
import com.tuktak.app.domain.usecase.GetContactsUseCase
import com.tuktak.app.domain.usecase.LoadRemoteContactsUseCase
import com.tuktak.app.domain.usecase.SyncDeviceContactsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContactsModule {

    @Provides
    @Singleton
    fun provideContactsRepository(
        supabase: SupabaseClient,
        @ApplicationContext context: Context
    ): ContactsRepository {
        return ContactsRepositoryImpl(supabase, context)
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
