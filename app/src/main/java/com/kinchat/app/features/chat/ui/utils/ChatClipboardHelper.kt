package com.kinchat.app.features.chat.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ChatClipboardHelper {
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Copied Messages", text))
    }
}
