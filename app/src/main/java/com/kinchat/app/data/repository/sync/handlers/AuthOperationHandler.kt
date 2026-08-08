package com.kinchat.app.data.repository.sync.handlers

import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
import com.kinchat.app.domain.repository.AuthRepository
import javax.inject.Inject

class AuthOperationHandler @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun handle(op: PendingOperationEntity) {
        if (op.type == OperationType.UPDATE_FCM_TOKEN) {
            val token = op.referenceId
            val result = authRepository.updateFcmToken(token)
            if (result.isFailure) {
                throw result.exceptionOrNull() ?: Exception("FCM token sync failed")
            }
        }
    }
}
