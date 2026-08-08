package com.kinchat.app.core.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

// 🚀 Vivo/Funtouch/OriginOS-এর মতো OEM এ battery optimization + autostart
// পারমিশন না থাকলে FCM data message process wake করাতে ব্যর্থ হয় (আমাদের
// লগে দেখা GCM broadcast CANCELLED সমস্যাটা ঠিক এই কারণেই)। এই হেল্পার
// ইউজারকে সেই সেটিংস পেজে গাইড করে।
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"
    private const val PREFS_NAME = "battery_opt_prefs"
    private const val KEY_LAST_PROMPTED = "last_prompted_at"
    private const val REPROMPT_INTERVAL_MS = 3 * 24 * 60 * 60 * 1000L // ৩ দিন

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun shouldShowPrompt(context: Context): Boolean {
        if (isIgnoringBatteryOptimizations(context)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShown = prefs.getLong(KEY_LAST_PROMPTED, 0L)
        return System.currentTimeMillis() - lastShown > REPROMPT_INTERVAL_MS
    }

    fun markPromptShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_PROMPTED, System.currentTimeMillis()).apply()
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Direct battery optimization intent failed, falling back", e)
            openAppSettingsFallback(context)
        }
    }

    // Vivo-র autostart manager activity নাম Funtouch/OriginOS ভার্সন ভেদে
    // পাল্টায় এবং এই ডিভাইসে (Android 16 / OriginOS-ভিত্তিক) verify করা
    // যায়নি, তাই best-effort হিসেবে চেষ্টা করা হচ্ছে — ব্যর্থ হলে সরাসরি
    // App Info পেজে নিয়ে যাওয়া হবে, ইউজার সেখান থেকে ম্যানুয়ালি করবে।
    fun openAutoStartSettings(context: Context) {
        val candidates = listOf(
            Intent().setClassName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            ),
            Intent().setClassName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            ),
            Intent("com.iqoo.secure.MainActivity")
        )

        for (intent in candidates) {
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // পরের candidate ট্রাই করো
            }
        }

        Log.w(TAG, "No known Vivo autostart activity resolved, falling back to app settings")
        openAppSettingsFallback(context)
    }

    private fun openAppSettingsFallback(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "App settings fallback also failed", e)
        }
    }
}
