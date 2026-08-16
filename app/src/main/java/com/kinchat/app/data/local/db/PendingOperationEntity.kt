package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_operations",
    // 🚀 FIX: Added index for status and sequence for optimized ordering and querying
    indices = [Index(value = ["status", "sequence"])]
)
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val type: OperationType,
    val referenceId: String,
    val payloadJson: String?,
    val createdAt: Long,
    val retryCount: Int = 0,
    // 🚀 FIX: New fields for Outbox Hardening (matching Phase 1 migration)
    val attempt: Int = 0,
    val maxAttempts: Int = 5,
    val lastError: String? = null,
    val status: String = "PENDING", // PENDING, FAILED, DEAD
    val sequence: Long = 0
) {
    fun incrementAttempt(errorMsg: String?): PendingOperationEntity {
        val nextAttempt = this.attempt + 1
        // Move to DEAD state if max attempts reached, otherwise mark as FAILED (ready for retry)
        val nextStatus = if (nextAttempt >= this.maxAttempts) "DEAD" else "FAILED"
        return this.copy(
            attempt = nextAttempt,
            retryCount = this.retryCount + 1,
            lastError = errorMsg,
            status = nextStatus
        )
    }
}
