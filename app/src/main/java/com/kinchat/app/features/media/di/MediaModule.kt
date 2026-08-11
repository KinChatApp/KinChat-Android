package com.kinchat.app.features.media.di

import android.content.Context
import com.kinchat.app.features.media.data.repository.MediaRepositoryImpl
import com.kinchat.app.features.media.domain.repository.MediaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    @Provides
    @Singleton
    fun provideMediaRepository(@ApplicationContext context: Context): MediaRepository {
        return MediaRepositoryImpl(context)
    }
}
