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

    // 🚀 FIX: Exclude DEAD operations and order by sequence for deterministic FIFO execution
    @Query("SELECT * FROM pending_operations WHERE status != 'DEAD' ORDER BY sequence ASC, createdAt ASC")
    suspend fun getAllPendingOperations(): List<PendingOperationEntity>

    // 🚀 FIX: Get next sequence number for maintaining order
    @Query("SELECT IFNULL(MAX(sequence), 0) + 1 FROM pending_operations")
    suspend fun getNextSequence(): Long

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteOperation(id: String)
}
