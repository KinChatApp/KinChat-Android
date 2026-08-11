package com.kinchat.app.features.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.features.media.domain.model.MediaAlbum
import com.kinchat.app.features.media.domain.model.MediaItem
import com.kinchat.app.features.media.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaPickerState(
    val albums: List<MediaAlbum> = emptyList(),
    val currentAlbum: MediaAlbum? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedItems: List<MediaItem> = emptyList(),
    val isAlbumSheetVisible: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MediaPickerState())
    val state: StateFlow<MediaPickerState> = _state.asStateFlow()

    fun loadMedia() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val albums = repository.getAlbums()
            val firstAlbum = albums.firstOrNull()
            
            if (firstAlbum != null) {
                val media = repository.getMediaByAlbum(firstAlbum.id)
                _state.update { it.copy(albums = albums, currentAlbum = firstAlbum, mediaItems = media, isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectAlbum(album: MediaAlbum) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isAlbumSheetVisible = false, currentAlbum = album) }
            val media = repository.getMediaByAlbum(album.id)
            _state.update { it.copy(mediaItems = media, isLoading = false) }
        }
    }

    fun toggleSelection(item: MediaItem) {
        _state.update { currentState ->
            val selected = currentState.selectedItems.toMutableList()
            if (selected.any { it.id == item.id }) {
                selected.removeAll { it.id == item.id }
            } else {
                if (selected.size < 30) selected.add(item) // Max 30 config
            }
            currentState.copy(selectedItems = selected)
        }
    }

    fun toggleAlbumSheet(isVisible: Boolean) {
        _state.update { it.copy(isAlbumSheetVisible = isVisible) }
    }
}
