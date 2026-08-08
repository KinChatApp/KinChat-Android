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
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KinChatApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppLogger.init() // Initialize the global developer logger
        
        // 🚀 Initialize Cloudinary (Replaced ImageKit)
        try {
            val config = HashMap<String, String>()
            // ⚠️ strings.xml এ আপনার Cloudinary Cloud Name টি অ্যাড করে নিতে হবে
            config["cloud_name"] = getString(R.string.cloudinary_cloud_name)
            config["secure"] = "true"
            MediaManager.init(this, config)
            AppLogger.d("KinChatApp", "✅ Cloudinary initialized successfully")
        } catch (e: Exception) {
            // MediaManager.init() একাধিকবার কল হলে যেন ক্র্যাশ না করে তাই Try-Catch দেওয়া হলো
            AppLogger.e("KinChatApp", "⚠️ Cloudinary init exception: ${e.message}")
        }
                                                                                            
        createNotificationChannels()                                                                     
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Messages Channel (High Priority) ⭐⭐⭐⭐⭐
            val msgChannel = NotificationChannel(
                "kinchat_messages_channel",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                setShowBadge(true)
            }

            // 2. Calls Channel (High Priority)
            val callChannel = NotificationChannel(
                "kinchat_calls_channel",
                "Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming audio and video calls"
            }
            // 3. System/Silent Channel (Low Priority)
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
