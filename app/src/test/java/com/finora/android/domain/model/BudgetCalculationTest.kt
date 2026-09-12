package com.finora.android.domain.model

import com.finora.android.core.model.Amount
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCalculationTest {

    @Test
    fun testBudgetOnTrack() {
        val budget = Amount.fromMajor(10000)
        val spent = Amount.fromMajor(5000) // 50%

        val summary = BudgetCalculator.calculate(budget, spent)

        assertEquals(BudgetStatus.ON_TRACK, summary.status)
        assertEquals(50f, summary.percentageUsed, 0.01f)
        assertEquals(500000L, summary.remainingAmount.minorUnits)
        assertEquals(0L, summary.overBudgetAmount.minorUnits)
    }

    @Test
    fun testBudgetNearLimit() {
        val budget = Amount.fromMajor(10000)
        val spent = Amount.fromMajor(8500) // 85%

        val summary = BudgetCalculator.calculate(budget, spent)

        assertEquals(BudgetStatus.NEAR_LIMIT, summary.status)
        assertEquals(85f, summary.percentageUsed, 0.01f)
        assertEquals(150000L, summary.remainingAmount.minorUnits)
        assertEquals(0L, summary.overBudgetAmount.minorUnits)
    }

    @Test
    fun testBudgetOverBudget() {
        val budget = Amount.fromMajor(10000)
        val spent = Amount.fromMajor(12500) // 125%

        val summary = BudgetCalculator.calculate(budget, spent)

        assertEquals(BudgetStatus.OVER_BUDGET, summary.status)
        assertEquals(125f, summary.percentageUsed, 0.01f)
        assertEquals(0L, summary.remainingAmount.minorUnits)
        assertEquals(250000L, summary.overBudgetAmount.minorUnits)
    }

    @Test
    fun testZeroBudget() {
        val budget = Amount.ZERO
        val spent = Amount.fromMajor(500)

        val summary = BudgetCalculator.calculate(budget, spent)

        assertEquals(BudgetStatus.ON_TRACK, summary.status)
        assertEquals(0f, summary.percentageUsed, 0.01f)
    }
}
