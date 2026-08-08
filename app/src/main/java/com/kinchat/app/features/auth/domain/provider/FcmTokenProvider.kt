package com.kinchat.app.features.auth.domain.provider

interface FcmTokenProvider {
    suspend fun getToken(): String?
}
