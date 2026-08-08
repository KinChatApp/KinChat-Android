package com.kinchat.app.di

import android.content.Context
import com.kinchat.app.data.remote.api.ChatNotificationService
import com.kinchat.app.data.remote.api.ChatNotificationServiceImpl
import com.kinchat.app.data.remote.api.ChatRpcService
import com.kinchat.app.data.remote.api.ChatRpcServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatServicesModule {

    @Provides
    @Singleton
    fun provideChatNotificationService(
        @ApplicationContext context: Context,
        supabaseClient: SupabaseClient
    ): ChatNotificationService {
        return ChatNotificationServiceImpl(context, supabaseClient)
    }

    @Provides
    @Singleton
    fun provideChatRpcService(
        supabaseClient: SupabaseClient
    ): ChatRpcService {
        return ChatRpcServiceImpl(supabaseClient)
    }
}
