package com.kinchat.app.features.media.domain.model

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val mimeType: String,
    val dateAdded: Long,
    val durationMs: Long? = null
)

data class MediaAlbum(
    val id: String,
    val name: String,
    val coverUri: Uri?,
    val mediaCount: Int
)
