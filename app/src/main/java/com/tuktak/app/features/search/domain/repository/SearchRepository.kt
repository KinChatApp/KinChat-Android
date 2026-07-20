package com.tuktak.app.features.search.domain.repository

import com.tuktak.app.features.search.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String): Flow<Result<SearchResult>>
}
