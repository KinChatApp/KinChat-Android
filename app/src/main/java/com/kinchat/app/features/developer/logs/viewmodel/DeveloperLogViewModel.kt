package com.kinchat.app.features.developer.logs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.core.logging.LogLevel
import com.kinchat.app.core.logging.LogMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DeveloperLogViewModel @Inject constructor() : ViewModel() {
    private val repository = AppLogger.repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedLevel = MutableStateFlow<LogLevel?>(null)
    val selectedLevel = _selectedLevel.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()
    
    private val _pausedLogs = MutableStateFlow<List<LogMessage>>(emptyList())

    val filteredLogs = combine(
        repository.logs,
        _searchQuery,
        _selectedLevel,
        _isPaused
    ) { liveLogs, query, level, paused ->
        val logsToProcess = if (paused) _pausedLogs.value else liveLogs
        
        logsToProcess.filter { log ->
            val matchesQuery = query.isBlank() || 
                log.message.contains(query, ignoreCase = true) || 
                log.tag.contains(query, ignoreCase = true)
            val matchesLevel = level == null || log.level == level
            matchesQuery && matchesLevel
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    
    fun setLevelFilter(level: LogLevel?) { _selectedLevel.value = level }
    
    fun togglePause() {
        val willPause = !_isPaused.value
        if (willPause) {
            _pausedLogs.value = repository.getAllLogs()
        }
        _isPaused.value = willPause
    }

    fun clearLogs() { repository.clearLogs() }
    
    fun getFormattedLogsForExport(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return repository.getAllLogs().joinToString("\n") { log ->
            val time = format.format(Date(log.timestamp))
            val error = log.throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
            "[$time] [${log.level.name}] ${log.tag}: ${log.message}$error"
        }
    }
}
