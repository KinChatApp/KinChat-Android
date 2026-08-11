package com.kinchat.app.features.media.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.kinchat.app.features.media.domain.model.MediaAlbum
import com.kinchat.app.features.media.domain.model.MediaItem
import com.kinchat.app.features.media.domain.model.MediaType
import com.kinchat.app.features.media.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepositoryImpl(private val context: Context) : MediaRepository {

    private val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DURATION,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
    )

    private val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
    private val selectionArgs = arrayOf(
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    )
    private val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    private val collectionUri = MediaStore.Files.getContentUri("external")

    override suspend fun getAlbums(): List<MediaAlbum> = withContext(Dispatchers.IO) {
        val albumsMap = mutableMapOf<String, AlbumTemp>()
        
        context.contentResolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val bucketId = cursor.getString(bucketIdCol) ?: "unknown"
                val bucketName = cursor.getString(bucketNameCol) ?: "Unknown"
                val uri = ContentUris.withAppendedId(collectionUri, id)

                val album = albumsMap.getOrPut(bucketId) { AlbumTemp(bucketId, bucketName, uri, 0) }
                album.count++
            }
        }

        val allMediaAlbum = MediaAlbum(
            id = "all",
            name = "Recent",
            coverUri = albumsMap.values.firstOrNull()?.coverUri,
            mediaCount = albumsMap.values.sumOf { it.count }
        )

        val folderAlbums = albumsMap.values.map { 
            MediaAlbum(id = it.id, name = it.name, coverUri = it.coverUri, mediaCount = it.count) 
        }

        listOf(allMediaAlbum) + folderAlbums.sortedByDescending { it.mediaCount }
    }

    override suspend fun getMediaByAlbum(albumId: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        
        val specificSelection = if (albumId == "all") selection else "($selection) AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
        val specificArgs = if (albumId == "all") selectionArgs else arrayOf(*selectionArgs, albumId)

        context.contentResolver.query(collectionUri, projection, specificSelection, specificArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val typeInt = cursor.getInt(mediaTypeCol)
                val mimeType = cursor.getString(mimeTypeCol) ?: ""
                val dateAdded = cursor.getLong(dateAddedCol)
                val duration = cursor.getLong(durationCol)

                val type = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaType.VIDEO else MediaType.IMAGE
                val uri = ContentUris.withAppendedId(collectionUri, id)

                mediaList.add(MediaItem(id, uri, type, mimeType, dateAdded, if (type == MediaType.VIDEO) duration else null))
            }
        }
        mediaList
    }

    private data class AlbumTemp(val id: String, val name: String, val coverUri: Uri?, var count: Int)
}
