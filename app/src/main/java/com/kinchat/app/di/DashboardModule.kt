package com.kinchat.app.di

import com.kinchat.app.data.repository.DashboardRepositoryImpl
import com.kinchat.app.domain.repository.DashboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    
    @Binds
    abstract fun bindDashboardRepository(
        impl: DashboardRepositoryImpl
    ): DashboardRepository
}
