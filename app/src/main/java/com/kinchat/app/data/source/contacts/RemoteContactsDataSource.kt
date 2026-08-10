package com.kinchat.app.data.source.contacts

import android.util.Log
import com.kinchat.app.domain.model.RegisteredUserDto
import com.kinchat.app.domain.model.UserContact
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class RemoteContactsDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "RemoteContactsDataSource"
        private const val LOOKUP_CHUNK_SIZE = 200
        private const val RPC_CHUNK_SIZE = 500
    }

    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    suspend fun getUserContacts(userId: String): List<UserContact> = withContext(Dispatchers.IO) {
        supabase.postgrest["user_contacts"]
            .select {
                filter { eq("user_id", userId) }
            }.decodeList<UserContact>()
    }

    suspend fun lookupRegisteredUsers(phones: List<String>): List<RegisteredUserDto> = withContext(Dispatchers.IO) {
        val matchedUsers = mutableListOf<RegisteredUserDto>()
        
        phones.chunked(LOOKUP_CHUNK_SIZE).forEach { chunk ->
            try {
                val result = supabase.postgrest["users"].select {
                    filter { isIn("phone", chunk) }
                }.decodeList<RegisteredUserDto>()
                matchedUsers.addAll(result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to look up registered users for a chunk", e)
            }
        }
        
        matchedUsers
    }

    suspend fun syncUserContacts(contactsPayload: List<JsonObject>): SyncResult = withContext(Dispatchers.IO) {
        var anyFailure = false
        var lastError: String? = null
        
        contactsPayload.chunked(RPC_CHUNK_SIZE).forEach { chunk ->
            try {
                supabase.postgrest.rpc(
                    function = "sync_user_contacts",
                    parameters = buildJsonObject {
                        put("contacts", JsonArray(chunk))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "sync_user_contacts RPC failed for a chunk", e)
                anyFailure = true
                lastError = e.message
            }
        }
        
        SyncResult(!anyFailure, lastError)
    }

    data class SyncResult(
        val success: Boolean,
        val error: String? = null
    )
}
