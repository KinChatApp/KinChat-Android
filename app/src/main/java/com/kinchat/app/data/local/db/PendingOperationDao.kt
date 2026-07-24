package com.kinchat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PendingOperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingOperationEntity)

    @Update
    suspend fun updateOperation(operation: PendingOperationEntity)

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAllPendingOperations(): List<PendingOperationEntity>

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteOperation(id: String)
}
