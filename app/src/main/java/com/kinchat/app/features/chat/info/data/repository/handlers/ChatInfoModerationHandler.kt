package com.kinchat.app.features.chat.info.data.repository.handlers

import com.kinchat.app.features.chat.info.domain.model.ReportDto
import com.kinchat.app.features.chat.info.domain.model.UserBlockDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatInfoModerationHandler @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    suspend fun toggleBlock(partnerId: String, isBlocked: Boolean): Result<Unit> = runCatching {
        val myId = currentUserId ?: throw Exception("User not authenticated")
        
        if (isBlocked) {
            supabase.postgrest["user_blocks"].insert(
                UserBlockDto(blocker_id = myId, blocked_id = partnerId)
            )
        } else {
            supabase.postgrest["user_blocks"].delete {
                filter {
                    eq("blocker_id", myId)
                    eq("blocked_id", partnerId)
                }
            }
        }
    }

    suspend fun reportUser(partnerId: String): Result<Unit> = runCatching {
        val myId = currentUserId ?: throw Exception("User not authenticated")
        
        supabase.postgrest["reports"].insert(
            ReportDto(
                reporter_id = myId,
                reported_user_id = partnerId,
                reason = "User reported via contact info",
                target_type = "user",
                status = "pending"
            )
        )
    }
}
