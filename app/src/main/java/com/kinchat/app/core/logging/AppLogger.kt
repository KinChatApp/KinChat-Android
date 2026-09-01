package com.kinchat.app.core.logging

import android.os.SystemClock
import android.util.Log

/**
 * Global API for logging. No Context required.
 *
 * Example:
 * AppLogger.d("Chat", "Message Sent")
 *
 * Startup profiling:
 * AppLogger.startup("Application.onCreate START")
 * AppLogger.startup("MainActivity.onCreate START")
 */
object AppLogger {

    val repository = LogRepository()

    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    /*
     * Monotonic clock for startup profiling.
     * elapsedRealtime() is not affected by system clock changes.
     */
    @Volatile
    private var startupStartTimeMs: Long = SystemClock.elapsedRealtime()

    fun init() {
        startupStartTimeMs = SystemClock.elapsedRealtime()

        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e(
                "Crash",
                "Uncaught exception in ${thread.name}",
                throwable
            )

            // Let the app crash normally after logging.
            defaultExceptionHandler?.uncaughtException(
                thread,
                throwable
            )
        }
    }

    /**
     * Logs a startup timing checkpoint.
     *
     * Example output:
     * [STARTUP +842ms] MainActivity setContent START
     */
    fun startup(message: String) {
        val elapsedMs =
            SystemClock.elapsedRealtime() - startupStartTimeMs

        val formatted =
            "[STARTUP +${elapsedMs}ms] $message"

        log(
            level = LogLevel.INFO,
            tag = "KinChatStartup",
            message = formatted
        )
    }

    /**
     * Resets the startup timer.
     *
     * Useful when testing a complete fresh launch.
     */
    fun resetStartupTimer() {
        startupStartTimeMs = SystemClock.elapsedRealtime()
    }

    fun d(
        tag: String,
        message: String
    ) = log(
        LogLevel.DEBUG,
        tag,
        message
    )

    fun i(
        tag: String,
        message: String
    ) = log(
        LogLevel.INFO,
        tag,
        message
    )

    fun w(
        tag: String,
        message: String
    ) = log(
        LogLevel.WARN,
        tag,
        message
    )

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) = log(
        LogLevel.ERROR,
        tag,
        message,
        throwable
    )

    fun e(
        tag: String,
        throwable: Throwable
    ) = log(
        LogLevel.ERROR,
        tag,
        throwable.message ?: "Exception",
        throwable
    )

    private fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val logMsg = LogMessage(
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )

        repository.addLog(logMsg)

        // Mirror to standard Android Logcat for ADB usage.
        when (level) {
            LogLevel.DEBUG -> {
                Log.d(tag, message, throwable)
            }

            LogLevel.INFO -> {
                Log.i(tag, message, throwable)
            }

            LogLevel.WARN -> {
                Log.w(tag, message, throwable)
            }

            LogLevel.ERROR -> {
                Log.e(tag, message, throwable)
            }
        }
    }
}
