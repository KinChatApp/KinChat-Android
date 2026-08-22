package com.kinchat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cloudinary.android.MediaManager
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.repository.chat.sync.PendingSyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
// OneSignal Imports
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@HiltAndroidApp
class KinChatApplication : Application(), Configuration.Provider {

    // 🚀 NEW: Notification Extension-এ Context ব্যবহার করার জন্য
    companion object {
        lateinit var instance: KinChatApplication
            private set
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 🚀 Added: Inject Sync Coordinator for Startup Flush & Network Monitoring
    @Inject
    lateinit var pendingSyncCoordinator: PendingSyncCoordinator

    // OneSignal specific variables
    private val ONESIGNAL_APP_ID = "c3b6c28c-fdf0-4eb1-be03-e02f2057628d"
    private val dialogShown = AtomicBoolean(false)
    private var pushSubscriptionObserver: IPushSubscriptionObserver? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // 🚀 NEW: Context instance সেট করা হলো
        instance = this

        AppLogger.init() // Initialize the global developer logger

        // --- OneSignal Initialization ---
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        // Setup observer to check subscription status
        setupPushSubscriptionObserver()
        // --------------------------------

        // 🚀 Phase 5: Start observing network changes and flush pending operations on startup
        pendingSyncCoordinator.startMonitoring()
        pendingSyncCoordinator.triggerSync()

        // Initialize Cloudinary
        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = getString(R.string.cloudinary_cloud_name)
            config["secure"] = "true"
            MediaManager.init(this, config)
            AppLogger.d("KinChatApp", "✅ Cloudinary initialized successfully")
        } catch (e: Exception) {
            AppLogger.e("KinChatApp", "⚠️ Cloudinary init exception: ${e.message}")
        }

        createNotificationChannels()

        // 🚀 FIX: Initialize ZegoCloud globally so it listens for offline calls
        val prefs = getSharedPreferences("ZegoPrefs", Context.MODE_PRIVATE)
        val savedUserId = prefs.getString("userId", null)
        val savedUserName = prefs.getString("userName", null)

        if (!savedUserId.isNullOrBlank() && !savedUserName.isNullOrBlank()) {
            AppLogger.d("ZegoCloud", "🚀 Auto-initializing ZegoCloud in Application for offline calls")
            initZegoCloud(savedUserId, savedUserName)
        }
    }

    private fun setupPushSubscriptionObserver() {
        val observer = object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                checkAndRequestPermission(state.current.id)
            }
        }
        pushSubscriptionObserver = observer
        OneSignal.User.pushSubscription.addObserver(observer)
        checkAndRequestPermission(OneSignal.User.pushSubscription.id)
    }

    private fun checkAndRequestPermission(subscriptionId: String?) {
        val isRegistered = !subscriptionId.isNullOrEmpty() && !subscriptionId.startsWith("local-")
        if (isRegistered && dialogShown.compareAndSet(false, true)) {
            // OneSignal SDK 5.x handles Android 13+ permission request internally when calling this
            CoroutineScope(Dispatchers.Main).launch {
                OneSignal.Notifications.requestPermission(true)
            }
        }
    }

    fun initZegoCloud(userId: String, userName: String) {
        val appID: Long = BuildConfig.ZEGO_APP_ID
        val appSign: String = BuildConfig.ZEGO_APP_SIGN

        val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()

        ZegoUIKitPrebuiltCallService.init(
            this,
            appID,
            appSign,
            userId,
            userName,
            callInvitationConfig
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val msgChannel = NotificationChannel(
                "kinchat_messages_channel",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                setShowBadge(true)
            }

            val callChannel = NotificationChannel(
                "kinchat_calls_channel",
                "Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming audio and video calls"
            }

            val systemChannel = NotificationChannel(
                "kinchat_system_channel",
                "System & Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background sync and system alerts"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(msgChannel, callChannel, systemChannel))
        }
    }
}
