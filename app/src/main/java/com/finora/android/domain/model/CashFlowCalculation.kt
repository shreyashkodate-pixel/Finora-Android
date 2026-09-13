package com.finora.android.domain.model

import com.finora.android.core.model.Amount

/**
 * Summary of user cash flow per SRS §3.21 (INC) and FR-INC-V1.2-005.
 * Deterministic math over minor units (paise/cents).
 */
data class CashFlowSummary(
    val totalIncome: Amount,
    val totalExpenses: Amount,
    val netCashFlow: Amount,
    val isPositive: Boolean,
    val savingsRatePercentage: Float
)

object CashFlowCalculator {

    fun calculate(totalIncome: Amount, totalExpenses: Amount): CashFlowSummary {
        val incomeUnits = totalIncome.minorUnits
        val expenseUnits = totalExpenses.minorUnits

        val netUnits = incomeUnits - expenseUnits
        val isPositive = netUnits >= 0L

        val savingsRate = if (incomeUnits > 0L) {
            val savedUnits = netUnits.coerceAtLeast(0L)
            (savedUnits.toDouble() / incomeUnits.toDouble() * 100.0).toFloat()
        } else {
            0f
        }

        return CashFlowSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netCashFlow = Amount(netUnits),
            isPositive = isPositive,
            savingsRatePercentage = savingsRate
        )
    }
}
