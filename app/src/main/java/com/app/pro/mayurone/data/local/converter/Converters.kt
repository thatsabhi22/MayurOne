package com.app.pro.mayurone.data.local.converter

import androidx.room.TypeConverter
import com.app.pro.mayurone.data.local.model.CalculationBasis
import com.app.pro.mayurone.data.local.model.EstimateStatus
import com.app.pro.mayurone.data.local.model.ShareChannel
import com.app.pro.mayurone.data.local.model.SyncStatus
import com.app.pro.mayurone.data.local.model.UserRole

class Converters {

    @TypeConverter
    fun fromCalculationBasis(basis: CalculationBasis): String = basis.name

    @TypeConverter
    fun toCalculationBasis(value: String): CalculationBasis =
        runCatching { CalculationBasis.valueOf(value) }.getOrDefault(CalculationBasis.QTY_X_RATE)

    @TypeConverter
    fun fromEstimateStatus(status: EstimateStatus): String = status.name

    @TypeConverter
    fun toEstimateStatus(value: String): EstimateStatus =
        runCatching { EstimateStatus.valueOf(value) }.getOrDefault(EstimateStatus.DRAFT)

    @TypeConverter
    fun fromShareChannel(channel: ShareChannel): String = channel.name

    @TypeConverter
    fun toShareChannel(value: String): ShareChannel =
        runCatching { ShareChannel.valueOf(value) }.getOrDefault(ShareChannel.SYSTEM_SHARE)

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus =
        runCatching { SyncStatus.valueOf(value) }.getOrDefault(SyncStatus.PENDING)

    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole =
        runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.STAFF)
}
