package com.kinchat.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val fileUrl: String?,
    
    // Metadata for UI
    val mimeType: String?,
    val fileName: String?,
    val fileSize: Long?,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null, // For video/audio
    val thumbnailUrl: String? = null,
    
    val createdAt: Long,
    
    // Offline-First Fields
    val localUri: String? = null,
    val uploadState: UploadState = UploadState.PENDING
)
