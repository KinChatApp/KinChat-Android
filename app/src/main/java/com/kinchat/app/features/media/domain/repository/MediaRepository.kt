package com.kinchat.app.features.media.domain.repository

import com.kinchat.app.features.media.domain.model.MediaAlbum
import com.kinchat.app.features.media.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun getAlbums(): List<MediaAlbum>
    suspend fun getMediaByAlbum(albumId: String, limit: Int, offset: Int): List<MediaItem>
    fun observeMediaChanges(): Flow<Unit>
}
