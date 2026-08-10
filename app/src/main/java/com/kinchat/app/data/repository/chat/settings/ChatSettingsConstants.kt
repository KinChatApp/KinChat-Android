package com.kinchat.app.data.repository.chat.settings

internal object ChatSettingsConstants {
    // Table Names
    const val TABLE_CHAT_PARTICIPANTS = "chat_participants"
    const val TABLE_USER_BLOCKS = "user_blocks"
    
    // Column Names
    const val COLUMN_CHAT_ID = "chat_id"
    const val COLUMN_USER_ID = "user_id"
    const val COLUMN_IS_FAVORITE = "is_favorite"
    const val COLUMN_BLOCKER_ID = "blocker_id"
    const val COLUMN_BLOCKED_ID = "blocked_id"
}
