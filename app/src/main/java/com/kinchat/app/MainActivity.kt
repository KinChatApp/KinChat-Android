package com.kinchat.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /*
     * Lazy dependencies prevent unnecessary dependency initialization
     * during Activity startup.
     */
    @Inject
    lateinit var authRepository: Lazy<AuthRepository>

    @Inject
    lateinit var settingsRepository: Lazy<SettingsRepository>

    @Inject
    lateinit var pendingSyncCoordinator: Lazy<PendingSyncCoordinator>

    private val pendingChatId = MutableStateFlow<String?>(null)

    /*
     * Default settings are available immediately.
     * Actual settings are loaded after the UI has started.
     */
    private val userSettings = MutableStateFlow(UserSettings())

    /*
     * Prevent duplicate Zego initialization for the same user.
     */
    private var zegoInitializedForUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        AppLogger.startup(
            "MainActivity.onCreate START"
        )

        installSplashScreen()

        super.onCreate(savedInstanceState)

        AppLogger.startup(
            "MainActivity after super.onCreate"
        )

        handleNotificationIntent(intent)

        val prefs = getSharedPreferences(
            "ZegoPrefs",
            Context.MODE_PRIVATE
        )

        /*
         * Use locally saved user id only to decide the first route.
         * No Supabase authentication wait here.
         */
        val savedUserId = prefs
            .getString("userId", null)
            ?.trim()

        val initialRoute =
            if (!savedUserId.isNullOrBlank()) {
                NavRoutes.DASHBOARD
            } else {
                NavRoutes.LOGIN
            }

        AppLogger.startup(
            "Initial route resolved: $initialRoute"
        )

        AppLogger.startup(
            "Before setContent"
        )

        setContent {

            /*
             * ============================================================
             * PHASE 1
             * ============================================================
             *
             * Render only a lightweight surface first.
             *
             * This allows Android's splash screen to finish quickly
             * without immediately composing Dashboard + navigation +
             * chat list + other feature trees.
             */
            var mainContentReady by remember {
                mutableStateOf(false)
            }

            /*
             * After the first lightweight composition is committed,
             * switch to the real application UI.
             */
            LaunchedEffect(Unit) {

                AppLogger.startup(
                    "LIGHTWEIGHT FIRST COMPOSITION"
                )

                mainContentReady = true

                AppLogger.startup(
                    "MAIN CONTENT ENABLED"
                )
            }

            KinChatTheme(
                darkTheme = if (mainContentReady) {
                    when (userSettings.value.theme.lowercase()) {
                        "dark" -> true
                        "light" -> false
                        else -> isSystemInDarkTheme()
                    }
                } else {
                    isSystemInDarkTheme()
                }
            ) {

                /*
                 * PHASE 1: lightweight first frame.
                 */
                if (!mainContentReady) {

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                } else {

                    /*
                     * ====================================================
                     * PHASE 2
                     * ====================================================
                     *
                     * Full application UI starts only after the first
                     * lightweight frame has been committed.
                     */

                    val currentUserSettings by userSettings
                        .collectAsState()

                    val isDarkTheme = when (
                        currentUserSettings.theme.lowercase()
                    ) {
                        "dark" -> true
                        "light" -> false
                        else -> isSystemInDarkTheme()
                    }

                    /*
                     * The theme may change after real settings arrive.
                     */
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
                                        .shouldShowPrompt(
                                            this@MainActivity
                                        )
                                )
                            }

                            if (showBatteryDialog) {
                                BatteryOptimizationDialog(
                                    onDismiss = {
                                        BatteryOptimizationHelper
                                            .markPromptShown(
                                                this@MainActivity
                                            )

                                        showBatteryDialog = false
                                    },
                                    onConfirm = {
                                        BatteryOptimizationHelper
                                            .markPromptShown(
                                                this@MainActivity
                                            )

                                        showBatteryDialog = false

                                        BatteryOptimizationHelper
                                            .requestIgnoreBatteryOptimizations(
                                                this@MainActivity
                                            )
                                    }
                                )
                            }

                            NotificationPermissionEffect()

                            val navController =
                                rememberNavController()

                            val targetChatId by pendingChatId
                                .collectAsState(
                                    initial = null
                                )

                            LaunchedEffect(targetChatId) {

                                val id =
                                    targetChatId
                                        ?: return@LaunchedEffect

                                try {
                                    navController.navigate(
                                        "chat/$id"
                                    )
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
            }
        }

        AppLogger.startup(
            "After setContent"
        )

        AppLogger.startup(
            "MainActivity.onCreate END"
        )

        /*
         * ================================================================
         * DEFERRED STARTUP
         * ================================================================
         *
         * These services do not participate in the initial frame:
         *
         * - OneSignal
         * - Cloudinary
         * - Notification channels
         * - Settings repository
         * - Sync monitoring
         * - Authentication observer
         * - Zego initialization
         */
        window.decorView.post {

            if (isFinishing || isDestroyed) {
                AppLogger.startup(
                    "Deferred startup cancelled: Activity destroyed"
                )
                return@post
            }

            AppLogger.startup(
                "Deferred callback START"
            )

            /*
             * Deferred application services.
             */
            try {

                AppLogger.startup(
                    "Before initializeDeferredServices"
                )

                (application as KinChatApplication)
                    .initializeDeferredServices()

                AppLogger.startup(
                    "After initializeDeferredServices"
                )

            } catch (e: Exception) {

                AppLogger.e(
                    "KinChatStartup",
                    "Deferred application initialization failed: ${e.message}",
                    e
                )
            }

            /*
             * Settings observation.
             */
            lifecycleScope.launch(Dispatchers.IO) {

                AppLogger.startup(
                    "Settings coroutine START"
                )

                try {

                    settingsRepository
                        .get()
                        .getUserSettingsFlow()
                        .collect { settings ->
                            userSettings.value = settings
                        }

                } catch (e: Exception) {

                    AppLogger.e(
                        "StartupSettings",
                        "Failed to observe user settings: ${e.message}",
                        e
                    )
                }
            }

            /*
             * Authentication + sync.
             */
            lifecycleScope.launch(Dispatchers.IO) {

                AppLogger.startup(
                    "Auth/Sync coroutine START"
                )

                try {

                    AppLogger.startup(
                        "Before pendingSyncCoordinator.startMonitoring"
                    )

                    pendingSyncCoordinator
                        .get()
                        .startMonitoring()

                    AppLogger.startup(
                        "After pendingSyncCoordinator.startMonitoring"
                    )

                    AppLogger.startup(
                        "Before pendingSyncCoordinator.triggerSync"
                    )

                    pendingSyncCoordinator
                        .get()
                        .triggerSync()

                    AppLogger.startup(
                        "After pendingSyncCoordinator.triggerSync"
                    )

                } catch (e: Exception) {

                    AppLogger.e(
                        "StartupSync",
                        "Failed to start deferred sync: ${e.message}",
                        e
                    )
                }

                try {

                    AppLogger.startup(
                        "Before observeAuthState"
                    )

                    authRepository
                        .get()
                        .observeAuthState()
                        .collect { state ->

                            when (state) {

                                is AppAuthState.Authenticated -> {

                                    val userId =
                                        state.userId.trim()

                                    if (userId.isBlank()) {
                                        return@collect
                                    }

                                    prefs.edit()
                                        .putString(
                                            "userId",
                                            userId
                                        )
                                        .putString(
                                            "userName",
                                            state.userName
                                        )
                                        .apply()

                                    /*
                                     * Prevent duplicate Zego initialization.
                                     */
                                    if (
                                        zegoInitializedForUserId ==
                                        userId
                                    ) {
                                        return@collect
                                    }

                                    zegoInitializedForUserId =
                                        userId

                                    launch(Dispatchers.Main) {

                                        if (
                                            isFinishing ||
                                            isDestroyed
                                        ) {
                                            return@launch
                                        }

                                        try {

                                            AppLogger.startup(
                                                "Zego initialization START"
                                            )

                                            (application as KinChatApplication)
                                                .initZegoCloud(
                                                    userId = userId,
                                                    userName = state.userName
                                                )

                                            AppLogger.startup(
                                                "Zego initialization DONE"
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
                                            ZegoUIKitPrebuiltCallService
                                                .unInit()
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

                } catch (e: Exception) {

                    AppLogger.e(
                        "StartupAuth",
                        "Failed to observe authentication state: ${e.message}",
                        e
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {

        super.onNewIntent(intent)

        setIntent(intent)

        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(
        intent: Intent?
    ) {

        val chatId = intent
            ?.getStringExtra("chat_id")
            ?.trim()

        if (!chatId.isNullOrBlank()) {
            pendingChatId.value = chatId
        }
    }
}
