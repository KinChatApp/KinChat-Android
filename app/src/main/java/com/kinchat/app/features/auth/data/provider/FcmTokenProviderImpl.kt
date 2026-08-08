package com.kinchat.app.features.auth.data.provider

import com.google.firebase.messaging.FirebaseMessaging
import com.kinchat.app.features.auth.domain.provider.FcmTokenProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class FcmTokenProviderImpl @Inject constructor() : FcmTokenProvider {
    
    override suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resume(null)
            }
        }
    }
}
