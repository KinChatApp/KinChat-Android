package com.kinchat.app.core.logging

import android.util.Log

/**
 * Global API for logging. No Context required.
 * Example: AppLogger.d("Chat", "Message Sent")
 */
object AppLogger {
    val repository = LogRepository()
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    fun init() {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("Crash", "Uncaught exception in ${thread.name}", throwable)
            // Let the app crash normally after logging
            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    
    fun e(tag: String, message: String, throwable: Throwable? = null) = 
        log(LogLevel.ERROR, tag, message, throwable)
        
    fun e(tag: String, throwable: Throwable) = 
        log(LogLevel.ERROR, tag, throwable.message ?: "Exception", throwable)

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val logMsg = LogMessage(level = level, tag = tag, message = message, throwable = throwable)
        repository.addLog(logMsg)
        
        // Mirror to standard Android Logcat for ADB usage
        when(level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
}
