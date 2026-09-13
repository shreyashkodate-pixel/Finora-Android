package com.finora.android.domain.model

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.RecurringExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringCalculationTest {

    @Test
    fun testMonthlyNormalization() {
        val monthlyAmount = 100000L // ₹1,000.00
        assertEquals(100000L, RecurringCalculator.toMonthlyAmountMinorUnits(monthlyAmount, "MONTHLY"))

        val yearlyAmount = 1200000L // ₹12,000.00 / yr
        assertEquals(100000L, RecurringCalculator.toMonthlyAmountMinorUnits(yearlyAmount, "YEARLY"))

        val weeklyAmount = 120000L // ₹1,200.00 / wk -> (1200 * 52) / 12 = 5200
        assertEquals(520000L, RecurringCalculator.toMonthlyAmountMinorUnits(weeklyAmount, "WEEKLY"))
    }

    @Test
    fun testCalculateSummary() {
        val now = 1700000000000L
        val items = listOf(
            RecurringExpenseEntity(
                id = "1",
                profileId = "p1",
                title = "Netflix",
                amountMinorUnits = 50000L, // ₹500/mo
                currencyCode = "INR",
                categoryId = "c1",
                frequency = "MONTHLY",
                startDate = now,
                nextDueDate = now + (3L * 24 * 60 * 60 * 1000), // due in 3 days -> dueSoon
                isActive = true
            ),
            RecurringExpenseEntity(
                id = "2",
                profileId = "p1",
                title = "Gym",
                amountMinorUnits = 1200000L, // ₹12,000/yr = ₹1,000/mo
                currencyCode = "INR",
                categoryId = "c2",
                frequency = "YEARLY",
                startDate = now,
                nextDueDate = now + (30L * 24 * 60 * 60 * 1000), // not due soon
                isActive = true
            ),
            RecurringExpenseEntity(
                id = "3",
                profileId = "p1",
                title = "Old Sub",
                amountMinorUnits = 20000L,
                currencyCode = "INR",
                categoryId = "c1",
                frequency = "MONTHLY",
                startDate = now,
                nextDueDate = now,
                isActive = false // paused/inactive
            )
        )

        val summary = RecurringCalculator.calculateSummary(items, now)

        assertEquals(2, summary.activeCount)
        assertEquals(1, summary.dueSoonCount)
        assertEquals(150000L, summary.monthlyCommitment.minorUnits) // ₹500 + ₹1,000 = ₹1,500
        assertEquals(1800000L, summary.annualCommitment.minorUnits) // ₹1,500 * 12 = ₹18,000
    }
}
