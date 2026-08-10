package com.kinchat.app.data.repository.chat

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import javax.inject.Inject
import javax.inject.Singleton

interface ChatSessionProvider {
    fun getCurrentUserId(): String?
}

@Singleton
class ChatSessionProviderImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ChatSessionProvider {
    override fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
}
