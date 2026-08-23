package com.kinchat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cloudinary.android.MediaManager
import com.kinchat.app.core.logging.AppLogger
import com.kinchat.app.data.repository.chat.sync.PendingSyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class KinChatApplication : Application(), Configuration.Provider {

    companion object {
        lateinit var instance: KinChatApplication
            private set
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var pendingSyncCoordinator: PendingSyncCoordinator

    private val ONESIGNAL_APP_ID = "c3b6c28c-fdf0-4eb1-be03-e02f2057628d"

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppLogger.init()

        // --- OneSignal Initialization ---
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
        
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(true)
        }
        // --------------------------------

        pendingSyncCoordinator.startMonitoring()
        pendingSyncCoordinator.triggerSync()

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

        val prefs = getSharedPreferences("ZegoPrefs", Context.MODE_PRIVATE)
        val savedUserId = prefs.getString("userId", null)
        val savedUserName = prefs.getString("userName", null)

        if (!savedUserId.isNullOrBlank() && !savedUserName.isNullOrBlank()) {
            AppLogger.d("ZegoCloud", "🚀 Auto-initializing ZegoCloud in Application for offline calls")
            initZegoCloud(savedUserId, savedUserName)
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
