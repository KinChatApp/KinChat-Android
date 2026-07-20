package com.tuktak.app.core.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object MediaDownloader {
    fun downloadMedia(context: Context, url: String, type: String, providedFileName: String? = null) {
        try {
            val uri = Uri.parse(url)
            val fallbackName = uri.lastPathSegment ?: "media_${System.currentTimeMillis()}"
            var fileName = providedFileName ?: fallbackName

            // Ensure correct extension if missing
            if (!fileName.contains(".")) {
                fileName = when (type) {
                    "image" -> "$fileName.jpg"
                    "video" -> "$fileName.mp4"
                    "audio" -> "$fileName.m4a"
                    else -> fileName
                }
            }

            val request = DownloadManager.Request(uri)
                .setTitle("Downloading $fileName")
                .setDescription("Saving file to device...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    when (type) {
                        "image" -> Environment.DIRECTORY_PICTURES
                        "video" -> Environment.DIRECTORY_MOVIES
                        "audio" -> Environment.DIRECTORY_MUSIC
                        else -> Environment.DIRECTORY_DOWNLOADS
                    },
                    "TukTak/$fileName"
                )

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to download media", Toast.LENGTH_SHORT).show()
        }
    }
}
