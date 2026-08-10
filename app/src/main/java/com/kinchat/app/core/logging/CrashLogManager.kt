package com.kinchat.app.core.logging

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter

class CrashLogManager(context: Context) {
    // Use applicationContext to prevent memory leaks
    private val prefs = context.applicationContext.getSharedPreferences("CrashLogs", Context.MODE_PRIVATE)

    fun setupExceptionHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))
            val exceptionAsString = sw.toString()

            prefs.edit().putString(KEY_LAST_CRASH, exceptionAsString).commit()
            defaultExceptionHandler?.uncaughtException(thread, exception)
        }
    }

    fun getLastCrashLog(): String? = prefs.getString(KEY_LAST_CRASH, null)

    fun clearCrashLog() {
        prefs.edit().remove(KEY_LAST_CRASH).apply()
    }

    companion object {
        private const val KEY_LAST_CRASH = "last_crash"
    }
}
