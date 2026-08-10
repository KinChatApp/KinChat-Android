package com.kinchat.app.core.logging

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kinchat.app.core.logging.AppLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🚀 DEBUG TOOL: adb/root ছাড়া ডিভাইসে error trace দেখার জন্য।
 * Downloads ফোল্ডারে plain text ফাইলে log লিখে রাখে, Termux দিয়ে সহজে পড়া যায়।
 * এটা production build এ থাকলেও সমস্যা নেই, শুধু একটা ছোট text file লেখে।
 * 
 * [UPDATED]: এখন এটি ফাইলে সেভ করার পাশাপাশি ইন-অ্যাপ Developer Log Viewer-এও ডেটা পাঠাবে।
 */
object DebugLogger {

    private const val PREFS_NAME = "debug_logger_prefs"
    private const val KEY_URI = "debug_log_uri"
    private const val FILE_NAME = "kinchat_debug_log.txt"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Synchronized
    fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        
        // ১. ইন-অ্যাপ ফ্লোটিং UI তে লগ পাঠানো (নতুন ফিচার)
        if (throwable != null) {
            AppLogger.e(tag, message, throwable)
        } else {
            AppLogger.d(tag, message)
        }

        // ২. আগের মতোই ফাইলে সেভ করার কাজ (আপনার অরিজিনাল কোড)
        try {
            val line = buildString {
                append(dateFormat.format(Date()))
                append(" [").append(tag).append("] ")
                append(message)
                if (throwable != null) {
                    append("\n")
                    append(throwable.stackTraceToString())
                }
                append("\n")
            }
            appendToFile(context, line)
        } catch (e: Exception) {
            // Logger নিজে কখনো crash করাবে না
        }
    }

    private fun appendToFile(context: Context, line: String) {
        val uri = getOrCreateFileUri(context) ?: return
        try {
            context.contentResolver.openOutputStream(uri, "wa")?.use { out ->
                out.write(line.toByteArray())
            }
        } catch (e: Exception) {
            clearStoredUri(context)
        }
    }

    private fun getOrCreateFileUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_URI, null)
        if (stored != null) return Uri.parse(stored)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, values) ?: return null
        prefs.edit().putString(KEY_URI, uri.toString()).apply()
        return uri
    }

    private fun clearStoredUri(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_URI).apply()
    }

    fun clearLog(context: Context) {
        // ১. ইন-অ্যাপ ভিউয়ারের লগ ক্লিয়ার করা
        AppLogger.repository.clearLogs()

        // ২. ফিজিক্যাল ফাইলের লগ ক্লিয়ার করা
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_URI, null)
        if (stored != null) {
            try { context.contentResolver.delete(Uri.parse(stored), null, null) } catch (_: Exception) {}
            prefs.edit().remove(KEY_URI).apply()
        }
    }
}
