package com.finora.android.domain.model

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.RecurringExpenseEntity
import java.util.Calendar

data class RecurringCommitmentSummary(
    val monthlyCommitment: Amount,
    val annualCommitment: Amount,
    val activeCount: Int,
    val dueSoonCount: Int
)

object RecurringCalculator {

    /**
     * Calculates the monthly normalized commitment for an item based on frequency.
     * WEEKLY: (amount * 52) / 12
     * DAILY: (amount * 365) / 12
     * MONTHLY: amount
     * YEARLY: amount / 12
     */
    fun toMonthlyAmountMinorUnits(amountMinorUnits: Long, frequency: String): Long {
        return when (frequency.uppercase()) {
            "DAILY" -> (amountMinorUnits * 365) / 12
            "WEEKLY" -> (amountMinorUnits * 52) / 12
            "YEARLY" -> amountMinorUnits / 12
            else -> amountMinorUnits // MONTHLY default
        }
    }

    /**
     * Calculates monthly and annual totals across all active subscriptions/commitments.
     */
    fun calculateSummary(
        items: List<RecurringExpenseEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): RecurringCommitmentSummary {
        val activeItems = items.filter { it.isActive }
        var totalMonthlyMinor = 0L
        val sevenDaysAhead = nowMillis + (7L * 24 * 60 * 60 * 1000)
        var dueSoon = 0

        for (item in activeItems) {
            totalMonthlyMinor += toMonthlyAmountMinorUnits(item.amountMinorUnits, item.frequency)
            if (item.nextDueDate in nowMillis..sevenDaysAhead) {
                dueSoon++
            }
        }

        val annualMinor = totalMonthlyMinor * 12L

        return RecurringCommitmentSummary(
            monthlyCommitment = Amount(totalMonthlyMinor),
            annualCommitment = Amount(annualMinor),
            activeCount = activeItems.size,
            dueSoonCount = dueSoon
        )
    }

    /**
     * Calculates next due date after an occurrence is logged or confirmed.
     */
    fun computeNextDueDate(currentDueDate: Long, frequency: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }
        when (frequency.uppercase()) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
