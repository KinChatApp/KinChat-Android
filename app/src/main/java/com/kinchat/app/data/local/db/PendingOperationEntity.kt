package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val type: OperationType,
    val referenceId: String,
    val payloadJson: String?,
    val createdAt: Long,
    val retryCount: Int = 0
) {
    fun incrementRetryCount(): PendingOperationEntity {
        return this.copy(retryCount = this.retryCount + 1)
    }
}
