package com.kinchat.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kinchat.app.BuildConfig
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.remote.redis.UpstashRedisApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.UPSTASH_REDIS_REST_TOKEN}")
                .build()
            chain.proceed(request)
        }

        // 🚀 NEW: vConsole-এর মতো নেটওয়ার্ক ট্র্যাকিং ইন্টারসেপ্টর
        val appLoggerInterceptor = Interceptor { chain ->
            val request = chain.request()
            AppLogger.d("NetworkAPI", "➡️ REQ: ${request.method} ${request.url}")
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful) {
                    AppLogger.d("NetworkAPI", "✅ RES: [${response.code}] ${request.url}")
                } else {
                    AppLogger.e("NetworkAPI", "❌ RES ERROR: [${response.code}] ${request.url} - ${response.message}")
                }
                response
            } catch (e: Exception) {
                AppLogger.e("NetworkAPI", "🚨 NETWORK FAIL: ${request.url}", e)
                throw e
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(appLoggerInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS) // WebSocket-এর জন্য যুক্ত করা হলো
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.UPSTASH_REDIS_REST_URL.let { if (it.endsWith("/")) it else "$it/" })
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideUpstashRedisApi(retrofit: Retrofit): UpstashRedisApi {
        return retrofit.create(UpstashRedisApi::class.java)
    }
}
