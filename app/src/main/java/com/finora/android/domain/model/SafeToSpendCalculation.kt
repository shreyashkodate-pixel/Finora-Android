package com.finora.android.domain.model

import kotlin.math.max

/**
 * Safe-to-Spend classification state per SRS §3.27 (FR-STS-V2.0-001 to 005).
 */
enum class SafeToSpendState {
    HEALTHY,
    MODERATE,
    CAUTION,
    DANGER
}

/**
 * Result data class representing the daily safe-to-spend analysis.
 */
data class SafeToSpendResult(
    val dailyAllowanceMinorUnits: Long,
    val remainingBudgetMinorUnits: Long,
    val upcomingRecurringMinorUnits: Long,
    val remainingDaysInCycle: Int,
    val state: SafeToSpendState,
    val advisoryNotes: String
)

/**
 * Deterministic calculation engine for Safe-to-Spend Today.
 * Evaluates remaining liquid budget, pending recurring commitments, and days left in cycle.
 */
object SafeToSpendCalculation {

    fun calculate(
        remainingBudgetMinorUnits: Long,
        totalBudgetMinorUnits: Long,
        upcomingRecurringMinorUnits: Long,
        remainingDaysInCycle: Int
    ): SafeToSpendResult {
        val days = max(1, remainingDaysInCycle)

        // Edge case: Budget exceeded or zero
        if (remainingBudgetMinorUnits <= 0L) {
            return SafeToSpendResult(
                dailyAllowanceMinorUnits = 0L,
                remainingBudgetMinorUnits = remainingBudgetMinorUnits,
                upcomingRecurringMinorUnits = upcomingRecurringMinorUnits,
                remainingDaysInCycle = days,
                state = SafeToSpendState.DANGER,
                advisoryNotes = "Monthly budget limit exhausted. Limit all discretionary spending."
            )
        }

        // Uncommitted buffer after reserving for upcoming recurring commitments
        val uncommittedBuffer = remainingBudgetMinorUnits - upcomingRecurringMinorUnits

        val state = when {
            uncommittedBuffer <= 0L -> SafeToSpendState.DANGER
            uncommittedBuffer < (remainingBudgetMinorUnits * 0.25).toLong() -> SafeToSpendState.CAUTION
            totalBudgetMinorUnits > 0L && (remainingBudgetMinorUnits.toDouble() / totalBudgetMinorUnits) < 0.20 -> SafeToSpendState.MODERATE
            else -> SafeToSpendState.HEALTHY
        }

        val dailyAllowance = if (uncommittedBuffer > 0L) {
            uncommittedBuffer / days
        } else {
            0L
        }

        val note = when (state) {
            SafeToSpendState.HEALTHY -> "Spending is on pace. You have comfortable discretionary buffer."
            SafeToSpendState.MODERATE -> "Moderate buffer remaining. Pacing is close to baseline."
            SafeToSpendState.CAUTION -> "Upcoming recurring bills consume most of your remaining budget."
            SafeToSpendState.DANGER -> "Committed upcoming bills exceed remaining budget. Reserve cash."
        }

        return SafeToSpendResult(
            dailyAllowanceMinorUnits = dailyAllowance,
            remainingBudgetMinorUnits = remainingBudgetMinorUnits,
            upcomingRecurringMinorUnits = upcomingRecurringMinorUnits,
            remainingDaysInCycle = days,
            state = state,
            advisoryNotes = note
        )
    }
}
