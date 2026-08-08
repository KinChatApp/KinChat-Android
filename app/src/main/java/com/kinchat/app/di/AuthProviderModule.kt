package com.kinchat.app.di

import com.kinchat.app.features.auth.data.provider.FcmTokenProviderImpl
import com.kinchat.app.features.auth.domain.provider.FcmTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthProviderModule {

    @Binds
    abstract fun bindFcmTokenProvider(
        fcmTokenProviderImpl: FcmTokenProviderImpl
    ): FcmTokenProvider
}
