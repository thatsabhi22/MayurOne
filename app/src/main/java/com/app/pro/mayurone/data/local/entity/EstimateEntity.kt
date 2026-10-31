package com.app.pro.mayurone.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.app.pro.mayurone.data.local.model.EstimateStatus

@Entity(
    tableName = "estimates",
    indices = [Index(value = ["tenantId", "estimateNumber"], unique = true)]
)
data class EstimateEntity(
    @PrimaryKey val estimateId: String,
    val tenantId: String,
    val estimateNumber: String,
    val status: EstimateStatus,
    val currentRevisionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
