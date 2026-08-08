package com.kinchat.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kinchat.app.core.ui.MainLayout
import com.kinchat.app.core.ui.components.BatteryOptimizationDialog
import com.kinchat.app.core.ui.components.CrashLogDialog
import com.kinchat.app.core.ui.components.NotificationPermissionEffect
import com.kinchat.app.core.utils.BatteryOptimizationHelper
import com.kinchat.app.core.utils.CrashLogManager
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var crashLogManager: CrashLogManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Core Utilities
        crashLogManager = CrashLogManager(this)
        crashLogManager.setupExceptionHandler()

        handleNotificationIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var crashLogToShow by remember { 
                        mutableStateOf(crashLogManager.getLastCrashLog()) 
                    }

                    var showBatteryDialog by remember {
                        mutableStateOf(BatteryOptimizationHelper.shouldShowPrompt(this@MainActivity))
                    }

                    // 1. Crash Log Dialog
                    crashLogToShow?.let { log ->
                        CrashLogDialog(
                            crashLog = log,
                            onDismiss = {
                                crashLogManager.clearCrashLog()
                                crashLogToShow = null
                            }
                        )
                    }

                    // 2. Battery Optimization Nudge
                    if (showBatteryDialog) {
                        BatteryOptimizationDialog(
                            onDismiss = {
                                BatteryOptimizationHelper.markPromptShown(this@MainActivity)
                                showBatteryDialog = false
                            },
                            onConfirm = {
                                BatteryOptimizationHelper.markPromptShown(this@MainActivity)
                                showBatteryDialog = false
                                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this@MainActivity)
                            }
                        )
                    }

                    // 3. Notification Permission Request (Android 13+)
                    NotificationPermissionEffect()

                    // 4. Main App Navigation
                    val navController = rememberNavController()
                    MainLayout {
                        AppNavigation(
                            navController = navController,
                            authRepository = authRepository
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update current intent
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val chatId = intent?.getStringExtra("chat_id")
        if (!chatId.isNullOrEmpty()) {
            // TODO: Route to specific chat via navigation intent hooks
        }
    }
}
