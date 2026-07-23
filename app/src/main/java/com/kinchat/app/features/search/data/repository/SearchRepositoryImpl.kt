package com.kinchat.app.features.search.data.repository

import com.kinchat.app.features.search.data.model.SearchRpcParams
import com.kinchat.app.features.search.data.model.SearchRpcResponseDto
import com.kinchat.app.features.search.domain.model.ContactSearchResult
import com.kinchat.app.features.search.domain.model.MessageSearchResult
import com.kinchat.app.features.search.domain.model.SearchResult
import com.kinchat.app.features.search.domain.repository.SearchRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : SearchRepository {

    override fun search(query: String): Flow<Result<SearchResult>> = flow {
        try {
            val user = supabase.auth.currentUserOrNull()
                ?: throw IllegalStateException("User not authenticated")
            val userId = user.id

            // Single network call to the Supabase PostgreSQL RPC function
            val response = supabase.postgrest.rpc(
                function = "search_app_content",
                parameters = SearchRpcParams(p_user_id = userId, p_query = query)
            ).decodeAs<SearchRpcResponseDto>()

            // Map DTOs to Domain Models
            val mappedContacts = response.contacts.map {
                ContactSearchResult(
                    id = it.id,
                    name = it.contactName,
                    phone = it.contactPhone,
                    registeredUserId = it.registeredUserId
                )
            }

            val mappedMessages = response.messages.map {
                MessageSearchResult(
                    id = it.id,
                    content = it.content,
                    createdAt = it.createdAt,
                    displayName = it.displayName,
                    otherUserId = it.otherUserId
                )
            }

            emit(
                Result.success(
                    SearchResult(
                        contacts = mappedContacts,
                        messages = mappedMessages
                    )
                )
            )

        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
