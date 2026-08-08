package com.kinchat.app.data.source.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface SupabaseAuthDataSource {
    suspend fun importAuthToken(accessToken: String, refreshToken: String)
    suspend fun signOut()
    suspend fun isUserLoggedIn(): Boolean
    fun getCurrentUserId(): String?
}

class SupabaseAuthDataSourceImpl @Inject constructor(
    private val supabase: SupabaseClient
) : SupabaseAuthDataSource {
    
    override suspend fun importAuthToken(accessToken: String, refreshToken: String) {
        withContext(Dispatchers.IO) {
            supabase.auth.importAuthToken(accessToken, refreshToken)
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            supabase.auth.signOut()
        }
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return try {
            supabase.auth.awaitInitialization()
            supabase.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    override fun getCurrentUserId(): String? {
        return supabase.auth.currentSessionOrNull()?.user?.id
    }
}
