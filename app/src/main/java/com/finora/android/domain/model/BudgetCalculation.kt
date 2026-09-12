package com.finora.android.domain.model

import com.finora.android.core.model.Amount

/**
 * Summary of a budget's spending progress and status per SRS FR-BUD-V1.0-003 & 004.
 */
data class BudgetSummary(
    val budgetAmount: Amount,
    val spentAmount: Amount,
    val remainingAmount: Amount,
    val percentageUsed: Float,
    val overBudgetAmount: Amount,
    val status: BudgetStatus
)

object BudgetCalculator {

    /**
     * Deterministic calculation over authoritative records per SRS §6.6.
     * Never depends on AI or external network calls.
     */
    fun calculate(budgetAmount: Amount, spentAmount: Amount): BudgetSummary {
        val budgetUnits = budgetAmount.minorUnits
        val spentUnits = spentAmount.minorUnits

        val remainingUnits = (budgetUnits - spentUnits).coerceAtLeast(0L)
        val overUnits = (spentUnits - budgetUnits).coerceAtLeast(0L)

        val percentage = if (budgetUnits > 0L) {
            (spentUnits.toDouble() / budgetUnits.toDouble() * 100.0).toFloat()
        } else {
            0f
        }

        val status = when {
            percentage >= 100f -> BudgetStatus.OVER_BUDGET
            percentage >= 80f -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.ON_TRACK
        }

        return BudgetSummary(
            budgetAmount = budgetAmount,
            spentAmount = spentAmount,
            remainingAmount = Amount(remainingUnits),
            percentageUsed = percentage,
            overBudgetAmount = Amount(overUnits),
            status = status
        )
    }
}
