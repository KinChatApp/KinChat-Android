package com.kinchat.app.di

import com.kinchat.app.BuildConfig
import com.kinchat.app.core.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.functions.Functions
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        AppLogger.i("SupabaseConfig", "Initializing Supabase Client...")
        return try {
            val client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
                install(Realtime)
                install(Functions) // 🚀 Edge Functions

                // WebSocket (Realtime)
                httpEngine = OkHttp.create()
            }
            AppLogger.i("SupabaseConfig", "✅ Supabase Client Initialized Successfully")
            client
        } catch (e: Exception) {
            AppLogger.e("SupabaseConfig", "🚨 Supabase Initialization Failed", e)
            throw e
        }
    }
}
