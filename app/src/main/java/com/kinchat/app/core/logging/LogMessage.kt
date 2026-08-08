package com.kinchat.app.core.logging

import java.util.UUID

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogMessage(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)
