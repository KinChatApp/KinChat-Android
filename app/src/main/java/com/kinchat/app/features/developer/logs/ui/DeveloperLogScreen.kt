package com.kinchat.app.features.developer.logs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinchat.app.core.logging.LogLevel
import com.kinchat.app.core.logging.LogMessage
import com.kinchat.app.features.developer.logs.viewmodel.DeveloperLogViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperLogScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeveloperLogViewModel = hiltViewModel()
) {
    val logs by viewModel.filteredLogs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()

    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-scroll to top (newest) when new logs arrive if not paused
    LaunchedEffect(logs.size, isPaused) {
        if (!isPaused && logs.isNotEmpty() && listState.firstVisibleItemIndex < 2) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Logs", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePause() }) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.ShoppingCart, // Adjust icon names as per your imports, using standard ones here. Play/Pause equivalent
                            contentDescription = "Pause/Resume",
                            tint = if (isPaused) Color.Red else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = viewModel::clearLogs) {
                        Icon(Icons.Default.Delete, "Clear Logs")
                    }
                    IconButton(onClick = {
                        val text = viewModel.getFormattedLogsForExport()
                        shareText(context, text)
                    }) {
                        Icon(Icons.Default.Share, "Export Logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search logs or tags...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear Search")
                        }
                    }
                }
            )

            // Filters
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedLevel == null,
                    onClick = { viewModel.setLevelFilter(null) },
                    label = { Text("ALL") }
                )
                LogLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { viewModel.setLevelFilter(level) },
                        label = { Text(level.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getLevelColor(level).copy(alpha = 0.2f)
                        )
                    )
                }
            }

            if (isPaused) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Live updates paused", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                }
            }

            // Log List
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItem(
                        log = log,
                        onCopyLog = { 
                            copyToClipboard(context, formatSingleLog(log))
                            Toast.makeText(context, "Log copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: LogMessage, onCopyLog: () -> Unit) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val time = formatter.format(Date(log.timestamp))
    val levelColor = getLevelColor(log.level)

    SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCopyLog() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = log.level.name,
                    color = levelColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = time, fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "[${log.tag}]", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.message,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
            log.throwable?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error.stackTraceToString(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

private fun getLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.DEBUG -> Color(0xFF2196F3) // Blue
    LogLevel.INFO -> Color(0xFF4CAF50)  // Green
    LogLevel.WARN -> Color(0xFFFF9800)  // Orange
    LogLevel.ERROR -> Color(0xFFF44336) // Red
}

private fun formatSingleLog(log: LogMessage): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    val time = format.format(Date(log.timestamp))
    val error = log.throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
    return "[$time] [${log.level.name}] ${log.tag}: ${log.message}$error"
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Log", text)
    clipboard.setPrimaryClip(clip)
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Developer Logs Export")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Export Logs via"))
}
