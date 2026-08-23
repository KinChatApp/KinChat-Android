package com.kinchat.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kinchat.app.core.designsystem.KinChatTheme
import com.kinchat.app.core.ui.MainLayout
import com.kinchat.app.core.ui.components.BatteryOptimizationDialog
import com.kinchat.app.core.ui.components.CrashLogDialog
import com.kinchat.app.core.ui.components.NotificationPermissionEffect
import com.kinchat.app.core.utils.BatteryOptimizationHelper
import com.kinchat.app.core.logging.CrashLogManager
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.domain.model.UserSettings
import com.kinchat.app.domain.repository.AppAuthState
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.SettingsRepository
import com.kinchat.app.navigation.AppNavigation
import com.kinchat.app.navigation.NavRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var crashLogManager: CrashLogManager
    private val pendingChatId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        crashLogManager = CrashLogManager(this)
        crashLogManager.setupExceptionHandler()
        handleNotificationIntent(intent)

        val prefs = getSharedPreferences("ZegoPrefs", Context.MODE_PRIVATE)
        val savedUserId = prefs.getString("userId", null)
        val initialRoute = if (!savedUserId.isNullOrBlank()) NavRoutes.DASHBOARD else NavRoutes.LOGIN

        // 🚀 FIX: Observing abstract AuthState instead of Supabase SessionStatus
        lifecycleScope.launch(Dispatchers.IO) {
            authRepository.observeAuthState().collect { state ->
                when (state) {
                    is AppAuthState.Authenticated -> {
                        if (state.userId.isNotBlank()) {
                            prefs.edit().putString("userId", state.userId).putString("userName", state.userName).apply()
                            launch(Dispatchers.Main) {
                                (application as KinChatApplication).initZegoCloud(state.userId, state.userName)
                            }
                        }
                    }
                    is AppAuthState.Unauthenticated -> {
                        prefs.edit().clear().apply()
                        launch(Dispatchers.Main) {
                            ZegoUIKitPrebuiltCallService.unInit()
                        }
                    }
                    else -> {}
                }
            }
        }

        setContent {
            val userSettings by settingsRepository.getUserSettingsFlow().collectAsState(initial = UserSettings())
            val isDarkTheme = when (userSettings.theme.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            KinChatTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
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

                    val targetChatId by pendingChatId.collectAsState()
                    LaunchedEffect(targetChatId) {
                        targetChatId?.let { id ->
                            try { navController.navigate("chat/$id") } catch (e: Exception) {}
                            pendingChatId.value = null
                        }
                    }

                    MainLayout {
                        AppNavigation(
                            navController = navController,
                            startDestination = initialRoute
                        )
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
        if (!chatId.isNullOrEmpty()) pendingChatId.value = chatId
    }
}
