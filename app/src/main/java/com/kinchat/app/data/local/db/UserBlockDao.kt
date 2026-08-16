package com.kinchat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserBlockDao {
    @Query("SELECT * FROM user_blocks WHERE blockerId = :blockerId AND blockedId = :blockedId LIMIT 1")
    suspend fun getBlockStatus(blockerId: String, blockedId: String): UserBlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: UserBlockEntity)

    @Query("DELETE FROM user_blocks WHERE blockerId = :blockerId AND blockedId = :blockedId")
    suspend fun deleteBlock(blockerId: String, blockedId: String)
}
