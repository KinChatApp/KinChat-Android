package com.kinchat.app.features.search.data.repository

import com.kinchat.app.data.local.db.ChatMessageDao
import com.kinchat.app.features.search.domain.model.SearchResult
import com.kinchat.app.features.search.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val messageDao: ChatMessageDao
) : SearchRepository {

    override fun search(query: String): Flow<Result<SearchResult>> = flow {
        try {
            // TODO: Room FTS থেকে লোকাল সার্চ করে SearchResult-এ ম্যাপ করতে হবে
            emit(Result.success(SearchResult(emptyList(), emptyList())))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
