package com.kinchat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE chatId = :chatId AND userId = :userId")
    suspend fun getDraft(chatId: String, userId: String): DraftEntity?

    @Query("DELETE FROM drafts WHERE chatId = :chatId AND userId = :userId")
    suspend fun deleteDraft(chatId: String, userId: String)
}
