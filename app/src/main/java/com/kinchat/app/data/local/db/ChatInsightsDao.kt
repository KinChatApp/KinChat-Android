package com.kinchat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatInsightsDao {
    @Query("SELECT * FROM chat_insights WHERE friendId = :friendId")
    fun getInsightsFlow(friendId: String): Flow<ChatInsightsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ChatInsightsEntity)

    // 🚀 Delta Update: যখন নিজে মেসেজ সেন্ড করবেন, তখন শুধু ইনক্রিমেন্ট হবে (পুরো DB কুয়েরি হবে না)
    @Query("""
        UPDATE chat_insights 
        SET 
            totalMessages = totalMessages + 1,
            myMessages = myMessages + 1,
            myWords = myWords + :wordCount,
            myChars = myChars + :charCount,
            myLongest = CASE WHEN :charCount > myLongest THEN :charCount ELSE myLongest END,
            lastMessageAt = :timestamp
        WHERE chatId = :chatId
    """)
    suspend fun incrementMyMessageCount(
        chatId: String, 
        wordCount: Int, 
        charCount: Int, 
        timestamp: String
    )
}
