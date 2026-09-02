package com.kinchat.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kinchat.app.core.designsystem.KinChatTheme
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.core.ui.MainLayout
import com.kinchat.app.core.ui.components.BatteryOptimizationDialog
import com.kinchat.app.core.ui.components.NotificationPermissionEffect
import com.kinchat.app.core.utils.BatteryOptimizationHelper
import com.kinchat.app.data.repository.chat.sync.PendingSyncCoordinator
import com.kinchat.app.domain.model.UserSettings
import com.kinchat.app.domain.repository.AppAuthState
import com.kinchat.app.domain.repository.AuthRepository
import com.kinchat.app.domain.repository.SettingsRepository
import com.kinchat.app.navigation.AppNavigation
import com.kinchat.app.navigation.NavRoutes
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: Lazy<AuthRepository>

    @Inject
    lateinit var settingsRepository: Lazy<SettingsRepository>

    @Inject
    lateinit var pendingSyncCoordinator: Lazy<PendingSyncCoordinator>

    private val pendingChatId = MutableStateFlow<String?>(null)
    private val userSettings = MutableStateFlow(UserSettings())
    private var zegoInitializedForUserId: String? = null
    private var keepSplashScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLogger.startup("MainActivity.onCreate START")

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        super.onCreate(savedInstanceState)

        AppLogger.startup("MainActivity after super.onCreate")

        handleNotificationIntent(intent)

        val prefs = getSharedPreferences("ZegoPrefs", Context.MODE_PRIVATE)
        val savedUserId = prefs.getString("userId", null)?.trim()
        val initialRoute = if (!savedUserId.isNullOrBlank()) NavRoutes.DASHBOARD else NavRoutes.LOGIN

        AppLogger.startup("Initial route resolved: $initialRoute")

        setContent {
            var mainContentReady by remember { mutableStateOf(false) }
            val currentUserSettings by userSettings.collectAsState()

            val isDarkTheme = when (currentUserSettings.theme.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // Phase 1: Lightweight Frame Rendering
            LaunchedEffect(Unit) {
                AppLogger.startup("LIGHTWEIGHT FIRST COMPOSITION")
                withFrameNanos { }
                withFrameNanos { }
                mainContentReady = true
                AppLogger.startup("LIGHTWEIGHT FRAMES COMPLETED")
            }

            // Phase 2: Release the real Android splash screen once the first frame is drawn
            LaunchedEffect(mainContentReady) {
                if (!mainContentReady) return@LaunchedEffect

                withFrameNanos { }
                withFrameNanos { }

                keepSplashScreen = false
                AppLogger.startup("SPLASH RELEASED")
            }

            // Phase 3: Deferred SDK Initialization (runs only after splash is released)
            LaunchedEffect(mainContentReady) {
                if (!mainContentReady) return@LaunchedEffect

                // splash release-এর ২ ফ্রেম + আরও ২ ফ্রেম বাফার
                withFrameNanos { }
                withFrameNanos { }
                withFrameNanos { }
                withFrameNanos { }

                AppLogger.startup("Deferred startup STARTED via LaunchedEffect")

                // SDKs running on Main Thread AFTER UI is drawn
                try {
                    (applicationContext as? KinChatApplication)?.initializeDeferredServices()
                } catch (e: Exception) {
                    AppLogger.e("KinChatStartup", "Deferred application initialization failed: ${e.message}", e)
                }

                // IO-bound Background Tasks
                launch(Dispatchers.IO) {
                    try {
                        settingsRepository.get().getUserSettingsFlow().collect { settings ->
                            userSettings.value = settings
                        }
                    } catch (e: Exception) {
                        AppLogger.e("StartupSettings", "Failed to observe user settings: ${e.message}", e)
                    }
                }

                launch(Dispatchers.IO) {
                    try {
                        pendingSyncCoordinator.get().startMonitoring()
                        pendingSyncCoordinator.get().triggerSync()
                    } catch (e: Exception) {
                        AppLogger.e("StartupSync", "Failed to start deferred sync: ${e.message}", e)
                    }

                    try {
                        authRepository.get().observeAuthState().collect { state ->
                            when (state) {
                                is AppAuthState.Authenticated -> {
                                    val userId = state.userId.trim()
                                    if (userId.isBlank()) return@collect

                                    prefs.edit()
                                        .putString("userId", userId)
                                        .putString("userName", state.userName)
                                        .apply()

                                    if (zegoInitializedForUserId == userId) return@collect

                                    zegoInitializedForUserId = userId

                                    withContext(Dispatchers.Main) {
                                        if (isFinishing || isDestroyed) return@withContext

                                        // Scheduling Workaround: Zego requires Main Thread.
                                        // এই কোড splash release-এর পরে চলে, তাই এই delay এখন
                                        // মূল UI jank-এর কারণ না; শুধু Zego init টাইমিং সেফটি।
                                        delay(1000)

                                        try {
                                            (application as KinChatApplication).initZegoCloud(
                                                userId = userId,
                                                userName = state.userName
                                            )
                                        } catch (e: Exception) {
                                            AppLogger.e("ZegoCloud", "Deferred Zego initialization failed: ${e.message}", e)
                                        }
                                    }
                                }
                                is AppAuthState.Unauthenticated -> {
                                    prefs.edit().clear().apply()
                                    zegoInitializedForUserId = null

                                    withContext(Dispatchers.Main) {
                                        try {
                                            ZegoUIKitPrebuiltCallService.unInit()
                                        } catch (e: Exception) {
                                            AppLogger.e("ZegoCloud", "ZegoCloud unInit failed: ${e.message}", e)
                                        }
                                    }
                                }
                                else -> { /* Unknown auth state: no action. */ }
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("StartupAuth", "Failed to observe authentication state: ${e.message}", e)
                    }
                }
            }

            KinChatTheme(darkTheme = isDarkTheme) {
                if (!mainContentReady) {
                    // System splash এখনও দেখাচ্ছে; শুধু background surface, কোনো fake logo নেই
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {}
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        var showBatteryDialog by remember {
                            mutableStateOf(BatteryOptimizationHelper.shouldShowPrompt(this@MainActivity))
                        }

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

                        NotificationPermissionEffect()

                        val navController = rememberNavController()
                        val targetChatId by pendingChatId.collectAsState(initial = null)

                        LaunchedEffect(targetChatId) {
                            val id = targetChatId ?: return@LaunchedEffect
                            try {
                                navController.navigate("chat/$id")
                            } catch (e: Exception) {
                                AppLogger.e("Navigation", "Failed to navigate to chat: ${e.message}", e)
                            }
                            pendingChatId.value = null
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

        AppLogger.startup("MainActivity.onCreate END")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val chatId = intent?.getStringExtra("chat_id")?.trim()
        if (!chatId.isNullOrBlank()) {
            pendingChatId.value = chatId
        }
    }
}
