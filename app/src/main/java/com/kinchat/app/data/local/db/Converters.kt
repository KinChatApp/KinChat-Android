package com.kinchat.app.data.local.db

import androidx.room.TypeConverter

enum class UploadState { PENDING, UPLOADING, UPLOADED, FAILED }
enum class OperationType { SEND_MESSAGE, EDIT_MESSAGE, DELETE_MESSAGE, ADD_REACTION, REMOVE_REACTION, UPLOAD_ATTACHMENT }
enum class ReactionType { like, love, laugh, wow, sad, pray } // Matches Supabase ENUM

class Converters {
    @TypeConverter fun fromMessageStatus(value: MessageStatus) = value.name
    @TypeConverter fun toMessageStatus(value: String) = enumValueOf<MessageStatus>(value)
    
    @TypeConverter fun fromMessageType(value: MessageType) = value.name
    @TypeConverter fun toMessageType(value: String) = enumValueOf<MessageType>(value)

    @TypeConverter fun fromUploadState(value: UploadState) = value.name
    @TypeConverter fun toUploadState(value: String) = enumValueOf<UploadState>(value)

    @TypeConverter fun fromOperationType(value: OperationType) = value.name
    @TypeConverter fun toOperationType(value: String) = enumValueOf<OperationType>(value)

    @TypeConverter fun fromReactionType(value: ReactionType) = value.name
    @TypeConverter fun toReactionType(value: String) = enumValueOf<ReactionType>(value)
}
