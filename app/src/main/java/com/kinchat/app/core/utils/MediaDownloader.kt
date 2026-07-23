package com.kinchat.app.core.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File

object MediaDownloader {
    private const val TAG = "MediaDownloader"
    private const val APP_FOLDER_NAME = "KinChat"

    /**
     * Downloads media using Android's DownloadManager.
     * Returns a Result containing the download ID if successful, or an exception if it fails.
     */
    fun downloadMedia(
        context: Context, 
        url: String, 
        type: String, 
        providedFileName: String? = null
    ): Result<Long> {
        val appContext = context.applicationContext

        return try {
            if (url.isBlank()) {
                throw IllegalArgumentException("Download URL cannot be blank")
            }

            val uri = Uri.parse(url)
            val fallbackName = uri.lastPathSegment ?: "media_${System.currentTimeMillis()}"
            var fileName = providedFileName ?: fallbackName
            val normalizedType = type.lowercase().trim()

            // Ensure correct extension if missing
            if (!fileName.contains(".")) {
                fileName = when (normalizedType) {
                    "image" -> "$fileName.jpg"
                    "video" -> "$fileName.mp4"
                    "audio" -> "$fileName.m4a"
                    else -> fileName
                }
            }

            val destinationDirectory = when (normalizedType) {
                "image" -> Environment.DIRECTORY_PICTURES
                "video" -> Environment.DIRECTORY_MOVIES
                "audio" -> Environment.DIRECTORY_MUSIC
                else -> Environment.DIRECTORY_DOWNLOADS
            }

            val request = DownloadManager.Request(uri).apply {
                setTitle("Downloading $fileName")
                setDescription("Saving file to device...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setDestinationInExternalPublicDir(
                    destinationDirectory,
                    "$APP_FOLDER_NAME${File.separator}$fileName"
                )
            }

            val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: throw IllegalStateException("DownloadManager service is not available on this device")

            val downloadId = downloadManager.enqueue(request)
            
            // Retained Toast as per requirement to keep existing behavior, 
            // but ideally this should be handled by the UI layer observing the Result.
            Toast.makeText(appContext, "Download started...", Toast.LENGTH_SHORT).show()
            
            Result.success(downloadId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download media from url: $url", e)
            Toast.makeText(appContext, "Failed to download media", Toast.LENGTH_SHORT).show()
            Result.failure(e)
        }
    }
}
