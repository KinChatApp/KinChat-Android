package com.kinchat.app.core.logging

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DEBUG TOOL:
 * adb/root ছাড়াই ডিভাইসে error trace দেখার জন্য।
 *
 * Logs are:
 * 1. Immediately sent to the in-app Developer Log Viewer.
 * 2. Written asynchronously to Downloads/kinchat_debug_log.txt.
 *
 * IMPORTANT:
 * File I/O is intentionally moved off the caller thread so logging does not
 * block UI/startup work.
 */
object DebugLogger {

    private const val PREFS_NAME = "debug_logger_prefs"
    private const val KEY_URI = "debug_log_uri"
    private const val FILE_NAME = "kinchat_debug_log.txt"

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.getDefault()
        )

    /*
     * Dedicated background scope for physical log-file writes.
     */
    private val fileScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    /*
     * Makes file writes sequential so log lines do not race each other.
     */
    private val fileMutex = Mutex()

    /**
     * Logs immediately to the in-app logger and asynchronously to disk.
     */
    fun log(
        context: Context,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        // 1. Immediate in-app log.
        if (throwable != null) {
            AppLogger.e(
                tag,
                message,
                throwable
            )
        } else {
            AppLogger.d(
                tag,
                message
            )
        }

        // 2. Physical file logging happens asynchronously.
        val line = buildString {
            append(
                dateFormat.format(Date())
            )
            append(" [")
            append(tag)
            append("] ")
            append(message)

            if (throwable != null) {
                append("\n")
                append(
                    throwable.stackTraceToString()
                )
            }

            append("\n")
        }

        fileScope.launch {
            fileMutex.withLock {
                try {
                    appendToFile(
                        context = context.applicationContext,
                        line = line
                    )
                } catch (_: Exception) {
                    // Logger must never crash the app.
                }
            }
        }
    }

    private fun appendToFile(
        context: Context,
        line: String
    ) {
        val uri =
            getOrCreateFileUri(context)
                ?: return

        try {
            context.contentResolver
                .openOutputStream(
                    uri,
                    "wa"
                )
                ?.use { out ->
                    out.write(
                        line.toByteArray()
                    )
                    out.flush()
                }
        } catch (_: Exception) {
            clearStoredUri(context)
        }
    }

    private fun getOrCreateFileUri(
        context: Context
    ): Uri? {
        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val stored =
            prefs.getString(
                KEY_URI,
                null
            )

        if (stored != null) {
            return Uri.parse(stored)
        }

        val values =
            ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    FILE_NAME
                )

                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "text/plain"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )
                }
            }

        val resolver =
            context.contentResolver

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri(
                    "external"
                )
            }

        val uri =
            resolver.insert(
                collection,
                values
            ) ?: return null

        prefs.edit()
            .putString(
                KEY_URI,
                uri.toString()
            )
            .apply()

        return uri
    }

    private fun clearStoredUri(
        context: Context
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(KEY_URI)
            .apply()
    }

    /**
     * Clears in-app logs immediately and the physical log file asynchronously.
     */
    fun clearLog(
        context: Context
    ) {
        // 1. Clear in-app viewer immediately.
        AppLogger.repository.clearLogs()

        // 2. Clear physical file without blocking UI.
        fileScope.launch {
            fileMutex.withLock {
                val prefs =
                    context.applicationContext
                        .getSharedPreferences(
                            PREFS_NAME,
                            Context.MODE_PRIVATE
                        )

                val stored =
                    prefs.getString(
                        KEY_URI,
                        null
                    )

                if (stored != null) {
                    try {
                        context.applicationContext
                            .contentResolver
                            .delete(
                                Uri.parse(stored),
                                null,
                                null
                            )
                    } catch (_: Exception) {
                    }
                }

                prefs.edit()
                    .remove(KEY_URI)
                    .apply()
            }
        }
    }
}
