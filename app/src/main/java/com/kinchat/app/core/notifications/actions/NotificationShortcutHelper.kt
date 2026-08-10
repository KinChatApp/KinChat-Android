package com.kinchat.app.core.notifications.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

class NotificationShortcutHelper(context: Context) {
    private val appContext = context.applicationContext

    fun pushShortcutIfNeeded(
        chatId: String,
        senderName: String,
        senderIcon: IconCompat,
        contentIntent: Intent,
        senderPerson: Person
    ) {
        try {
            val alreadyUpToDate = ShortcutManagerCompat.getDynamicShortcuts(appContext)
                .any { it.id == chatId && it.shortLabel == senderName }
            if (alreadyUpToDate) return

            val shortcut = ShortcutInfoCompat.Builder(appContext, chatId)
                .setShortLabel(senderName)
                .setLongLabel(senderName)
                .setIcon(senderIcon)
                .setIntent(contentIntent)
                .setLongLived(true)
                .setPerson(senderPerson)
                .build()
                
            ShortcutManagerCompat.pushDynamicShortcut(appContext, shortcut)
        } catch (e: Exception) {
            Log.e(TAG, "Shortcut push failed for chatId=$chatId", e)
        }
    }

    companion object {
        private const val TAG = "NotificationShortcut"
    }
}
