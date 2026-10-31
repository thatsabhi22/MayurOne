package com.app.pro.mayurone.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.app.pro.mayurone.data.local.model.ShareChannel

@Entity(
    tableName = "share_events",
    indices = [Index(value = ["estimateId"]), Index(value = ["revisionId"])]
)
data class ShareEventEntity(
    @PrimaryKey val shareId: String,
    val tenantId: String,
    val estimateId: String,
    val revisionId: String,
    val channel: ShareChannel,
    val sharedAt: Long = System.currentTimeMillis()
)
