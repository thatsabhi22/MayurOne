package com.app.pro.mayurone.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index(value = ["tenantId"])]
)
data class CustomerEntity(
    @PrimaryKey val customerId: String,
    val tenantId: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
