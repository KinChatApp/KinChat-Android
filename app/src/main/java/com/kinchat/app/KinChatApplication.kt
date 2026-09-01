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

    private val ONESIGNAL_APP_ID =
        "c3b6c28c-fdf0-4eb1-be03-e02f2057628d"

    private var deferredServicesInitialized = false

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        instance = this
        AppLogger.init()

        /*
         * IMPORTANT:
         * Keep Application startup as lightweight as possible.
         *
         * The following services are intentionally NOT initialized here:
         * - OneSignal
         * - Cloudinary
         * - Pending sync monitoring
         * - Initial sync
         * - Notification channels
         *
         * They are initialized later from MainActivity after the first UI
         * frame has had a chance to render.
         *
         * ZegoCloud is also initialized later after authentication.
         */
    }

    /**
     * Initializes non-critical application services after the initial UI
     * has started rendering.
     *
     * This function is intentionally safe to call more than once.
     */
    fun initializeDeferredServices() {
        if (deferredServicesInitialized) return
        deferredServicesInitialized = true

        // --- OneSignal ---
        try {
            OneSignal.Debug.logLevel = LogLevel.VERBOSE
            OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

            AppLogger.d(
                "KinChatStartup",
                "✅ OneSignal initialized (deferred)"
            )
        } catch (e: Exception) {
            AppLogger.e(
                "KinChatStartup",
                "⚠️ OneSignal deferred initialization failed: ${e.message}",
                e
            )
        }

        // --- Cloudinary ---
        try {
            val config = HashMap<String, String>().apply {
                this["cloud_name"] = getString(R.string.cloudinary_cloud_name)
                this["secure"] = "true"
            }

            MediaManager.init(this, config)

            AppLogger.d(
                "KinChatStartup",
                "✅ Cloudinary initialized (deferred)"
            )
        } catch (e: Exception) {
            AppLogger.e(
                "KinChatStartup",
                "⚠️ Cloudinary deferred initialization failed: ${e.message}",
                e
            )
        }

        // --- Notification Channels ---
        try {
            createNotificationChannels()

            AppLogger.d(
                "KinChatStartup",
                "✅ Notification channels initialized (deferred)"
            )
        } catch (e: Exception) {
            AppLogger.e(
                "KinChatStartup",
                "⚠️ Notification channel initialization failed: ${e.message}",
                e
            )
        }

        /*
         * PendingSyncCoordinator is intentionally NOT initialized here.
         *
         * MainActivity will start:
         *   pendingSyncCoordinator.startMonitoring()
         *   pendingSyncCoordinator.triggerSync()
         *
         * after the first UI frame.
         */
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
