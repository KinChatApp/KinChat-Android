package com.kinchat.app.di

import com.kinchat.app.data.source.auth.DeviceTokenDataSource
import com.kinchat.app.data.source.auth.DeviceTokenDataSourceImpl
import com.kinchat.app.data.source.auth.SupabaseAuthDataSource
import com.kinchat.app.data.source.auth.SupabaseAuthDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindSupabaseAuthDataSource(
        impl: SupabaseAuthDataSourceImpl
    ): SupabaseAuthDataSource

    @Binds
    @Singleton
    abstract fun bindDeviceTokenDataSource(
        impl: DeviceTokenDataSourceImpl
    ): DeviceTokenDataSource
}
