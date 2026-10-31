package com.app.pro.mayurone.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenants")
data class TenantEntity(
    @PrimaryKey val tenantId: String,
    val businessName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val logoUri: String? = null,
    val currencySymbol: String = "₹",
    val createdAt: Long = System.currentTimeMillis()
)
