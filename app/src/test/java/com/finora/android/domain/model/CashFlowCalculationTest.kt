package com.finora.android.domain.model

import com.finora.android.core.model.Amount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CashFlowCalculationTest {

    @Test
    fun testPositiveCashFlowAndSavingsRate() {
        val income = Amount.fromMajor(50000) // ₹50,000.00
        val expenses = Amount.fromMajor(35000) // ₹35,000.00

        val summary = CashFlowCalculator.calculate(income, expenses)

        assertTrue(summary.isPositive)
        assertEquals(1500000L, summary.netCashFlow.minorUnits) // +₹15,000.00
        assertEquals(30f, summary.savingsRatePercentage, 0.01f) // (15,000 / 50,000) * 100 = 30%
    }

    @Test
    fun testNegativeCashFlowZeroSavingsRate() {
        val income = Amount.fromMajor(20000) // ₹20,000.00
        val expenses = Amount.fromMajor(25000) // ₹25,000.00

        val summary = CashFlowCalculator.calculate(income, expenses)

        assertFalse(summary.isPositive)
        assertEquals(-500000L, summary.netCashFlow.minorUnits) // -₹5,000.00
        assertEquals(0f, summary.savingsRatePercentage, 0.01f)
    }

    @Test
    fun testZeroIncome() {
        val income = Amount.ZERO
        val expenses = Amount.fromMajor(1000)

        val summary = CashFlowCalculator.calculate(income, expenses)

        assertFalse(summary.isPositive)
        assertEquals(-100000L, summary.netCashFlow.minorUnits)
        assertEquals(0f, summary.savingsRatePercentage, 0.01f)
    }

    @Test
    fun testExactZeroNet() {
        val income = Amount.fromMajor(10000)
        val expenses = Amount.fromMajor(10000)

        val summary = CashFlowCalculator.calculate(income, expenses)

        assertTrue(summary.isPositive)
        assertEquals(0L, summary.netCashFlow.minorUnits)
        assertEquals(0f, summary.savingsRatePercentage, 0.01f)
    }
}
