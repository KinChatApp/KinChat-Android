package com.kinchat.app.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Thread-safe, low-overhead in-memory cache for logs.
 */
class LogRepository(private val maxLogs: Int = 2000) {
    private val _logs = MutableStateFlow<List<LogMessage>>(emptyList())
    val logs: StateFlow<List<LogMessage>> = _logs.asStateFlow()

    private val logQueue = ConcurrentLinkedDeque<LogMessage>()

    fun addLog(log: LogMessage) {
        logQueue.addFirst(log) // Add to top (newest first)
        
        while (logQueue.size > maxLogs) {
            logQueue.removeLast() // Drop oldest
        }
        
        _logs.value = logQueue.toList()
    }

    fun clearLogs() {
        logQueue.clear()
        _logs.value = emptyList()
    }

    fun getAllLogs(): List<LogMessage> = logQueue.toList()
}
