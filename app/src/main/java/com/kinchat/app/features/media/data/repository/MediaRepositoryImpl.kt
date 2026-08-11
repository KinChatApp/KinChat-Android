package com.kinchat.app.features.media.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.features.media.domain.model.MediaAlbum
import com.kinchat.app.features.media.domain.model.MediaItem
import com.kinchat.app.features.media.domain.model.MediaType
import com.kinchat.app.features.media.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class MediaRepositoryImpl(private val context: android.content.Context) : MediaRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    private val baseProjection = mutableListOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.Files.FileColumns.DURATION)
        }
    }

    private val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
    private val selectionArgs = arrayOf(
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
    )
    private val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns._ID} DESC"
    private val collectionUri = MediaStore.Files.getContentUri("external")

    override suspend fun getAlbums(): List<MediaAlbum> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
        )
        val albumsMap = mutableMapOf<String, AlbumTemp>()

        runCatching {
            contentResolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
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
        }.onFailure { AppLogger.e("MediaRepo", "getAlbums failed", it) }

        if (albumsMap.isEmpty()) return@withContext emptyList()

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

    override suspend fun getMediaByAlbum(albumId: String, limit: Int, offset: Int): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val mediaList = mutableListOf<MediaItem>()

            val specificSelection = if (albumId == "all") selection else "($selection) AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
            val specificArgs = if (albumId == "all") selectionArgs else arrayOf(*selectionArgs, albumId)

            runCatching {
                val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bundle = Bundle().apply {
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, specificSelection)
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, specificArgs)
                        putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    }
                    contentResolver.query(collectionUri, baseProjection.toTypedArray(), bundle, null)
                } else {
                    val pagedSortOrder = "$sortOrder LIMIT $limit OFFSET $offset"
                    contentResolver.query(collectionUri, baseProjection.toTypedArray(), specificSelection, specificArgs, pagedSortOrder)
                }

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val mediaTypeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val mimeTypeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                    val durationCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        c.getColumnIndex(MediaStore.Files.FileColumns.DURATION)
                    } else -1

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val typeInt = c.getInt(mediaTypeCol)
                        val mimeType = c.getString(mimeTypeCol) ?: ""
                        val dateAdded = c.getLong(dateAddedCol)
                        val duration = if (durationCol != -1) c.getLong(durationCol) else 0L

                        val type = if (typeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaType.VIDEO else MediaType.IMAGE
                        val uri = ContentUris.withAppendedId(collectionUri, id)

                        mediaList.add(MediaItem(id, uri, type, mimeType, dateAdded, if (type == MediaType.VIDEO) duration else null))
                    }
                }
            }.onFailure { AppLogger.e("MediaRepo", "getMediaByAlbum failed for $albumId", it) }

            mediaList
        }

    override fun observeMediaChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        contentResolver.registerContentObserver(collectionUri, true, observer)
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    private data class AlbumTemp(val id: String, val name: String, val coverUri: Uri?, var count: Int)
}
