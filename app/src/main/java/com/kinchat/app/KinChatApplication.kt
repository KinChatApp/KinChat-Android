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
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

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

    private val ONESIGNAL_APP_ID =
        "c3b6c28c-fdf0-4eb1-be03-e02f2057628d"

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
        // --------------------------------

        // Background sync monitoring.
        // This is independent from Dashboard UI rendering.
        pendingSyncCoordinator.startMonitoring()
        pendingSyncCoordinator.triggerSync()

        // --- Cloudinary Initialization ---
        try {
            val config = HashMap<String, String>().apply {
                this["cloud_name"] = getString(R.string.cloudinary_cloud_name)
                this["secure"] = "true"
            }

            MediaManager.init(this, config)

            AppLogger.d(
                "KinChatApp",
                "✅ Cloudinary initialized successfully"
            )
        } catch (e: Exception) {
            AppLogger.e(
                "KinChatApp",
                "⚠️ Cloudinary init exception: ${e.message}"
            )
        }
        // --------------------------------

        createNotificationChannels()

        // IMPORTANT:
        // ZegoCloud is intentionally NOT initialized here.
        //
        // Previously it was initialized during Application startup
        // using saved credentials, which could add unnecessary work
        // to the app's critical startup path.
        //
        // ZegoCloud will instead be initialized after authentication
        // from MainActivity.
    }

    fun initZegoCloud(
        userId: String,
        userName: String
    ) {
        try {
            val appID: Long = BuildConfig.ZEGO_APP_ID
            val appSign: String = BuildConfig.ZEGO_APP_SIGN

            val callInvitationConfig =
                ZegoUIKitPrebuiltCallInvitationConfig()

            ZegoUIKitPrebuiltCallInvitationService.init(
                this,
                appID,
                appSign,
                userId,
                userName,
                callInvitationConfig
            )

            AppLogger.d(
                "ZegoCloud",
                "✅ ZegoCloud initialized for user=$userId"
            )
        } catch (e: Exception) {
            AppLogger.e(
                "ZegoCloud",
                "❌ ZegoCloud initialization failed: ${e.message}",
                e
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

            notificationManager.createNotificationChannels(
                listOf(
                    msgChannel,
                    callChannel,
                    systemChannel
                )
            )
        }
    }
}
