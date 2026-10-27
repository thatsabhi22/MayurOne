package com.app.pro.mayurone.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.app.pro.mayurone.data.local.model.SyncStatus

@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["tenantId"]), Index(value = ["syncStatus"])]
)
data class SyncQueueEntity(
    @PrimaryKey val operationId: String,
    val tenantId: String,
    val entityType: String,
    val operationType: String,
    val payloadJson: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
