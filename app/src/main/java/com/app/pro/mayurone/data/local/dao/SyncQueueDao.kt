package com.app.pro.mayurone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.pro.mayurone.data.local.entity.SyncQueueEntity
import com.app.pro.mayurone.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue WHERE tenantId = :tenantId ORDER BY createdAt ASC")
    fun getSyncQueueFlow(tenantId: String): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE tenantId = :tenantId AND syncStatus = 'PENDING'")
    fun getPendingSyncCountFlow(tenantId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOperation(operation: SyncQueueEntity)

    @Update
    suspend fun updateOperation(operation: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE operationId = :operationId")
    suspend fun deleteOperation(operationId: String)
}
