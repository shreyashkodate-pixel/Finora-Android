package com.finora.android.domain.model

import kotlin.math.max

enum class SimulatorVerdict {
    SAFE_TO_BUY,
    PROCEED_WITH_CAUTION,
    DELAY_PURCHASE
}

data class PurchaseSimulationResult(
    val purchaseAmountMinorUnits: Long,
    val remainingBudgetBeforeMinorUnits: Long,
    val remainingBudgetAfterMinorUnits: Long,
    val safeToSpendBeforeMinorUnits: Long,
    val safeToSpendAfterMinorUnits: Long,
    val verdict: SimulatorVerdict,
    val verdictRationale: String,
    val impactAnalysis: String,
    val alternatives: List<String>
)

object PurchaseSimulatorCalculation {

    fun simulate(
        purchaseAmountMinorUnits: Long,
        currentRemainingBudgetMinorUnits: Long,
        totalBudgetMinorUnits: Long,
        upcomingRecurringMinorUnits: Long,
        remainingDaysInCycle: Int,
        totalLiquidSavingsMinorUnits: Long
    ): PurchaseSimulationResult {
        val days = max(1, remainingDaysInCycle)

        val beforeSafeToSpend = maxOf(
            0L,
            (currentRemainingBudgetMinorUnits - upcomingRecurringMinorUnits) / days
        )

        val remainingAfter = currentRemainingBudgetMinorUnits - purchaseAmountMinorUnits
        val uncommittedAfter = remainingAfter - upcomingRecurringMinorUnits
        val afterSafeToSpend = if (uncommittedAfter > 0) uncommittedAfter / days else 0L

        val verdict: SimulatorVerdict
        val rationale: String
        val impactAnalysis: String
        val alternatives = mutableListOf<String>()

        when {
            // Scenario 1: Outright exceeds remaining budget
            remainingAfter < 0L -> {
                verdict = SimulatorVerdict.DELAY_PURCHASE
                rationale = "Exceeds remaining monthly budget by ₹${kotlin.math.abs(remainingAfter) / 100}."
                impactAnalysis = "Purchasing today will push your monthly budget into a negative deficit and drain your emergency cash cushion."
                alternatives.add("Delay purchase to next month's budget cycle")
                alternatives.add("Split into two milestone payments if available")
                alternatives.add("Create a targeted Savings Goal to fund this purchase in 60 days")
            }

            // Scenario 2: Severe impact on Safe-to-Spend (< 40% of original daily allowance)
            uncommittedAfter < (upcomingRecurringMinorUnits * 0.5).toLong() || (afterSafeToSpend < (beforeSafeToSpend * 0.4)) -> {
                verdict = SimulatorVerdict.PROCEED_WITH_CAUTION
                rationale = "Tightens your daily safe-to-spend buffer significantly from ₹${beforeSafeToSpend / 100}/day down to ₹${afterSafeToSpend / 100}/day."
                impactAnalysis = "Leaves minimal safety margin for unforeseen incidental costs during the remaining $days days."
                alternatives.add("Reduce discretionary dining/shopping this week by ₹${(purchaseAmountMinorUnits * 0.3).toLong() / 100}")
                alternatives.add("Verify all upcoming bills are accounted for before finalizing")
            }

            // Scenario 3: Healthy headroom remains
            else -> {
                verdict = SimulatorVerdict.SAFE_TO_BUY
                rationale = "Comfortably covered by available discretionary budget."
                impactAnalysis = "Your daily safe-to-spend remains stable at ₹${afterSafeToSpend / 100}/day with all upcoming bills fully protected."
                alternatives.add("Log payment against your primary debit account upon acquisition")
            }
        }

        return PurchaseSimulationResult(
            purchaseAmountMinorUnits = purchaseAmountMinorUnits,
            remainingBudgetBeforeMinorUnits = currentRemainingBudgetMinorUnits,
            remainingBudgetAfterMinorUnits = remainingAfter,
            safeToSpendBeforeMinorUnits = beforeSafeToSpend,
            safeToSpendAfterMinorUnits = afterSafeToSpend,
            verdict = verdict,
            verdictRationale = rationale,
            impactAnalysis = impactAnalysis,
            alternatives = alternatives
        )
    }
}
