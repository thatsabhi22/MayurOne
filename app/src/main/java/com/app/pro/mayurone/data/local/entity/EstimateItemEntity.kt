package com.app.pro.mayurone.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.app.pro.mayurone.data.local.model.CalculationBasis

@Entity(
    tableName = "estimate_items",
    indices = [Index(value = ["revisionId"]), Index(value = ["tenantId"])]
)
data class EstimateItemEntity(
    @PrimaryKey val itemId: String,
    val revisionId: String,
    val tenantId: String,
    val itemOrder: Int,
    val description: String,
    val quantity: Double = 1.0,
    val rate: Double = 0.0,
    val weight: Double = 0.0,
    val calculationBasis: CalculationBasis = CalculationBasis.QTY_X_RATE,
    val amount: Double = 0.0
)
