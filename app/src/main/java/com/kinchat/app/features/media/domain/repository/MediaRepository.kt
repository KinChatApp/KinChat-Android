package com.kinchat.app.features.media.domain.repository

import com.kinchat.app.features.media.domain.model.MediaAlbum
import com.kinchat.app.features.media.domain.model.MediaItem

interface MediaRepository {
    suspend fun getAlbums(): List<MediaAlbum>
    suspend fun getMediaByAlbum(albumId: String): List<MediaItem>
}
