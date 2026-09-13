package com.finora.android.domain.model

import com.finora.android.data.local.entity.SavingsGoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsCalculationTest {

    @Test
    fun testGoalInProgress() {
        val now = 1700000000000L
        val goal = SavingsGoalEntity(
            id = "g1",
            profileId = "p1",
            name = "Goa Trip",
            targetAmountMinorUnits = 5000000L, // ₹50,000.00
            currencyCode = "INR",
            targetDate = now + (30L * 24 * 60 * 60 * 1000L * 2), // ~2 months ahead
            templateType = "VACATION",
            isCompleted = false
        )

        val summary = SavingsCalculator.calculateGoalSummary(
            goal = goal,
            currentSavedUnits = 2000000L, // ₹20,000.00 saved
            nowMillis = now
        )

        assertEquals(40f, summary.percentageComplete, 0.01f) // 20k / 50k = 40%
        assertEquals(3000000L, summary.remainingGap.minorUnits) // ₹30,000.00 remaining
        assertFalse(summary.isCompleted)
        assertNotNull(summary.requiredMonthlyPace)
    }

    @Test
    fun testGoalCompleted() {
        val goal = SavingsGoalEntity(
            id = "g2",
            profileId = "p1",
            name = "Emergency Fund",
            targetAmountMinorUnits = 10000000L, // ₹100,000.00
            currencyCode = "INR",
            templateType = "EMERGENCY_FUND",
            isCompleted = false
        )

        val summary = SavingsCalculator.calculateGoalSummary(
            goal = goal,
            currentSavedUnits = 10000000L // ₹100,000.00 saved
        )

        assertEquals(100f, summary.percentageComplete, 0.01f)
        assertEquals(0L, summary.remainingGap.minorUnits)
        assertTrue(summary.isCompleted)
    }
}
