package com.app.pro.mayurone.domain

import com.app.pro.mayurone.data.local.model.CalculationBasis
import java.math.BigDecimal
import java.math.RoundingMode

object CalculationEngine {

    fun calculateItemAmount(
        basis: CalculationBasis,
        quantity: Double,
        weight: Double,
        rate: Double
    ): Double {
        val qty = BigDecimal.valueOf(quantity.coerceAtLeast(0.0))
        val wt = BigDecimal.valueOf(weight.coerceAtLeast(0.0))
        val r = BigDecimal.valueOf(rate.coerceAtLeast(0.0))

        val amount = when (basis) {
            CalculationBasis.QTY_X_RATE -> qty.multiply(r)
            CalculationBasis.WEIGHT_X_RATE -> wt.multiply(r)
        }

        return amount.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun calculateTotal(itemAmounts: List<Double>): Double {
        var sum = BigDecimal.ZERO
        for (amount in itemAmounts) {
            sum = sum.add(BigDecimal.valueOf(amount.coerceAtLeast(0.0)))
        }
        return sum.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun formatCurrency(amount: Double, currencySymbol: String = "₹"): String {
        val rounded = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
        return "$currencySymbol $rounded"
    }
}
