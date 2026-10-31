package com.app.pro.mayurone.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.pro.mayurone.data.local.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val tenantId: String,
    val displayName: String,
    val phone: String,
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis()
)
