package com.kinchat.app.core.notifications.builder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.withTimeoutOrNull

class NotificationImageHelper(context: Context) {

    private val appContext = context.applicationContext

    // Pre-allocated objects for defaults (safe to share as they are not mutated)
    private val initialBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(BRAND_COLOR)
    }

    private val initialTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 64f
        textAlign = Paint.Align.CENTER
    }

    private val xfermodeSrcIn = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    suspend fun loadAvatarOrInitial(url: String?, name: String): IconCompat {
        if (!url.isNullOrEmpty()) {
            val bitmap = try {
                // 🚀 FIX (RC6): Reduced timeout from 1500ms to 800ms to avoid degrading custom UI
                withTimeoutOrNull(AVATAR_LOAD_TIMEOUT_MS) {
                    val request = ImageRequest.Builder(appContext)
                        .data(url)
                        .allowHardware(false) // Hardware bitmaps crash Canvas operations
                        .size(TARGET_SIZE, TARGET_SIZE)
                        .build()
                    val result = appContext.imageLoader.execute(request)
                    (result.drawable as? BitmapDrawable)?.bitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "Avatar load failed for url=$url", e)
                null
            }

            if (bitmap != null && !bitmap.isRecycled) {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, TARGET_SIZE, TARGET_SIZE, true)
                return IconCompat.createWithBitmap(getCircleBitmap(scaledBitmap))
            }
        }

        val initial = if (name.isNotEmpty()) name.take(1).uppercase() else "?"
        return IconCompat.createWithBitmap(generateInitialBitmap(initial))
    }

    private fun getCircleBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val rect = Rect(0, 0, size, size)

        // 🚀 FIX (P9): Local Paint instance. 
        // Shared mutable Paint/xfermode causes thread-safety issues during concurrent notifications.
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        circlePaint.xfermode = xfermodeSrcIn
        canvas.drawBitmap(bitmap, rect, rect, circlePaint)

        return output
    }

    private fun generateInitialBitmap(initial: String): Bitmap {
        val bitmap = Bitmap.createBitmap(TARGET_SIZE, TARGET_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawCircle(TARGET_SIZE / 2f, TARGET_SIZE / 2f, TARGET_SIZE / 2f, initialBgPaint)

        val yPos = (canvas.height / 2f) - ((initialTextPaint.descent() + initialTextPaint.ascent()) / 2f)
        canvas.drawText(initial, TARGET_SIZE / 2f, yPos, initialTextPaint)

        return bitmap
    }

    companion object {
        private const val TAG = "NotificationImageHelper"
        private const val TARGET_SIZE = 128
        // 🚀 FIX: Reduced timeout to prevent triggering fallback notification
        private const val AVATAR_LOAD_TIMEOUT_MS = 800L
        private const val BRAND_COLOR = "#4CAF50"
    }
}
