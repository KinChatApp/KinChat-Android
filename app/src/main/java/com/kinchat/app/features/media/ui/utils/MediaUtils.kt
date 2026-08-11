package com.kinchat.app.features.media.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.kinchat.app.features.media.ui.MediaPickerViewModel
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

fun openAppSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        )
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
    }
}

fun launchCamera(
    context: Context,
    viewModel: MediaPickerViewModel,
    launcher: androidx.activity.result.ActivityResultLauncher<Uri>
) {
    val uri = createCameraFileUri(context)
    viewModel.setCameraUri(uri)
    launcher.launch(uri)
}

fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun createCameraFileUri(context: Context): Uri {
    val folder = File(context.cacheDir, "camera_images").apply { mkdirs() }
    val file = File(folder, "IMG_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
