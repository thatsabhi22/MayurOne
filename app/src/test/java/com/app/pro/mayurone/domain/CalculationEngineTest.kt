package com.app.pro.mayurone.domain

import com.app.pro.mayurone.data.local.model.CalculationBasis
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationEngineTest {

    @Test
    fun calculateItemAmount_quantityBased() {
        val amount = CalculationEngine.calculateItemAmount(
            basis = CalculationBasis.QTY_X_RATE,
            quantity = 5.0,
            weight = 0.0,
            rate = 120.0
        )
        assertEquals(600.0, amount, 0.001)
    }

    @Test
    fun calculateItemAmount_weightBased() {
        val amount = CalculationEngine.calculateItemAmount(
            basis = CalculationBasis.WEIGHT_X_RATE,
            quantity = 1.0,
            weight = 42.5,
            rate = 80.0
        )
        assertEquals(3400.0, amount, 0.001)
    }

    @Test
    fun calculateItemAmount_weightBasedWithQuantityGreaterThanOne() {
        val amount = CalculationEngine.calculateItemAmount(
            basis = CalculationBasis.WEIGHT_X_RATE,
            quantity = 2.0,
            weight = 18.5,
            rate = 120.0
        )
        // 2 * 18.5 * 120 = 4440.0
        assertEquals(4440.0, amount, 0.001)
    }

    @Test
    fun calculateTotal_sumsItemsCorrectly() {
        val total = CalculationEngine.calculateTotal(listOf(600.0, 3400.0, 150.75))
        assertEquals(4150.75, total, 0.001)
    }

    @Test
    fun formatCurrency_formatsWithSymbol() {
        val formatted = CalculationEngine.formatCurrency(4150.75, "₹")
        assertEquals("₹ 4150.75", formatted)
    }
}
