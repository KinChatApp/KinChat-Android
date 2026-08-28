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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var pendingSyncCoordinator: PendingSyncCoordinator

    private val pendingChatId = MutableStateFlow<String?>(null)

    // Prevent duplicate Zego initialization for the same user.
    private var zegoInitializedForUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        handleNotificationIntent(intent)

        val prefs = getSharedPreferences("ZegoPrefs", Context.MODE_PRIVATE)

        val savedUserId = prefs
            .getString("userId", null)
            ?.trim()

        val initialRoute =
            if (!savedUserId.isNullOrBlank()) {
                NavRoutes.DASHBOARD
            } else {
                NavRoutes.LOGIN
            }

        /*
         * Compose UI is established first.
         * Authentication observation and Zego initialization do not
         * participate in the initial Dashboard rendering path.
         */
        setContent {

            val userSettings by settingsRepository
                .getUserSettingsFlow()
                .collectAsState(
                    initial = UserSettings()
                )

            val isDarkTheme = when (userSettings.theme.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            KinChatTheme(
                darkTheme = isDarkTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    var showBatteryDialog by remember {
                        mutableStateOf(
                            BatteryOptimizationHelper
                                .shouldShowPrompt(this@MainActivity)
                        )
                    }

                    if (showBatteryDialog) {
                        BatteryOptimizationDialog(
                            onDismiss = {
                                BatteryOptimizationHelper
                                    .markPromptShown(this@MainActivity)

                                showBatteryDialog = false
                            },
                            onConfirm = {
                                BatteryOptimizationHelper
                                    .markPromptShown(this@MainActivity)

                                showBatteryDialog = false

                                BatteryOptimizationHelper
                                    .requestIgnoreBatteryOptimizations(
                                        this@MainActivity
                                    )
                            }
                        )
                    }

                    NotificationPermissionEffect()

                    val navController = rememberNavController()

                    val targetChatId by pendingChatId
                        .collectAsState(initial = null)

                    LaunchedEffect(targetChatId) {
                        val id = targetChatId ?: return@LaunchedEffect

                        try {
                            navController.navigate("chat/$id")
                        } catch (e: Exception) {
                            AppLogger.e(
                                "Navigation",
                                "Failed to navigate to chat: ${e.message}",
                                e
                            )
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

        /*
         * Authentication state is observed off the main thread.
         *
         * Zego initialization is deferred with decorView.post so the
         * initial UI gets a chance to render before the call SDK starts.
         */
        lifecycleScope.launch(Dispatchers.IO) {

            authRepository
                .observeAuthState()
                .collect { state ->

                    when (state) {

                        is AppAuthState.Authenticated -> {

                            val userId = state.userId.trim()

                            if (userId.isBlank()) {
                                return@collect
                            }

                            prefs.edit()
                                .putString("userId", userId)
                                .putString("userName", state.userName)
                                .apply()

                            pendingSyncCoordinator.triggerSync()

                            if (zegoInitializedForUserId == userId) {
                                return@collect
                            }

                            zegoInitializedForUserId = userId

                            window.decorView.post {

                                if (isFinishing || isDestroyed) {
                                    return@post
                                }

                                try {
                                    AppLogger.d(
                                        "ZegoCloud",
                                        "Initializing ZegoCloud after initial UI scheduling"
                                    )

                                    (application as KinChatApplication)
                                        .initZegoCloud(
                                            userId = userId,
                                            userName = state.userName
                                        )

                                } catch (e: Exception) {
                                    AppLogger.e(
                                        "ZegoCloud",
                                        "Deferred Zego initialization failed: ${e.message}",
                                        e
                                    )
                                }
                            }
                        }

                        is AppAuthState.Unauthenticated -> {

                            prefs.edit()
                                .clear()
                                .apply()

                            zegoInitializedForUserId = null

                            launch(Dispatchers.Main) {
                                try {
                                    ZegoUIKitPrebuiltCallService.unInit()
                                } catch (e: Exception) {
                                    AppLogger.e(
                                        "ZegoCloud",
                                        "ZegoCloud unInit failed: ${e.message}",
                                        e
                                    )
                                }
                            }
                        }

                        else -> {
                            // Unknown auth state: no action.
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
        val chatId = intent
            ?.getStringExtra("chat_id")
            ?.trim()

        if (!chatId.isNullOrBlank()) {
            pendingChatId.value = chatId
        }
    }
}
