package com.kinchat.app.features.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.features.search.domain.model.SearchResult
import com.kinchat.app.features.search.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val result: SearchResult) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
    object Empty : SearchUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchUiState.Idle)
            } else {
                searchRepository.search(query)
                    .map { result ->
                        result.fold(
                            onSuccess = { data ->
                                if (data.contacts.isEmpty() && data.messages.isEmpty()) {
                                    SearchUiState.Empty
                                } else {
                                    SearchUiState.Success(data)
                                }
                            },
                            onFailure = { error ->
                                SearchUiState.Error(error.message ?: "An unknown error occurred")
                            }
                        )
                    }
                    .catch { emit(SearchUiState.Error(it.message ?: "An error occurred")) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState.Idle
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }

    fun clearSearch() {
        _searchQuery.update { "" }
    }
}
