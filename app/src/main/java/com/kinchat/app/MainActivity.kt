package com.kinchat.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kinchat.app.core.ui.MainLayout
import com.kinchat.app.core.ui.components.BatteryOptimizationDialog
import com.kinchat.app.core.ui.components.CrashLogDialog
import com.kinchat.app.core.ui.components.NotificationPermissionEffect
import com.kinchat.app.core.utils.BatteryOptimizationHelper
import com.kinchat.app.core.logging.CrashLogManager
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.navigation.AppNavigation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var supabaseClient: SupabaseClient

    private lateinit var crashLogManager: CrashLogManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        crashLogManager = CrashLogManager(this)
        crashLogManager.setupExceptionHandler()

        handleNotificationIntent(intent)

        lifecycleScope.launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is io.github.jan.supabase.gotrue.SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val currentUserId = user?.id?.replace("-", "")?.trim()
                        val currentUserName = user?.phone ?: user?.email ?: "KinChat User"

                        if (!currentUserId.isNullOrBlank()) {
                            AppLogger.d("ZegoCloud", "🚀 Initializing ZegoCloud for Current User ID: '$currentUserId'")
                            
                            // 🚀 FIX: Save credentials so Application class can use them for offline calls
                            val prefs = getSharedPreferences("ZegoPrefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("userId", currentUserId).putString("userName", currentUserName).apply()
                            
                            (application as KinChatApplication).initZegoCloud(currentUserId, currentUserName)
                        } else {
                            AppLogger.e("ZegoCloud", "❌ Failed to init ZegoCloud: currentUserId is null or empty")
                        }
                    }
                    is io.github.jan.supabase.gotrue.SessionStatus.NotAuthenticated -> {
                        AppLogger.d("ZegoCloud", "User logged out, un-initializing ZegoCloud")
                        val prefs = getSharedPreferences("ZegoPrefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        ZegoUIKitPrebuiltCallService.unInit()
                    }
                    else -> {}
                }
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var crashLogToShow by remember { mutableStateOf(crashLogManager.getLastCrashLog()) }
                    var showBatteryDialog by remember { mutableStateOf(BatteryOptimizationHelper.shouldShowPrompt(this@MainActivity)) }

                    crashLogToShow?.let { log ->
                        CrashLogDialog(crashLog = log, onDismiss = { crashLogManager.clearCrashLog(); crashLogToShow = null })
                    }

                    if (showBatteryDialog) {
                        BatteryOptimizationDialog(
                            onDismiss = { BatteryOptimizationHelper.markPromptShown(this@MainActivity); showBatteryDialog = false },
                            onConfirm = { BatteryOptimizationHelper.markPromptShown(this@MainActivity); showBatteryDialog = false; BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this@MainActivity) }
                        )
                    }

                    NotificationPermissionEffect()
                    val navController = rememberNavController()
                    MainLayout {
                        AppNavigation(navController = navController, authRepository = authRepository)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val chatId = intent?.getStringExtra("chat_id")
        if (!chatId.isNullOrEmpty()) {
            // Navigate
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 🚀 FIX: Do NOT call unInit() here! Zego needs to stay alive in the background.
    }
}
