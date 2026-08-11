package com.kinchat.app.features.media.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.features.media.domain.model.MediaAlbum
import com.kinchat.app.features.media.domain.model.MediaItem
import com.kinchat.app.features.media.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MEDIA_SELECTION_LIMIT = 30
private const val MEDIA_PAGE_SIZE = 200

data class MediaPickerState(
    val albums: List<MediaAlbum> = emptyList(),
    val currentAlbum: MediaAlbum? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedItems: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isAlbumSheetVisible: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: MediaPickerError? = null,
    val showPreview: Boolean = false,
    val captionText: String = "",
    val cameraUri: Uri? = null,
    val showSelectionLimitMessage: Boolean = false
)

enum class MediaPickerError { LOAD_FAILED, ALBUM_LOAD_FAILED }

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MediaPickerState())
    val state: StateFlow<MediaPickerState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var currentOffset = 0

    init {
        viewModelScope.launch {
            repository.observeMediaChanges().collect {
                reloadCurrentAlbumSilently()
            }
        }
    }

    fun loadMedia() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, error = null)
            }
            val albums = safeQuery { repository.getAlbums() }

            if (albums == null) {
                _state.update { it.copy(isLoading = false, error = MediaPickerError.LOAD_FAILED) }
                return@launch
            }

            val firstAlbum = albums.firstOrNull()
            if (firstAlbum != null) {
                _state.update {
                    it.copy(albums = albums, currentAlbum = firstAlbum, error = null)
                }
                loadFirstPage(firstAlbum.id)
            } else {
                _state.update { it.copy(albums = albums, isLoading = false, hasMore = false) }
            }
        }
    }

    fun retry() {
        val album = _state.value.currentAlbum
        if (album == null) {
            loadMedia()
        } else {
            selectAlbum(album)
        }
    }

    fun selectAlbum(album: MediaAlbum) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    isAlbumSheetVisible = false,
                    currentAlbum = album,
                    mediaItems = emptyList(),
                    hasMore = true
                )
            }
            loadFirstPage(album.id)
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore || current.isLoading) return
        val albumId = current.currentAlbum?.id ?: return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val page = safeQuery { repository.getMediaByAlbum(albumId, MEDIA_PAGE_SIZE, currentOffset) }

            if (page == null) {
                _state.update { it.copy(isLoadingMore = false) }
                return@launch
            }

            _state.update { currentState ->
                currentState.copy(
                    mediaItems = currentState.mediaItems + page,
                    isLoadingMore = false,
                    hasMore = page.size == MEDIA_PAGE_SIZE
                )
            }
            currentOffset += page.size
        }
    }

    private suspend fun loadFirstPage(albumId: String) {
        currentOffset = 0
        _state.update { it.copy(isLoading = true, error = null, isLoadingMore = false) }
        val page = safeQuery { repository.getMediaByAlbum(albumId, MEDIA_PAGE_SIZE, 0) }

        if (page == null) {
            _state.update { it.copy(isLoading = false, error = MediaPickerError.ALBUM_LOAD_FAILED) }
            return
        }

        currentOffset = page.size
        _state.update {
            it.copy(mediaItems = page, isLoading = false, hasMore = page.size == MEDIA_PAGE_SIZE)
        }
    }

    private suspend fun reloadCurrentAlbumSilently() {
        val album = _state.value.currentAlbum ?: return
        val page = safeQuery { repository.getMediaByAlbum(album.id, MEDIA_PAGE_SIZE, 0) } ?: return

        currentOffset = page.size
        _state.update {
            it.copy(mediaItems = page, hasMore = page.size == MEDIA_PAGE_SIZE)
        }
    }

    private suspend fun <T> safeQuery(block: suspend () -> T): T? = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.e("MediaPickerVM", "Media query failed", e)
        null
    }

    fun toggleSelection(item: MediaItem) {
        _state.update { currentState ->
            val selectedIds = currentState.selectedIds
            when {
                item.id in selectedIds -> {
                    val selectedItems = currentState.selectedItems.filterNot { it.id == item.id }
                    currentState.copy(
                        selectedItems = selectedItems,
                        selectedIds = selectedIds - item.id
                    )
                }
                selectedIds.size >= MEDIA_SELECTION_LIMIT -> {
                    currentState.copy(showSelectionLimitMessage = true)
                }
                else -> {
                    currentState.copy(
                        selectedItems = currentState.selectedItems + item,
                        selectedIds = selectedIds + item.id
                    )
                }
            }
        }
    }

    fun resetSelectionLimitMessage() {
        _state.update { it.copy(showSelectionLimitMessage = false) }
    }

    fun toggleAlbumSheet(isVisible: Boolean) {
        _state.update { it.copy(isAlbumSheetVisible = isVisible) }
    }

    fun setShowPreview(isVisible: Boolean) {
        _state.update { it.copy(showPreview = isVisible) }
    }

    fun setCaptionText(text: String) {
        _state.update { it.copy(captionText = text) }
    }

    fun setCameraUri(uri: Uri?) {
        _state.update { it.copy(cameraUri = uri) }
    }
}
