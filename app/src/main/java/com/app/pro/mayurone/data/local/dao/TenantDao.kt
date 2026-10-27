package com.app.pro.mayurone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.pro.mayurone.data.local.entity.TenantEntity
import com.app.pro.mayurone.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {

    @Query("SELECT * FROM tenants LIMIT 1")
    fun getActiveTenantFlow(): Flow<TenantEntity?>

    @Query("SELECT * FROM tenants WHERE tenantId = :tenantId")
    suspend fun getTenantById(tenantId: String): TenantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: TenantEntity)

    @Update
    suspend fun updateTenant(tenant: TenantEntity)

    @Query("SELECT * FROM users WHERE tenantId = :tenantId")
    fun getUsersByTenantFlow(tenantId: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}
