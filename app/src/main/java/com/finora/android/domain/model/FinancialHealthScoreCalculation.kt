package com.finora.android.domain.model

import kotlin.math.min
import kotlin.math.max

/**
 * 5 distinct pillars defined by PRD §15.5 and SRS §3.28 (FR-FHS-V2.0-001 to 005).
 */
data class HealthPillar(
    val title: String,
    val score: Int, // 0 to 100
    val weightPercent: Int,
    val statusSummary: String
)

data class FinancialHealthScoreResult(
    val overallScore: Int, // 0 to 100
    val ratingLabel: String, // "Excellent", "Good", "Fair", "Vulnerable"
    val pillars: List<HealthPillar>,
    val positiveDrivers: List<String>,
    val negativeDrags: List<String>,
    val actionableTips: List<String>
)

object FinancialHealthScoreCalculation {

    fun calculate(
        savingsRatePercent: Double,
        budgetUtilizationPercent: Double,
        dailySpendStdDevRatio: Double, // standard deviation / mean spend (e.g. 0.3 = 30% variance)
        cashBufferMonths: Double, // liquid cash / monthly fixed expenses
        detectedLeaksCount: Int
    ): FinancialHealthScoreResult {

        // 1. Savings Discipline (Weight 25%): 20%+ savings rate gets 100
        val savingsScore = when {
            savingsRatePercent >= 25.0 -> 100
            savingsRatePercent >= 20.0 -> 90
            savingsRatePercent >= 10.0 -> 75
            savingsRatePercent > 0.0 -> 55
            else -> 25
        }
        val savingsPillar = HealthPillar(
            title = "Savings Discipline",
            score = savingsScore,
            weightPercent = 25,
            statusSummary = if (savingsRatePercent >= 20.0) "Excellent savings rate (>20%)" else "Room to improve monthly savings rate"
        )

        // 2. Budget Adherence (Weight 25%): <= 85% is ideal, > 100% penalized
        val budgetScore = when {
            budgetUtilizationPercent <= 0.0 -> 70 // No budget set yet
            budgetUtilizationPercent <= 80.0 -> 95
            budgetUtilizationPercent <= 100.0 -> 80
            budgetUtilizationPercent <= 115.0 -> 45
            else -> 20
        }
        val budgetPillar = HealthPillar(
            title = "Budget Adherence",
            score = budgetScore,
            weightPercent = 25,
            statusSummary = if (budgetUtilizationPercent <= 100.0) "Spending stays within allocated budget limits" else "Budget exceeded this cycle"
        )

        // 3. Spending Stability (Weight 20%): Lower variance = higher stability
        val stabilityScore = when {
            dailySpendStdDevRatio <= 0.35 -> 95
            dailySpendStdDevRatio <= 0.65 -> 80
            dailySpendStdDevRatio <= 1.0 -> 60
            else -> 35
        }
        val stabilityPillar = HealthPillar(
            title = "Spending Stability",
            score = stabilityScore,
            weightPercent = 20,
            statusSummary = if (stabilityScore >= 80) "Smooth daily cash outflow pattern" else "High daily spending spikes detected"
        )

        // 4. Cash Cushion (Weight 15%): 3-6 months is ideal
        val cushionScore = when {
            cashBufferMonths >= 4.0 -> 100
            cashBufferMonths >= 2.0 -> 80
            cashBufferMonths >= 1.0 -> 60
            cashBufferMonths > 0.0 -> 40
            else -> 20
        }
        val cushionPillar = HealthPillar(
            title = "Cash Cushion",
            score = cushionScore,
            weightPercent = 15,
            statusSummary = if (cashBufferMonths >= 3.0) "Solid emergency cash reserve" else "Liquid buffer covers under 3 months"
        )

        // 5. Leak Control (Weight 15%): 0 leaks = 100
        val leakScore = when (detectedLeaksCount) {
            0 -> 100
            1 -> 80
            2 -> 60
            else -> max(20, 100 - (detectedLeaksCount * 25))
        }
        val leakPillar = HealthPillar(
            title = "Leak Control",
            score = leakScore,
            weightPercent = 15,
            statusSummary = if (detectedLeaksCount == 0) "Zero wasteful recurring micro-drains" else "$detectedLeaksCount potential spending leaks flagged"
        )

        // Weighted Total: 25% + 25% + 20% + 15% + 15% = 100%
        val weightedScore = (
            (savingsScore * 0.25) +
            (budgetScore * 0.25) +
            (stabilityScore * 0.20) +
            (cushionScore * 0.15) +
            (leakScore * 0.15)
        ).toInt()

        val overallScore = max(0, min(100, weightedScore))

        val label = when {
            overallScore >= 85 -> "Excellent"
            overallScore >= 70 -> "Good"
            overallScore >= 50 -> "Fair"
            else -> "Vulnerable"
        }

        val positiveDrivers = mutableListOf<String>()
        val negativeDrags = mutableListOf<String>()
        val actionableTips = mutableListOf<String>()

        if (savingsScore >= 80) positiveDrivers.add("Consistently healthy savings rate (${savingsRatePercent.toInt()}%)")
        else negativeDrags.add("Savings rate is currently below target (${savingsRatePercent.toInt()}%)")

        if (budgetScore >= 80) positiveDrivers.add("Strict adherence to monthly category ceilings")
        else negativeDrags.add("Budget ceiling exceeded or near threshold (${budgetUtilizationPercent.toInt()}%)")

        if (cushionScore >= 80) positiveDrivers.add("Emergency buffer covers ${String.format("%.1f", cashBufferMonths)} months")
        else negativeDrags.add("Cash reserves below recommended 3-month threshold")

        if (leakScore < 80) negativeDrags.add("$detectedLeaksCount micro-spending leaks detected in transactions")

        // Top 3 Actionable Tips
        if (leakScore < 80) {
            actionableTips.add("Review flagged recurring leaks to save up to ₹1,500/mo")
        }
        if (savingsScore < 80) {
            actionableTips.add("Automate an extra ₹2,000 transfer to your savings goal on payday")
        }
        if (budgetScore < 80) {
            actionableTips.add("Reallocate ₹1,000 from dining to groceries to stay within overall limit")
        }
        if (actionableTips.size < 3) {
            actionableTips.add("Keep logging daily expenses to preserve 100% ledger audit fidelity")
        }

        return FinancialHealthScoreResult(
            overallScore = overallScore,
            ratingLabel = label,
            pillars = listOf(savingsPillar, budgetPillar, stabilityPillar, cushionPillar, leakPillar),
            positiveDrivers = positiveDrivers,
            negativeDrags = negativeDrags,
            actionableTips = actionableTips.take(3)
        )
    }
}
