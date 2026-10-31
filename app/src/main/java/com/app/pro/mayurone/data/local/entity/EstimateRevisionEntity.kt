package com.app.pro.mayurone.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "estimate_revisions",
    indices = [Index(value = ["estimateId"]), Index(value = ["tenantId"])]
)
data class EstimateRevisionEntity(
    @PrimaryKey val revisionId: String,
    val estimateId: String,
    val tenantId: String,
    val revisionNumber: Int,
    val parentRevisionId: String? = null,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val notes: String = "",
    val totalAmount: Double = 0.0,
    val createdBy: String = "Owner",
    val createdAt: Long = System.currentTimeMillis()
)
