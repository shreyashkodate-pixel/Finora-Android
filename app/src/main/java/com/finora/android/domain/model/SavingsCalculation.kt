package com.finora.android.domain.model

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.SavingsGoalEntity

data class SavingsGoalSummary(
    val goal: SavingsGoalEntity,
    val targetAmount: Amount,
    val currentSavedAmount: Amount,
    val remainingGap: Amount,
    val percentageComplete: Float,
    val isCompleted: Boolean,
    val requiredMonthlyPace: Amount?
)

object SavingsCalculator {

    fun calculateGoalSummary(
        goal: SavingsGoalEntity,
        currentSavedUnits: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): SavingsGoalSummary {
        val targetUnits = goal.targetAmountMinorUnits
        val remainingUnits = (targetUnits - currentSavedUnits).coerceAtLeast(0L)
        val percentage = if (targetUnits > 0L) {
            (currentSavedUnits.toDouble() / targetUnits.toDouble() * 100.0).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }

        val isCompleted = currentSavedUnits >= targetUnits || goal.isCompleted

        // Calculate pace if target date exists and is in the future
        val requiredMonthlyPace = if (goal.targetDate != null && goal.targetDate > nowMillis && remainingUnits > 0L) {
            val millisRemaining = goal.targetDate - nowMillis
            val monthsRemaining = (millisRemaining.toDouble() / (30.44 * 24 * 60 * 60 * 1000)).coerceAtLeast(1.0)
            val paceMinorUnits = (remainingUnits / monthsRemaining).toLong()
            Amount(paceMinorUnits)
        } else {
            null
        }

        return SavingsGoalSummary(
            goal = goal,
            targetAmount = Amount(targetUnits),
            currentSavedAmount = Amount(currentSavedUnits),
            remainingGap = Amount(remainingUnits),
            percentageComplete = percentage,
            isCompleted = isCompleted,
            requiredMonthlyPace = requiredMonthlyPace
        )
    }
}
